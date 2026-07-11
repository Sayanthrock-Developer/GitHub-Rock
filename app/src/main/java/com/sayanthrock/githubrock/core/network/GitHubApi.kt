package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubAuthApi {
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Query("client_id") clientId: String,
        @Query("scope") scope: String
    ): DeviceCodeResponse

    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun requestToken(
        @Query("client_id") clientId: String,
        @Query("device_code") deviceCode: String,
        @Query("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): DeviceTokenResponse

    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun refreshToken(
        @Query("client_id") clientId: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Query("refresh_token") refreshToken: String
    ): DeviceTokenResponse
}

interface GitHubRestApi {
    @GET("user") suspend fun me(): GitHubUser
    @GET("rate_limit") suspend fun rateLimit(): RateLimitResponse

    @GET("user/repos")
    suspend fun repositories(
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<GitHubRepositoryModel>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): RepositorySearchResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun contents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): List<ContentEntry>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun file(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): ContentEntry

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitRefRequest
    ): Response<Unit>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun commitFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: FileCommitRequest
    ): Response<ContentEntry>

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: PullRequestRequest
    ): PullRequest

    @GET("repos/{owner}/{repo}/issues")
    suspend fun issues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30
    ): List<GitHubIssue>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): GitHubIssue

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun pullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30
    ): List<PullRequestSummary>

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun workflows(@Path("owner") owner: String, @Path("repo") repo: String): WorkflowList

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun workflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): WorkflowRuns

    @POST("repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: Long,
        @Body request: WorkflowDispatchRequest
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{runId}/cancel")
    suspend fun cancelWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{runId}/rerun")
    suspend fun rerunWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): List<Release>

    @DELETE("repos/{owner}/{repo}/releases/{releaseId}")
    suspend fun deleteRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("releaseId") releaseId: Long
    ): Response<Unit>
}
