package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubNotification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    val subject: GitHubNotificationSubject,
    val repository: GitHubNotificationRepository,
    val url: String? = null
)

@Serializable
data class GitHubNotificationSubject(
    val title: String,
    val url: String? = null,
    @SerialName("latest_comment_url") val latestCommentUrl: String? = null,
    val type: String
)

@Serializable
data class GitHubNotificationRepository(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: GitHubNotificationOwner,
    @SerialName("html_url") val htmlUrl: String? = null,
    val private: Boolean = false
)

@Serializable
data class GitHubNotificationOwner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)
