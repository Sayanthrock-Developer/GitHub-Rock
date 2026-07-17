package com.sayanthrock.githubrock.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFlowPollTimingTest {
    @Test fun rapidManualChecksDoNotResetTheOriginalInterval() {
        val firstRequestAt = 10_000L

        assertEquals(
            2_000L,
            remainingPollDelayMillis(
                lastRequestAtMillis = firstRequestAt,
                nowMillis = 13_000L,
                intervalSeconds = 5
            )
        )
        assertEquals(
            0L,
            remainingPollDelayMillis(
                lastRequestAtMillis = firstRequestAt,
                nowMillis = 15_000L,
                intervalSeconds = 5
            )
        )
    }

    @Test fun slowDownTransitionIsPersistedBeforeTheNextPoll() {
        assertEquals(
            10,
            nextPollIntervalSeconds(
                currentIntervalSeconds = 5,
                error = "slow_down"
            )
        )
        assertEquals(
            10,
            nextPollIntervalSeconds(
                currentIntervalSeconds = 10,
                error = "authorization_pending"
            )
        )
    }

    @Test fun slowDownUsesTheExpandedInterval() {
        assertEquals(
            7_000L,
            remainingPollDelayMillis(
                lastRequestAtMillis = 20_000L,
                nowMillis = 23_000L,
                intervalSeconds = 10
            )
        )
    }

    @Test fun refreshTokenMustExistAndRemainValidBeyondTheSafetyWindow() {
        val now = 10_000L

        assertFalse(isRefreshTokenUsable(null, null, now))
        assertFalse(isRefreshTokenUsable("", null, now))
        assertFalse(isRefreshTokenUsable("refresh", now + 60L, now))
        assertTrue(isRefreshTokenUsable("refresh", now + 61L, now))
        assertTrue(isRefreshTokenUsable("refresh", null, now))
    }
}
