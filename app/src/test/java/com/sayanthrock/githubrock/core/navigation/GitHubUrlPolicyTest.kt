package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUrlPolicyTest {
    @Test fun signupAndDeviceAuthorizationAreNotRepositoryLinks() {
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://github.com/signup"))
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://github.com/login/device"))
    }

    @Test fun standardGitHubRepositoryUrlsAreAccepted() {
        assertTrue(GitHubUrlPolicy.isRepositoryUrl("https://github.com/SayanthRock/GitHub-Rock"))
        assertTrue(GitHubUrlPolicy.isRepositoryUrl("https://github.com/SayanthRock/GitHub-Rock/issues"))
    }

    @Test fun nonGitHubAndLookalikeHostsAreRejected() {
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("http://github.com/signup"))
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("https://github.com.example.com/signup"))
    }

    @Test fun customTabsMustUseAnExternalBrowserPackage() {
        assertTrue(isExternalBrowserPackage("com.android.chrome", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage("com.sayanthrock.githubrock", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage(null, "com.sayanthrock.githubrock"))
    }
}
