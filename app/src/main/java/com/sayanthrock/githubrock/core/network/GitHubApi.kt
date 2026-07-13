package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.*
import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubAuthApi {
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Query("client_id") clientId: String
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

    @GET("user/followers")
    suspend fun followers(
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Owner>

    @GET("user/following")
    suspend fun following(
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Owner>

    @GET("rate_limit") suspend fun rateLimit(): RateLimitResponse

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(@Path("owner") owner: String, @Path("repo") repo: String): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(@Path("owner") owner: String, @Path("repo") repo: String): Response<Unit>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepository(@Path("owner") owner: String, @Path("repo") repo: String): GitHubRepositoryModel

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateReleaseRequest
    ): Release

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

    @GET("repos/{owner}/{repo}/branches/{branch}/protection")
    suspend fun branchProtection(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitRefRequest
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun branchReference(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String
    ): GitReference

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun commitFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: FileCommitRequest
    ): Response<ContentEntry>

    @DELETE("repos/{owner}/{repo}/contents/{path}")
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: FileDeleteRequest
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: PullRequestRequest
    ): PullRequest

    @GET("repos/{owner}/{repo}/issues/{issueNumber}/comments")
    suspend fun issueComments(@Path("owner") owner: String, @Path("repo") repo: String, @Path("issueNumber") issueNumber: Int): List<IssueComment>

    @POST("repos/{owner}/{repo}/issues/{issueNumber}/comments")
    suspend fun addIssueComment(@Path("owner") owner: String, @Path("repo") repo: String, @Path("issueNumber") issueNumber: Int, @Body body: Map<String, String>): IssueComment

    @GET("repos/{owner}/{repo}/pulls/{pullNumber}/reviews")
    suspend fun pullReviews(@Path("owner") owner: String, @Path("repo") repo: String, @Path("pullNumber") pullNumber: Int): List<PullRequestReview>

    @POST("repos/{owner}/{repo}/pulls/{pullNumber}/reviews")
    suspend fun submitPullReview(@Path("owner") owner: String, @Path("repo") repo: String, @Path("pullNumber") pullNumber: Int, @Body request: ReviewRequest): PullRequestReview

    @PUT("repos/{owner}/{repo}/pulls/{pullNumber}/merge")
    suspend fun mergePullRequest(@Path("owner") owner: String, @Path("repo") repo: String, @Path("pullNumber") pullNumber: Int, @Body request: Map<String, String>): MergeResponse

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/jobs")
    suspend fun workflowJobs(@Path("owner") owner: String, @Path("repo") repo: String, @Path("runId") runId: Long): Map<String, List<WorkflowJob>>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun workflowArtifacts(@Path("owner") owner: String, @Path("repo") repo: String, @Path("runId") runId: Long): Map<String, List<WorkflowArtifact>>

    @GET("repos/{owner}/{repo}/actions/jobs/{jobId}/logs")
    suspend fun workflowJobLogs(@Path("owner") owner: String, @Path("repo") repo: String, @Path("jobId") jobId: Long): Response<ResponseBody>

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

    @PATCH("repos/{owner}/{repo}/issues/{issueNumber}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issueNumber") issueNumber: Int,
        @Body request: UpdateIssueRequest
    ): GitHubIssue

    @POST("repos/{owner}/{repo}/issues/{issueNumber}/reactions")
    suspend fun addIssueReaction(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issueNumber") issueNumber: Int,
        @Body request: IssueReactionRequest
    ): IssueReaction

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun pullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30
    ): List<PullRequestSummary>

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun workflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): WorkflowList

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun workflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): WorkflowRuns

    @GET("repos/{owner}/{repo}/actions/workflows/{workflowId}/runs")
    suspend fun workflowRunsForWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: Long,
        @Query("event") event: String = "workflow_dispatch",
        @Query("per_page") perPage: Int = 20
    ): WorkflowRuns

    @GET("repos/{owner}/{repo}/actions/runs/{runId}")
    suspend fun workflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): WorkflowRun

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

    @PATCH("repos/{owner}/{repo}/releases/{releaseId}")
    suspend fun updateRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("releaseId") releaseId: Long,
        @Body request: UpdateReleaseRequest
    ): Release

    @DELETE("repos/{owner}/{repo}/releases/{releaseId}")
    suspend fun deleteRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("releaseId") releaseId: Long
    ): Response<Unit>
}
