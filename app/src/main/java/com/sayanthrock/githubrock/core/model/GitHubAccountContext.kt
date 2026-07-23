package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubOrganizationAccount(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String = ""
)
