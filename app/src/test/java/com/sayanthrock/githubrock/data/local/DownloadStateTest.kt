package com.sayanthrock.githubrock.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateTest {
    @Test
    fun wireValuesRoundTripToTypedStates() {
        DownloadState.entries.forEach { state ->
            assertEquals(state, DownloadState.fromWireValue(state.wireValue))
        }
    }

    @Test
    fun unknownPersistedStateFailsClosedAsFailed() {
        assertEquals(DownloadState.FAILED, DownloadState.fromWireValue("future_state"))
    }

    @Test
    fun lifecycleContainsVerificationAndInstallableStates() {
        assertTrue(DownloadState.entries.contains(DownloadState.VERIFYING))
        assertTrue(DownloadState.entries.contains(DownloadState.INSTALLABLE))
    }
}
