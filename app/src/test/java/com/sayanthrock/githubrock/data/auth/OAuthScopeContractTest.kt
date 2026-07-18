package com.sayanthrock.githubrock.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthScopeContractTest {
    @Test fun deviceFlowRequestsTheScopesRequiredByNativeFeatures() {
        val scopes = GITHUB_OAUTH_SCOPES.split(' ')

        assertEquals(scopes.distinct(), scopes)
        assertTrue("repo" in scopes)
        assertTrue("workflow" in scopes)
        assertTrue("read:user" in scopes)
        assertTrue("user:email" in scopes)
        assertTrue("read:org" in scopes)
        assertTrue("notifications" in scopes)
    }
}
