package com.sayanthrock.githubrock.download

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStateTest {
    @Test
    fun everyStateHasStablePersistedWireValue() {
        assertEquals(
            listOf(
                "queued", "starting", "downloading", "paused", "retrying", "completed",
                "verifying", "verified", "installing", "installed", "failed", "cancelled", "unavailable"
            ),
            DownloadState.entries.map { it.wire }
        )
    }

    @Test
    fun persistedValueRoundTrips() {
        DownloadState.entries.forEach { state ->
            assertEquals(state, DownloadState.fromWire(state.wire))
        }
    }

    @Test
    fun unknownPersistedValueFailsClosed() {
        assertEquals(DownloadState.FAILED, DownloadState.fromWire("unknown-state"))
    }
}
