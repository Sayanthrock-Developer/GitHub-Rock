package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.screens.DownloadControl
import com.sayanthrock.githubrock.ui.screens.downloadControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadControlsTest {
    @Test
    fun activeDownloadsCanPauseOrCancel() {
        listOf("queued", "downloading", "retrying").forEach { status ->
            assertEquals(
                setOf(DownloadControl.Pause, DownloadControl.Cancel),
                downloadControls(status)
            )
        }
    }

    @Test
    fun pausedDownloadsCanResumeOrCancel() {
        assertEquals(
            setOf(DownloadControl.Resume, DownloadControl.Cancel),
            downloadControls("paused")
        )
    }

    @Test
    fun stoppedDownloadsCanRestartWithoutDuplicatingHistory() {
        listOf("failed", "cancelled").forEach { status ->
            assertEquals(
                setOf(DownloadControl.Retry),
                downloadControls(status)
            )
        }
    }

    @Test
    fun completedAndUnknownStatesHaveNoQueueControls() {
        assertTrue(downloadControls("completed").isEmpty())
        assertTrue(downloadControls("unknown").isEmpty())
    }
}
