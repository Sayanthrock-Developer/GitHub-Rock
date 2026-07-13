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
    /**
     * Requests a device code for GitHub OAuth authentication.
     *
     * @param clientId The GitHub OAuth application client ID.
     * @return The device code response.
     */
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String
    ): DeviceCodeResponse

    /**
     * Exchanges OAuth device or refresh credentials for an access token.
     *
     * @param clientId The GitHub OAuth application client ID.
     * @param grantType The OAuth grant type.
     * @param deviceCode The device code used for device authorization, if applicable.
     * @param refreshToken The refresh token used to obtain a new access token, if applicable.
     * @return The access token response.
     */
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
    /**
     * Retrieves repositories available to the authenticated user.
     *
     * @param page The page number to retrieve.
     * @param perPage The number of repositories per page.
     * @param sort The repository sort order.
     * @param affiliation The repository affiliations to include.
     * @return The HTTP response containing the repository list.
     */
    @GET("user/repos")
    suspend fun userRepositories(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): Response<List<RepositoryDto>>

    /**
     * Searches GitHub repositories using the specified query and result ordering.
     *
     * @param query The repository search query.
     * @param page The result page number.
     * @param perPage The number of results per page.
     * @param sort The criterion used to sort results.
     * @param order The result sort direction.
     * @return The GitHub repository search response.
     */
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc"
    ): Response<SearchRepositoriesResponse>

    /**
     * Retrieves the authenticated user's profile.
     *
     * @return The HTTP response containing the current user's profile.
     */
    @GET("user")
    suspend fun currentUser(): Response<GitHubUserDto>

    /**
     * Retrieves the authenticated user's API rate-limit information.
     *
     * @return The GitHub API rate-limit response.
     */
    @GET("rate_limit")
    suspend fun rateLimit(): Response<RateLimitResponse>

    /**
     * Executes a GitHub GraphQL query.
     *
     * @param request The GraphQL request payload.
     * @return The HTTP response containing the GraphQL result.
     */
    @POST("graphql")
    suspend fun graphQl(@Body request: GraphQlRequest): Response<GraphQlResponse>
}
