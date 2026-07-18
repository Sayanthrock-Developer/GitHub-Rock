package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.network.GitHubGraphQlApi
import com.sayanthrock.githubrock.core.network.GraphQlRequest
import com.sayanthrock.githubrock.core.security.TokenStore
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryArtworkResolver @Inject constructor(
    private val graphQlApi: GitHubGraphQlApi,
    private val tokenStore: TokenStore
) {
    private val previewCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(MAX_PREVIEW_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, String>?
            ): Boolean = size > MAX_PREVIEW_CACHE_SIZE
        }
    )

    suspend fun attach(repositories: List<GitHubRepositoryModel>): List<GitHubRepositoryModel> {
        if (repositories.isEmpty() || tokenStore.read()?.accessToken.isNullOrBlank()) return repositories

        val unresolved = repositories
            .distinctBy { it.cacheKey() }
            .filterNot { previewCache.containsKey(it.cacheKey()) }

        unresolved.chunked(GRAPHQL_BATCH_SIZE).forEach { batch ->
            runCatchingPreservingCancellation {
                graphQlApi.query(GraphQlRequest(buildQuery(batch))).data
            }.getOrNull()?.let { data ->
                batch.forEachIndexed { index, repository ->
                    val preview = (data["repo$index"] as? JsonObject)
                        ?.get("openGraphImageUrl")
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf(String::isNotBlank)
                        .orEmpty()
                    previewCache[repository.cacheKey()] = preview
                }
            }
        }

        return repositories.map { repository ->
            repository.copy(
                previewImageUrl = previewCache[repository.cacheKey()]?.takeIf(String::isNotBlank)
            )
        }
    }

    private fun GitHubRepositoryModel.cacheKey(): String = fullName.lowercase(Locale.ROOT)

    private fun buildQuery(repositories: List<GitHubRepositoryModel>): String = buildString {
        append("query RepositoryArtwork {")
        repositories.forEachIndexed { index, repository ->
            append("repo")
            append(index)
            append(": repository(owner: ")
            append(repository.owner.login.asGraphQlString())
            append(", name: ")
            append(repository.name.asGraphQlString())
            append(") { openGraphImageUrl }")
        }
        append('}')
    }

    private fun String.asGraphQlString(): String = buildString {
        append('"')
        this@asGraphQlString.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }

    private companion object {
        const val GRAPHQL_BATCH_SIZE = 50
        const val MAX_PREVIEW_CACHE_SIZE = 512
    }
}
