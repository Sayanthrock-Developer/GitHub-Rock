package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val name: String? = null,
    val bio: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class GitHubRepositoryModel(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: Owner,
    val description: String? = null,
    val private: Boolean = false,
    val fork: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("clone_url") val cloneUrl: String = "",
    @SerialName("default_branch") val defaultBranch: String = "main",
    val language: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    @SerialName("updated_at") val updatedAt: String = "",
    val topics: List<String> = emptyList()
)

@Serializable
data class RepositorySearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<GitHubRepositoryModel>
)

@Serializable
data class ContentEntry(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    val type: String = "file",
    @SerialName("download_url") val downloadUrl: String? = null,
    val content: String? = null,
    val encoding: String? = null
)

@Serializable
data class GitHubIssue(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    val body: String? = null,
    val user: Owner,
    val labels: List<GitHubLabel> = emptyList(),
    val assignees: List<Owner> = emptyList(),
    val milestone: Milestone? = null,
    val reactions: IssueReactionSummary = IssueReactionSummary(),
    @SerialName("comments") val commentCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class GitHubLabel(val name: String, val color: String = "")

@Serializable
data class Milestone(
    val number: Int,
    val title: String,
    val state: String = "open"
)

@Serializable
data class IssueReactionSummary(
    @SerialName("+1") val plusOne: Int = 0,
    @SerialName("-1") val minusOne: Int = 0,
    val laugh: Int = 0,
    val hooray: Int = 0,
    val confused: Int = 0,
    val heart: Int = 0,
    val rocket: Int = 0,
    val eyes: Int = 0
)

@Serializable
data class IssueReaction(
    val id: Long = 0,
    val content: String = ""
)

@Serializable
data class PullRequestSummary(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    val draft: Boolean = false,
    val user: Owner,
    val merged: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class Workflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String
)

@Serializable
data class WorkflowList(@SerialName("total_count") val totalCount: Int, val workflows: List<Workflow>)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    @SerialName("display_title") val displayTitle: String = "",
    val status: String,
    val conclusion: String? = null,
    val event: String = "",
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class WorkflowRuns(@SerialName("total_count") val totalCount: Int, @SerialName("workflow_runs") val runs: List<WorkflowRun>)

@Serializable
data class Release(
    val id: Long,
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<ReleaseAsset> = emptyList()
)

@Serializable
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String
)

@Serializable
data class RateLimitResponse(val rate: RateLimit)

@Serializable
data class RateLimit(val limit: Int, val remaining: Int, val reset: Long)

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Int,
    val interval: Int = 5
)

@Serializable
data class DeviceTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)

@Serializable
data class CreateIssueRequest(val title: String, val body: String? = null)

@Serializable
data class UpdateIssueRequest(
    val state: String? = null,
    val title: String? = null,
    val body: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null,
    val milestone: Int? = null
)

@Serializable
data class IssueReactionRequest(val content: String)

@Serializable
data class CreateReleaseRequest(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = true,
    val prerelease: Boolean = false
)

@Serializable
data class UpdateReleaseRequest(
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean,
    val prerelease: Boolean
)

@Serializable
data class WorkflowDispatchRequest(val ref: String, val inputs: Map<String, String> = emptyMap())

@Serializable
data class GitRefRequest(val ref: String, val sha: String)

@Serializable
data class GitReference(
    val ref: String,
    @SerialName("object") val target: GitObject
)

@Serializable
data class GitObject(val sha: String)

@Serializable
data class FileCommitRequest(
    val message: String,
    val content: String,
    val branch: String,
    val sha: String? = null
)

@Serializable
data class FileDeleteRequest(
    val message: String,
    val branch: String,
    val sha: String
)

@Serializable
data class PullRequestRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String? = null,
    val draft: Boolean = false
)

@Serializable
data class PullRequest(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("html_url") val htmlUrl: String = ""
)

@Serializable
data class IssueComment(
    val id: Long,
    val body: String,
    val user: Owner,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ReviewRequest(val body: String, val event: String)

@Serializable
data class PullRequestReview(
    val id: Long,
    val user: Owner,
    val body: String? = null,
    val state: String,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
data class WorkflowJob(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val steps: List<WorkflowStep> = emptyList()
)

@Serializable
data class WorkflowStep(val name: String, val status: String, val conclusion: String? = null)

@Serializable
data class WorkflowArtifact(
    val id: Long,
    val name: String,
    @SerialName("archive_download_url") val archiveDownloadUrl: String,
    @SerialName("expired") val expired: Boolean = false,
    @SerialName("size_in_bytes") val sizeBytes: Long = 0
)

@Serializable
data class MergeResponse(val sha: String? = null, val merged: Boolean, val message: String)

enum class WorkflowDisplayState { Queued, Running, Success, Failed, Cancelled, Unknown }

fun WorkflowRun.displayState(): WorkflowDisplayState = when {
    status == "queued" || status == "waiting" || status == "pending" -> WorkflowDisplayState.Queued
    status == "in_progress" -> WorkflowDisplayState.Running
    conclusion == "success" -> WorkflowDisplayState.Success
    conclusion == "failure" || conclusion == "timed_out" || conclusion == "action_required" -> WorkflowDisplayState.Failed
    conclusion == "cancelled" || conclusion == "skipped" -> WorkflowDisplayState.Cancelled
    else -> WorkflowDisplayState.Unknown
}
