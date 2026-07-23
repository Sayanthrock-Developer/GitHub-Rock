package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubSettingsRoutingTest {
    @Test fun profileAndRepositoriesUseNativeMobileScreens() {
        val destinations = allGitHubWebDestinations("SayanthRock").associateBy { it.id }

        assertEquals(
            GitHubSettingOpenMode.NativeProfile,
            githubSettingOpenMode(destinations.getValue("profile"))
        )
        assertEquals(
            GitHubSettingOpenMode.NativeRepositories,
            githubSettingOpenMode(destinations.getValue("repositories"))
        )
    }

    @Test fun accountAndSecurityPagesStayInsideTheTrustedGitHubPanel() {
        val destinations = allGitHubWebDestinations("SayanthRock").associateBy { it.id }

        listOf("notifications", "organizations", "account-security", "tokens", "billing")
            .forEach { id ->
                assertEquals(
                    GitHubSettingOpenMode.InAppGitHub,
                    githubSettingOpenMode(destinations.getValue(id))
                )
                assertTrue(isTrustedGitHubSettingsUrl(destinations.getValue(id).url))
            }
    }

    @Test fun trustedUrlPolicyRejectsNonHttpsAndLookalikeHosts() {
        assertTrue(isTrustedGitHubSettingsUrl("https://github.com/settings/security"))
        assertTrue(isTrustedGitHubSettingsUrl("https://gist.github.com/SayanthRock"))
        assertFalse(isTrustedGitHubSettingsUrl("http://github.com/settings/security"))
        assertFalse(isTrustedGitHubSettingsUrl("https://github.com.evil.example/settings"))
        assertFalse(isTrustedGitHubSettingsUrl("https://example.com/github"))
    }
}
