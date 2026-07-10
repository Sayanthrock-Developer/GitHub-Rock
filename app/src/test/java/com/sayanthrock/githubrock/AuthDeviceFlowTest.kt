package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.AccessTokenResponse
import com.sayanthrock.githubrock.core.model.DeviceFlowInterpreter
import com.sayanthrock.githubrock.core.model.DeviceFlowResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthDeviceFlowTest {
    @Test
    fun `access token is classified as success`() {
        assertEquals(
            DeviceFlowResult.SUCCESS,
            DeviceFlowInterpreter.classify(AccessTokenResponse(accessToken = "token"))
        )
    }

    @Test
    fun `slow down is classified explicitly`() {
        assertEquals(
            DeviceFlowResult.SLOW_DOWN,
            DeviceFlowInterpreter.classify(AccessTokenResponse(error = "slow_down"))
        )
    }

    @Test
    fun `expired and denied are not treated as pending`() {
        assertEquals(
            DeviceFlowResult.EXPIRED_TOKEN,
            DeviceFlowInterpreter.classify(AccessTokenResponse(error = "expired_token"))
        )
        assertEquals(
            DeviceFlowResult.ACCESS_DENIED,
            DeviceFlowInterpreter.classify(AccessTokenResponse(error = "access_denied"))
        )
    }
}
