package com.sayanthrock.githubrock.ui

internal object AuthReturnPolicy {
    fun shouldCheckAuthorization(
        awaitingVerificationBrowserReturn: Boolean,
        hasPendingDeviceCode: Boolean
    ): Boolean = awaitingVerificationBrowserReturn && hasPendingDeviceCode
}
