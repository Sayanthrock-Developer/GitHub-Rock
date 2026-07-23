package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.network.BackendDeviceTokenResponse
import com.sayanthrock.githubrock.core.network.BackendPublicConfigResponse
import com.sayanthrock.githubrock.data.backend.isVersionAtLeast
import com.sayanthrock.githubrock.data.backend.normalizedBackendBaseUrl
import com.sayanthrock.githubrock.data.backend.toDeviceTokenResponse
import com.sayanthrock.githubrock.data.backend.validateBackendForApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendContractTest {
    @Test fun endpointPolicyNormalizesHttpsAndRejectsUnsafeUrls() {
        assertEquals(
            "https://api.sayanthrock.com/",
            normalizedBackendBaseUrl("  https://api.sayanthrock.com///  ")
        )
        assertEquals(
            "https://example.com/github-rock/",
            normalizedBackendBaseUrl("https://example.com/github-rock")
        )
        assertNull(normalizedBackendBaseUrl("http://api.example.com"))
        assertNull(normalizedBackendBaseUrl("https://user:secret@example.com"))
        assertNull(normalizedBackendBaseUrl("https://example.com?token=secret"))
        assertNull(normalizedBackendBaseUrl(""))
    }

    @Test fun backendAuthorizationPreservesTokenExpiryAndRefreshData() {
        val token = BackendDeviceTokenResponse(
            state = "authorized",
            accessToken = "access",
            tokenType = "bearer",
            scope = "repo workflow",
            expiresIn = 28_800L,
            refreshToken = "refresh",
            refreshTokenExpiresIn = 15_811_200L,
        ).toDeviceTokenResponse()

        assertEquals("access", token.accessToken)
        assertEquals("refresh", token.refreshToken)
        assertEquals(28_800L, token.expiresIn)
        assertEquals(15_811_200L, token.refreshTokenExpiresIn)
    }

    @Test fun backendPollingStatesMapToExistingDeviceFlowErrors() {
        assertEquals(
            "authorization_pending",
            BackendDeviceTokenResponse(state = "pending").toDeviceTokenResponse().error,
        )
        assertEquals(
            "slow_down",
            BackendDeviceTokenResponse(state = "slow_down").toDeviceTokenResponse().error,
        )
        assertEquals(
            "expired_token",
            BackendDeviceTokenResponse(state = "expired").toDeviceTokenResponse().error,
        )
        assertEquals(
            "access_denied",
            BackendDeviceTokenResponse(state = "denied").toDeviceTokenResponse().error,
        )
    }

    @Test fun semanticVersionPolicyHandlesDebugAndPatchVersions() {
        assertTrue(isVersionAtLeast("0.2.0-debug", "0.1.9"))
        assertTrue(isVersionAtLeast("v1.0.0", "1.0"))
        assertFalse(isVersionAtLeast("0.1.9", "0.2.0"))
        assertFalse(isVersionAtLeast("0.1.0", "0.1.1"))
    }

    @Test fun maintenanceAndDisabledFeaturesRejectTheBackendPath() {
        val ready = BackendPublicConfigResponse(
            minSupportedAppVersion = "0.1.0",
            latestAppVersion = "0.2.0",
            maintenanceMode = false,
            features = mapOf("oauthDeviceProxy" to true),
        )
        validateBackendForApp(ready, "oauthDeviceProxy", currentVersion = "0.1.0")

        assertThrows(IllegalArgumentException::class.java) {
            validateBackendForApp(
                ready.copy(maintenanceMode = true),
                "oauthDeviceProxy",
                currentVersion = "0.1.0",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackendForApp(
                ready.copy(features = emptyMap()),
                "oauthDeviceProxy",
                currentVersion = "0.1.0",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackendForApp(
                ready.copy(apiVersion = "v2"),
                "oauthDeviceProxy",
                currentVersion = "0.1.0",
            )
        }
    }
}
