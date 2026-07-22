package com.sayanthrock.githubrock.core.navigation

import java.net.URI

enum class NativeProfileSection(val routeValue: String, val title: String) {
    Repositories("repositories", "Repositories"),
    Followers("followers", "Followers"),
    Following("following", "Following");

    companion object {
        fun fromRoute(value: String?): NativeProfileSection = entries.firstOrNull {
            it.routeValue.equals(value, ignoreCase = true)
        } ?: Repositories
    }
}

data class NativeProfileDestination(
    val login: String,
    val section: NativeProfileSection
) {
    val route: String get() = "native-profile/$login/${section.routeValue}"
}

fun nativeProfileDestination(url: String): NativeProfileDestination? {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (!uri.host.equals("github.com", ignoreCase = true)) return null

    val path = uri.path.orEmpty().trim('/').split('/').filter(String::isNotBlank)
    if (path.size != 1) return null
    val login = normalizedGitHubLogin(path.single()) ?: return null

    val tab = uri.rawQuery.orEmpty()
        .split('&')
        .mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            parts.takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }
        .firstOrNull { (key, _) -> key.equals("tab", ignoreCase = true) }
        ?.second

    val section = when (tab?.lowercase()) {
        "repositories" -> NativeProfileSection.Repositories
        "followers" -> NativeProfileSection.Followers
        "following" -> NativeProfileSection.Following
        else -> return null
    }
    return NativeProfileDestination(login, section)
}
