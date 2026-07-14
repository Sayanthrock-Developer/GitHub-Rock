package com.sayanthrock.githubrock.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class GraphQlRequest(val query: String)

@Serializable
data class GraphQlResponse(
    val data: JsonObject? = null,
    val errors: List<GraphQlError> = emptyList()
)

@Serializable
data class GraphQlError(val message: String = "")

interface GitHubGraphQlApi {
    @POST("graphql")
    suspend fun query(@Body request: GraphQlRequest): GraphQlResponse
}
