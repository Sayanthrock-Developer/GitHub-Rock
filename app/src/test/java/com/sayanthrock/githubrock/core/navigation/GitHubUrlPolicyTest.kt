package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUrlPolicyTest {
    @Test fun signupAndDeviceAuthorizationAreNotRepositoryLinks() {
        assertFalse(GitHubUrlPolicy.isRepositoryUrl(GITHUB_SIGN_UP_URL))
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://github.com/login/device"))
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://github.com/login?add_account=1"))
    }

    @Test fun signupAlwaysUsesTheOfficialSignupPage() {
        assertEquals(GitHubSignupLaunchPlan(GITHUB_SIGN_UP_URL, GITHUB_SIGN_UP_URL, true), githubSignupLaunchPlan(true))
        assertEquals(GitHubSignupLaunchPlan(GITHUB_SIGN_UP_URL, GITHUB_SIGN_UP_URL, false), githubSignupLaunchPlan(false))
    }

    @Test fun standardGitHubRepositoryUrlsAreAccepted() {
        assertTrue(GitHubUrlPolicy.isRepositoryUrl("https://github.com/SayanthRock/GitHub-Rock"))
        assertTrue(GitHubUrlPolicy.isRepositoryUrl("https://github.com/SayanthRock/GitHub-Rock/issues"))
    }

    @Test fun officialGistsOpenSafelyButAreNotRepositoryDeepLinks() {
        assertTrue(GitHubUrlPolicy.isGitHubHttpsUrl("https://gist.github.com/SayanthRock"))
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://gist.github.com/SayanthRock/abc123"))
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("https://gist.github.com.example.com/SayanthRock"))
    }

    @Test fun nonGitHubAndLookalikeHostsAreRejected() {
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("http://github.com/signup"))
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("https://github.com.example.com/signup"))
    }

    @Test fun backendOAuthUrlRequiresFixedPathAndHttps() {
        assertTrue(GitHubUrlPolicy.isBackendOAuthStartUrl("https://auth.githubrock.app/v1/auth/github/start?state=abc&code_challenge=xyz&code_challenge_method=S256"))
        assertFalse(GitHubUrlPolicy.isBackendOAuthStartUrl("http://auth.githubrock.app/v1/auth/github/start?state=abc"))
        assertFalse(GitHubUrlPolicy.isBackendOAuthStartUrl("https://auth.githubrock.app/evil?state=abc"))
        assertFalse(GitHubUrlPolicy.isBackendOAuthStartUrl("https://auth.githubrock.app/v1/auth/github/start#fragment?state=abc"))
        assertFalse(GitHubUrlPolicy.isBackendOAuthStartUrl("https://auth.githubrock.app@evil.example/v1/auth/github/start?state=abc"))
    }

    @Test fun customTabsMustUseAnExternalBrowserPackage() {
        assertTrue(isExternalBrowserPackage("com.android.chrome", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage("com.sayanthrock.githubrock", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage(null, "com.sayanthrock.githubrock"))
    }
}
