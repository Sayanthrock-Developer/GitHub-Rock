package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.AccessTokenResponse
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.GitHubUserDto
import com.sayanthrock.githubrock.core.model.GraphQlRequest
import com.sayanthrock.githubrock.core.model.GraphQlResponse
import com.sayanthrock.githubrock.core.model.RateLimitResponse
import com.sayanthrock.githubrock.core.model.RepositoryDto
import com.sayanthrock.githubrock.core.model.SearchRepositoriesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GitHubAuthApi {
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String
    ): DeviceCodeResponse

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("device_code") deviceCode: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): AccessTokenResponse
}

interface GitHubApi {
    @GET("user/repos")
    suspend fun userRepositories(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): Response<List<RepositoryDto>>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc"
    ): Response<SearchRepositoriesResponse>

    @GET("user")
    suspend fun currentUser(): Response<GitHubUserDto>

    @GET("rate_limit")
    suspend fun rateLimit(): Response<RateLimitResponse>

    @POST("graphql")
    suspend fun graphQl(@Body request: GraphQlRequest): Response<GraphQlResponse>
}
