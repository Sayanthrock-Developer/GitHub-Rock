package com.sayanthrock.githubrock

import org.junit.Assert.assertEquals
import org.junit.Test

private fun downloadProgressLevel(downloadedBytes: Long, totalBytes: Long, status: String): Int = when {
    status.equals("completed", ignoreCase = true) || status.equals("installable", ignoreCase = true) -> 100
    totalBytes <= 0L -> 0
    else -> ((downloadedBytes.coerceAtLeast(0L) * 100L) / totalBytes).toInt().coerceIn(0, 100)
}

class DownloadsProgressTest {
    @Test
    fun downloadLevelsUseTheSameZeroToOneHundredScale() {
        assertEquals(0, downloadProgressLevel(0, 0, "queued"))
        assertEquals(50, downloadProgressLevel(500, 1_000, "downloading"))
        assertEquals(100, downloadProgressLevel(0, 0, "completed"))
        assertEquals(100, downloadProgressLevel(2_000, 1_000, "downloading"))
    }
}
