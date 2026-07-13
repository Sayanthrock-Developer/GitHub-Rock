package com.sayanthrock.githubrock.ui.screens

internal enum class DownloadControl {
    Pause,
    Resume,
    Cancel,
    Retry
}

internal fun downloadControls(status: String): Set<DownloadControl> = when (status) {
    "queued", "downloading", "retrying" -> setOf(
        DownloadControl.Pause,
        DownloadControl.Cancel
    )
    "paused" -> setOf(
        DownloadControl.Resume,
        DownloadControl.Cancel
    )
    "failed", "cancelled" -> setOf(DownloadControl.Retry)
    else -> emptySet()
}
