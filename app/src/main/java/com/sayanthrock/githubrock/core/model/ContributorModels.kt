package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Contributor(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val contributions: Int
)

@Serializable
data class UserSearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<GitHubUser>
)

@Serializable data class CodeSearchItem(
    val name: String,
    val path: String,
    val sha: String,
    @SerialName("html_url") val htmlUrl: String = "",
    val repository: GitHubRepositoryModel
)
@Serializable data class CodeSearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<CodeSearchItem>
)
@Serializable data class IssueSearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<GitHubIssue>
)
@Serializable data class CommitSearchItem(
    val sha: String,
    @SerialName("html_url") val htmlUrl: String = "",
    val commit: CommitSearchCommit,
    val author: Owner? = null,
    val repository: GitHubRepositoryModel? = null
)
@Serializable data class CommitSearchCommit(
    val message: String = "",
    val author: CommitSearchAuthor? = null
)
@Serializable data class CommitSearchAuthor(
    val name: String? = null,
    val date: String? = null
)
@Serializable data class CommitSearchResponse(
    @SerialName("total_count") val totalCount: Int,
    val items: List<CommitSearchItem>
)
