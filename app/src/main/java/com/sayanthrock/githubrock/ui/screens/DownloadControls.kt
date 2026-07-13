package com.sayanthrock.githubrock.ui.screens

internal enum class DownloadControl {
    Pause,
    Resume,
    Cancel,
    Retry
}

/**
 * Determines which controls are available for a download status.
 *
 * @param status The current download status.
 * @return The controls available for the status, or an empty set for unrecognized statuses.
 */
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
