package com.sayanthrock.githubrock.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthReturnPolicyTest {
    @Test fun browserReturnWithPendingCodeTriggersAuthorizationCheck() {
        assertTrue(
            AuthReturnPolicy.shouldCheckAuthorization(
                awaitingVerificationBrowserReturn = true,
                hasPendingDeviceCode = true
            )
        )
    }

    @Test fun ordinaryResumeDoesNotTriggerAuthorizationCheck() {
        assertFalse(
            AuthReturnPolicy.shouldCheckAuthorization(
                awaitingVerificationBrowserReturn = false,
                hasPendingDeviceCode = true
            )
        )
    }

    @Test fun completedOrCancelledFlowDoesNotTriggerAuthorizationCheck() {
        assertFalse(
            AuthReturnPolicy.shouldCheckAuthorization(
                awaitingVerificationBrowserReturn = true,
                hasPendingDeviceCode = false
            )
        )
    }
}
