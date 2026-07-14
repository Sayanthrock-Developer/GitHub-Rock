package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.network.GitHubGraphQlApi
import com.sayanthrock.githubrock.core.network.GraphQlRequest
import com.sayanthrock.githubrock.core.security.TokenStore
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryArtworkResolver @Inject constructor(
    private val graphQlApi: GitHubGraphQlApi,
    private val tokenStore: TokenStore
) {
    suspend fun attach(repositories: List<GitHubRepositoryModel>): List<GitHubRepositoryModel> {
        if (repositories.isEmpty() || tokenStore.read()?.accessToken.isNullOrBlank()) return repositories

        val previews = mutableMapOf<String, String>()
        repositories.chunked(GRAPHQL_BATCH_SIZE).forEach { batch ->
            runCatching {
                graphQlApi.query(GraphQlRequest(buildQuery(batch))).data
            }.getOrNull()?.let { data ->
                batch.forEachIndexed { index, repository ->
                    (data["repo$index"] as? JsonObject)
                        ?.get("openGraphImageUrl")
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf(String::isNotBlank)
                        ?.let { previews[repository.fullName] = it }
                }
            }
        }

        return repositories.map { repository ->
            repository.copy(previewImageUrl = previews[repository.fullName])
        }
    }

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
        const val GRAPHQL_BATCH_SIZE = 25
    }
}
