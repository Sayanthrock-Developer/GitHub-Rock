package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class RepositoryOwnerType { User, Organization }

data class RepositoryOwnerOption(
    val login: String,
    val avatarUrl: String,
    val type: RepositoryOwnerType
)

@Serializable
data class RepositoryOrganization(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class RepositoryLicenseTemplate(
    val key: String,
    val name: String,
    @SerialName("spdx_id") val spdxId: String? = null
)

@Serializable
data class CreateRepositoryRequest(
    val name: String,
    val description: String? = null,
    @SerialName("private") val privateRepository: Boolean = false,
    @SerialName("auto_init") val initializeReadme: Boolean = false,
    @SerialName("gitignore_template") val gitignoreTemplate: String? = null,
    @SerialName("license_template") val licenseTemplate: String? = null
)

@Serializable
data class RenameBranchRequest(
    @SerialName("new_name") val newName: String
)

data class RepositoryCreationForm(
    val ownerLogin: String = "",
    val name: String = "",
    val description: String = "",
    val privateRepository: Boolean = false,
    val initializeReadme: Boolean = true,
    val gitignoreTemplate: String? = null,
    val licenseTemplate: String? = null,
    val defaultBranch: String = "main"
) {
    fun validationError(): String? {
        val normalizedName = name.trim()
        val normalizedBranch = defaultBranch.trim()
        return when {
            ownerLogin.isBlank() -> "Choose an owner account."
            normalizedName.isBlank() -> "Repository name is required."
            normalizedName.length > 100 -> "Repository name must be 100 characters or fewer."
            normalizedName.any(Char::isWhitespace) || '/' in normalizedName || '\\' in normalizedName ->
                "Repository name can use letters, numbers, dots, hyphens, and underscores only."
            description.length > 350 -> "Description must be 350 characters or fewer."
            !initializeReadme && (gitignoreTemplate != null || licenseTemplate != null) ->
                "Initialize the repository before selecting a .gitignore or license template."
            initializeReadme && !isValidBranchName(normalizedBranch) ->
                "Use a valid default branch name."
            else -> null
        }
    }

    fun toRequest(): CreateRepositoryRequest = CreateRepositoryRequest(
        name = name.trim(),
        description = description.trim().takeIf(String::isNotEmpty),
        privateRepository = privateRepository,
        initializeReadme = initializeReadme,
        gitignoreTemplate = gitignoreTemplate?.takeIf { initializeReadme },
        licenseTemplate = licenseTemplate?.takeIf { initializeReadme }
    )
}

internal fun isValidBranchName(value: String): Boolean {
    if (value.isBlank() || value.length > 255) return false
    if (value.startsWith('.') || value.startsWith('/') || value.endsWith('.') || value.endsWith('/')) return false
    if (value.endsWith(".lock") || value.contains("..") || value.contains("//") || value.contains("@{")) return false
    if (value.any(Char::isWhitespace)) return false
    return value.none { it in "~^:?*[\\" }
}
