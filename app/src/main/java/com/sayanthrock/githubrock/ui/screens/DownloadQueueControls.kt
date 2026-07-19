package com.sayanthrock.githubrock.ui.screens

internal enum class QueueDownloadControl {
    Pause,
    Resume,
    Cancel,
    Retry
}

/** Returns the actions available for a persisted download state. */
internal fun queueDownloadControls(status: String): Set<QueueDownloadControl> = when (status) {
    "queued", "downloading", "retrying" -> setOf(
        QueueDownloadControl.Pause,
        QueueDownloadControl.Cancel
    )
    "paused" -> setOf(
        QueueDownloadControl.Resume,
        QueueDownloadControl.Cancel
    )
    "failed", "cancelled" -> setOf(QueueDownloadControl.Retry)
    else -> emptySet()
}
