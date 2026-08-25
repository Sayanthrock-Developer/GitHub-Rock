package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUrlPolicyTest {
    @Test fun deviceAuthorizationIsNotRepositoryLink() {
        assertFalse(GitHubUrlPolicy.isRepositoryUrl("https://github.com/login/device"))
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
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("http://github.com/login/device"))
        assertFalse(GitHubUrlPolicy.isGitHubHttpsUrl("https://github.com.example.com/login/device"))
    }

    @Test fun customTabsMustUseAnExternalBrowserPackage() {
        assertTrue(isExternalBrowserPackage("com.android.chrome", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage("com.sayanthrock.githubrock", "com.sayanthrock.githubrock"))
        assertFalse(isExternalBrowserPackage(null, "com.sayanthrock.githubrock"))
    }
}
