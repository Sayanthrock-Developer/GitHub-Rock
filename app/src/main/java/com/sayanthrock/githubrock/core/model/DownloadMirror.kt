package com.sayanthrock.githubrock.core.model

import java.net.URI

/**
 * Download endpoints available to the user.
 *
 * Direct GitHub is the trusted default. Community mirrors are opt-in and receive
 * the selected public GitHub download URL when used.
 */
enum class DownloadMirror(
    val id: String,
    val label: String,
    val hostLabel: String,
    val community: Boolean
) {
    Direct(
        id = "direct",
        label = "Direct GitHub",
        hostLabel = "Official",
        community = false
    ),
    GhFast(
        id = "ghfast",
        label = "ghfast.top",
        hostLabel = "Community",
        community = true
    ),
    GitHubMoeyy(
        id = "github-moeyy",
        label = "github.moeyy.xyz",
        hostLabel = "Community",
        community = true
    ),
    GhProxy(
        id = "gh-proxy",
        label = "gh-proxy.com",
        hostLabel = "Community",
        community = true
    ),
    Ghps(
        id = "ghps",
        label = "ghps.cc",
        hostLabel = "Community",
        community = true
    ),
    Api99988866(
        id = "api-99988866",
        label = "gh.api.99988866.xyz",
        hostLabel = "Community",
        community = true
    ),
    JsDelivr(
        id = "jsdelivr",
        label = "fastly.jsdelivr.net",
        hostLabel = "Community",
        community = true
    );

    fun resolve(sourceUrl: String): String {
        val normalized = sourceUrl.trim()
        if (this == Direct) return normalized
        check(isTrustedGitHubSource(normalized)) { "Only GitHub HTTPS links can use a mirror" }

        return when (this) {
            Direct -> normalized
            GhFast -> "https://ghfast.top/$normalized"
            GitHubMoeyy -> "https://github.moeyy.xyz/$normalized"
            GhProxy -> "https://gh-proxy.com/$normalized"
            Ghps -> "https://ghps.cc/$normalized"
            Api99988866 -> "https://gh.api.99988866.xyz/$normalized"
            JsDelivr -> resolveJsDelivr(normalized)
        }
    }

    companion object {
        fun fromId(value: String?): DownloadMirror = entries.firstOrNull { it.id == value } ?: Direct

        private fun isTrustedGitHubSource(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            if (uri.scheme?.equals("https", ignoreCase = true) != true || uri.userInfo != null) return false
            val host = uri.host?.lowercase() ?: return false
            return host == "github.com" ||
                host == "raw.githubusercontent.com" ||
                host == "objects.githubusercontent.com" ||
                host.endsWith(".githubusercontent.com")
        }

        private fun resolveJsDelivr(value: String): String {
            val uri = URI(value)
            return when (uri.host?.lowercase()) {
                "raw.githubusercontent.com" -> {
                    val parts = uri.path.trim('/').split('/')
                    check(parts.size >= 4) { "This GitHub raw link cannot be converted for jsDelivr" }
                    val owner = parts[0]
                    val repository = parts[1]
                    val ref = parts[2]
                    val path = parts.drop(3).joinToString("/")
                    "https://fastly.jsdelivr.net/gh/$owner/$repository@$ref/$path"
                }
                "github.com" -> {
                    val parts = uri.path.trim('/').split('/')
                    check(parts.size >= 5 && parts[2] in setOf("blob", "raw")) {
                        "jsDelivr supports repository files, not GitHub release assets"
                    }
                    val owner = parts[0]
                    val repository = parts[1]
                    val ref = parts[3]
                    val path = parts.drop(4).joinToString("/")
                    "https://fastly.jsdelivr.net/gh/$owner/$repository@$ref/$path"
                }
                else -> error("jsDelivr supports GitHub repository files only")
            }
        }
    }
}
