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
