package com.sayanthrock.githubrock.core.model

/**
 * GitHub Rock uses one download source: the official GitHub URL.
 *
 * Release assets and Actions artifacts may be authenticated or short-lived, so
 * proxy/mirror rewriting is intentionally not supported by the download engine.
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
    );

    fun resolve(sourceUrl: String): String = sourceUrl.trim()

    companion object {
        fun fromId(value: String?): DownloadMirror = Direct
    }
}
