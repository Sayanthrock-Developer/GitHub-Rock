package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.DeviceTokenResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceFlowResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun decodesPendingResponseWithoutInventingToken() {
        val response = json.decodeFromString<DeviceTokenResponse>("""{"error":"authorization_pending","interval":5}""")
        assertEquals("authorization_pending", response.error)
        assertNull(response.accessToken)
    }

    @Test fun decodesExpiringGitHubAppToken() {
        val response = json.decodeFromString<DeviceTokenResponse>(
            """{"access_token":"token","expires_in":28800,"refresh_token":"refresh","refresh_token_expires_in":15897600}"""
        )
        assertEquals("token", response.accessToken)
        assertEquals(28_800L, response.expiresIn)
        assertEquals("refresh", response.refreshToken)
    }
}

