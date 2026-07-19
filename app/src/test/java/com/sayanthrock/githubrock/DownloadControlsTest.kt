package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.screens.QueueDownloadControl
import com.sayanthrock.githubrock.ui.screens.queueDownloadControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadControlsTest {
    @Test
    fun activeDownloadsCanPauseOrCancel() {
        listOf("queued", "downloading", "retrying").forEach { status ->
            assertEquals(
                setOf(QueueDownloadControl.Pause, QueueDownloadControl.Cancel),
                queueDownloadControls(status)
            )
        }
    }

    @Test
    fun pausedDownloadsCanResumeOrCancel() {
        assertEquals(
            setOf(QueueDownloadControl.Resume, QueueDownloadControl.Cancel),
            queueDownloadControls("paused")
        )
    }

    @Test
    fun stoppedDownloadsCanRestartWithoutDuplicatingHistory() {
        listOf("failed", "cancelled").forEach { status ->
            assertEquals(
                setOf(QueueDownloadControl.Retry),
                queueDownloadControls(status)
            )
        }
    }

    @Test
    fun completedAndUnknownStatesHaveNoQueueControls() {
        assertTrue(queueDownloadControls("completed").isEmpty())
        assertTrue(queueDownloadControls("unknown").isEmpty())
    }
}
