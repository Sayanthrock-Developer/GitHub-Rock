package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.network.GitHubGraphQlApi
import com.sayanthrock.githubrock.core.network.GraphQlRequest
import com.sayanthrock.githubrock.core.network.GraphQlResponse
import com.sayanthrock.githubrock.core.security.StoredTokens
import com.sayanthrock.githubrock.core.security.TokenStore
import com.sayanthrock.githubrock.data.repository.RepositoryArtworkResolver
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryArtworkResolverTest {
    @Test fun repeatedRepositoriesReuseTheInMemoryPreviewCache() = runBlocking {
        val api = FakeGraphQlApi()
        val resolver = RepositoryArtworkResolver(api, ConnectedTokenStore())
        val repositories = listOf(repository(1), repository(2))

        val first = resolver.attach(repositories)
        val second = resolver.attach(repositories)

        assertEquals(1, api.callCount)
        assertEquals(first.map { it.previewImageUrl }, second.map { it.previewImageUrl })
        assertTrue(first.all { !it.previewImageUrl.isNullOrBlank() })
    }

    @Test fun artworkRequestsUseAtMostFiftyRepositoriesPerBatch() = runBlocking {
        val api = FakeGraphQlApi()
        val resolver = RepositoryArtworkResolver(api, ConnectedTokenStore())

        resolver.attach((1L..51L).map(::repository))

        assertEquals(2, api.callCount)
    }

    private class FakeGraphQlApi : GitHubGraphQlApi {
        var callCount = 0

        override suspend fun query(request: GraphQlRequest): GraphQlResponse {
            callCount += 1
            val aliases = Regex("repo(\\d+):").findAll(request.query).map { it.groupValues[1] }.toList()
            return GraphQlResponse(
                data = buildJsonObject {
                    aliases.forEach { alias ->
                        put(
                            "repo$alias",
                            buildJsonObject {
                                put("openGraphImageUrl", JsonPrimitive("https://images.example/$callCount/$alias"))
                            }
                        )
                    }
                }
            )
        }
    }

    private class ConnectedTokenStore : TokenStore {
        override fun read() = StoredTokens("token", null, null, null)
        override fun save(tokens: StoredTokens) = Unit
        override fun clear() = Unit
    }

    private companion object {
        fun repository(id: Long) = GitHubRepositoryModel(
            id = id,
            name = "repo-$id",
            fullName = "owner/repo-$id",
            owner = Owner("owner")
        )
    }
}
