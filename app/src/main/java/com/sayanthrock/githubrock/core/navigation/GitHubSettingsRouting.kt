package com.sayanthrock.githubrock.core.navigation

import java.net.URI

enum class GitHubSettingOpenMode {
    NativeProfile,
    NativeRepositories,
    InAppGitHub
}

fun githubSettingOpenMode(destination: GitHubWebDestination): GitHubSettingOpenMode = when (destination.id) {
    "profile" -> GitHubSettingOpenMode.NativeProfile
    "repositories" -> GitHubSettingOpenMode.NativeRepositories
    else -> GitHubSettingOpenMode.InAppGitHub
}

fun isTrustedGitHubSettingsUrl(url: String): Boolean = runCatching {
    val uri = URI(url.trim())
    val host = uri.host?.lowercase() ?: return false
    uri.scheme.equals("https", ignoreCase = true) &&
        (host == "github.com" || host.endsWith(".github.com"))
}.getOrDefault(false)
