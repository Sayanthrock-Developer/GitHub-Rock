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
    if (path.isEmpty() || path.size > 2) return null
    val login = normalizedGitHubLogin(path.first()) ?: return null

    // GitHub exposes profile sections both as query tabs and as canonical
    // /followers and /following paths. Keep both forms native so profile links
    // never unexpectedly fall back to the external browser.
    val pathSection = path.getOrNull(1)?.let { value ->
        when (value.lowercase()) {
            "followers" -> NativeProfileSection.Followers
            "following" -> NativeProfileSection.Following
            else -> null
        }
    }

    val tab = uri.rawQuery.orEmpty()
        .split('&')
        .mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            parts.takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }
        .firstOrNull { (key, _) -> key.equals("tab", ignoreCase = true) }
        ?.second

    val querySection = when (tab?.lowercase()) {
        "repositories" -> NativeProfileSection.Repositories
        "followers" -> NativeProfileSection.Followers
        "following" -> NativeProfileSection.Following
        null -> null
        else -> return null
    }

    // A canonical section path wins over a conflicting query parameter.
    // A plain profile URL opens the native repositories section.
    val section = pathSection ?: querySection ?: NativeProfileSection.Repositories
    return NativeProfileDestination(login, section)
}
