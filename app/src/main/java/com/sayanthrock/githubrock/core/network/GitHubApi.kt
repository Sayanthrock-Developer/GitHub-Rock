package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(@Path("owner") owner: String, @Path("repo") repo: String): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(@Path("owner") owner: String, @Path("repo") repo: String): Response<Unit>

    @GET("user/starred")
    suspend fun starredRepositories(
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<GitHubRepositoryModel>

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

    @DELETE("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun deleteBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun branchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String
    ): GitRef

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createGitCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateGitCommitRequest
    ): GitCommit

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createGitTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateGitTreeRequest
    ): GitTree

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createGitBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateGitBlobRequest
    ): GitBlob

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String,
        @Body request: UpdateGitRefRequest
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreatePullRequestRequest
    ): PullRequest

    @GET("repos/{owner}/{repo}/issues")
    suspend fun issues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<Issue>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun pullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<PullRequest>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun workflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): WorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun workflowJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): WorkflowJobsResponse

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body request: WorkflowDispatchRequest
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/rerun")
    suspend fun rerunWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun workflowArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): WorkflowArtifactsResponse

    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<Release>

    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/{archive_format}")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long,
        @Path("archive_format") archiveFormat: String = "zip"
    ): ResponseBody

    @GET("repos/{owner}/{repo}")
    suspend fun repository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepositoryModel

    @GET("users/{username}/repos")
    suspend fun userRepositories(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<GitHubRepositoryModel>

    @GET("users/{username}/followers")
    suspend fun followers(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("users/{username}/following")
    suspend fun following(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("user/following/{username}")
    suspend fun isFollowing(@Path("username") username: String): Response<Unit>

    @PUT("user/following/{username}")
    suspend fun follow(@Path("username") username: String): Response<Unit>

    @DELETE("user/following/{username}")
    suspend fun unfollow(@Path("username") username: String): Response<Unit>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): Issue

    @PATCH("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body request: UpdateIssueRequest
    ): Issue

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body request: CreateIssueCommentRequest
    ): IssueComment

    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun updatePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body request: UpdatePullRequestRequest
    ): PullRequest

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    suspend fun mergePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body request: MergePullRequestRequest
    ): MergePullRequestResponse

    @POST("repos/{owner}/{repo}/pulls/{pull_number}/reviews")
    suspend fun createPullRequestReview(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body request: CreatePullRequestReviewRequest
    ): PullRequestReview

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun issueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<IssueComment>

    @GET("repos/{owner}/{repo}/pulls/{pull_number}/files")
    suspend fun pullRequestFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<PullRequestFile>

    @GET("repos/{owner}/{repo}/collaborators")
    suspend fun collaborators(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("affiliation") affiliation: String = "all",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("repos/{owner}/{repo}/milestones")
    suspend fun milestones(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<Milestone>

    @GET("repos/{owner}/{repo}/labels")
    suspend fun labels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<Label>

    @GET("repos/{owner}/{repo}/contributors")
    suspend fun contributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("repos/{owner}/{repo}/languages")
    suspend fun languages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Map<String, Long>
}

interface GitHubGraphQlApi {
    @POST("graphql")
    suspend fun query(@Body request: GraphQlRequest): GraphQlResponse
}
