package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Long,
    val interval: Long = 5
)

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("error_uri") val errorUri: String? = null
)

enum class DeviceFlowResult {
    SUCCESS,
    AUTHORIZATION_PENDING,
    SLOW_DOWN,
    EXPIRED_TOKEN,
    ACCESS_DENIED,
    UNKNOWN_ERROR
}

object DeviceFlowInterpreter {
    fun classify(response: AccessTokenResponse): DeviceFlowResult = when {
        !response.accessToken.isNullOrBlank() -> DeviceFlowResult.SUCCESS
        response.error == "authorization_pending" -> DeviceFlowResult.AUTHORIZATION_PENDING
        response.error == "slow_down" -> DeviceFlowResult.SLOW_DOWN
        response.error == "expired_token" -> DeviceFlowResult.EXPIRED_TOKEN
        response.error == "access_denied" -> DeviceFlowResult.ACCESS_DENIED
        else -> DeviceFlowResult.UNKNOWN_ERROR
    }
}

@Serializable
data class RepositoryOwnerDto(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class RepositoryDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: RepositoryOwnerDto,
    val description: String? = null,
    val private: Boolean = false,
    val fork: Boolean = false,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    val language: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SearchRepositoriesResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<RepositoryDto>
)

data class RepositorySummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: String,
    val avatarUrl: String?,
    val description: String?,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val htmlUrl: String,
    val defaultBranch: String,
    val stars: Int,
    val forks: Int,
    val openIssues: Int,
    val language: String?,
    val updatedAt: String?
)

fun RepositoryDto.toSummary(): RepositorySummary = RepositorySummary(
    id = id,
    name = name,
    fullName = fullName,
    owner = owner.login,
    avatarUrl = owner.avatarUrl,
    description = description,
    isPrivate = private,
    isFork = fork,
    htmlUrl = htmlUrl,
    defaultBranch = defaultBranch,
    stars = stars,
    forks = forks,
    openIssues = openIssues,
    language = language,
    updatedAt = updatedAt
)

@Serializable
data class GitHubUserDto(
    val login: String,
    val id: Long,
    val name: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

@Serializable
data class RateLimitResponse(val resources: RateLimitResources)

@Serializable
data class RateLimitResources(val core: RateLimitCore)

@Serializable
data class RateLimitCore(
    val limit: Int,
    val used: Int,
    val remaining: Int,
    val reset: Long
)

@Serializable
data class GraphQlRequest(
    val query: String,
    val variables: JsonObject = buildJsonObject { }
)

@Serializable
data class GraphQlResponse(
    val data: JsonElement? = null,
    val errors: JsonElement? = null
)

enum class WorkflowVisualState { QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, UNKNOWN }

object WorkflowStatusMapper {
    fun map(status: String?, conclusion: String?): WorkflowVisualState = when {
        status == "queued" || status == "waiting" || status == "requested" -> WorkflowVisualState.QUEUED
        status == "in_progress" || status == "pending" -> WorkflowVisualState.RUNNING
        conclusion == "success" -> WorkflowVisualState.SUCCESS
        conclusion == "failure" || conclusion == "timed_out" || conclusion == "action_required" -> WorkflowVisualState.FAILED
        conclusion == "cancelled" || conclusion == "skipped" -> WorkflowVisualState.CANCELLED
        else -> WorkflowVisualState.UNKNOWN
    }
}
