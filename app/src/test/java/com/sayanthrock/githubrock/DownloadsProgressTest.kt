package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.screens.downloadProgressLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadsProgressTest {
    @Test
    fun downloadLevelsUseTheSameZeroToOneHundredScale() {
        assertEquals(0, downloadProgressLevel(0, 0, "queued"))
        assertEquals(50, downloadProgressLevel(500, 1_000, "downloading"))
        assertEquals(100, downloadProgressLevel(0, 0, "completed"))
        assertEquals(100, downloadProgressLevel(2_000, 1_000, "downloading"))
    }
}
