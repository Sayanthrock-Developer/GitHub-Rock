package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubWebDestinationsTest {
    @Test fun catalogueCoversTheWiderGitHubPlatformWithTrustedUniqueLinks() {
        val sections = githubWebSections("SayanthRock")
        val destinations = sections.flatMap(GitHubWebSection::destinations)

        assertEquals(6, sections.size)
        assertTrue(destinations.size >= 35)
        assertEquals(destinations.size, destinations.map { it.id }.distinct().size)
        assertEquals(destinations.size, destinations.map { it.url }.distinct().size)
        assertTrue(destinations.all { GitHubUrlPolicy.isGitHubHttpsUrl(it.url) })

        val ids = destinations.mapTo(mutableSetOf()) { it.id }
        assertTrue(
            ids.containsAll(
                setOf(
                    "dashboard",
                    "notifications",
                    "pull-requests",
                    "issues",
                    "new-repository",
                    "codespaces",
                    "projects",
                    "actions-marketplace",
                    "advisories",
                    "account-security",
                    "billing"
                )
            )
        )
    }

    @Test fun validLoginPersonalizesProfileRepositoriesProjectsPackagesAndGists() {
        val destinations = allGitHubWebDestinations("  SayanthRock  ")
            .associateBy(GitHubWebDestination::id)

        assertEquals("SayanthRock", normalizedGitHubLogin("  SayanthRock  "))
        assertEquals("https://github.com/SayanthRock", destinations.getValue("profile").url)
        assertEquals(
            "https://github.com/SayanthRock?tab=repositories",
            destinations.getValue("repositories").url
        )
        assertEquals(
            "https://github.com/SayanthRock?tab=projects",
            destinations.getValue("projects").url
        )
        assertEquals(
            "https://github.com/SayanthRock?tab=packages",
            destinations.getValue("packages").url
        )
        assertEquals("https://gist.github.com/SayanthRock", destinations.getValue("gists").url)
    }

    @Test fun invalidLoginCannotInjectAHostPathOrQuery() {
        val malicious = "github.com/attacker?redirect=https://example.com"
        val urls = allGitHubWebDestinations(malicious).map(GitHubWebDestination::url)

        assertNull(normalizedGitHubLogin(malicious))
        assertFalse(urls.any { malicious in it })
        assertTrue(urls.all(GitHubUrlPolicy::isGitHubHttpsUrl))
    }

    @Test fun searchFindsSpecificToolsAcrossSections() {
        val filtered = filterGitHubWebSections(
            sections = githubWebSections("SayanthRock"),
            query = "personal access token"
        )

        assertEquals(listOf("tokens"), filtered.flatMap(GitHubWebSection::destinations).map { it.id })
    }

    @Test fun searchIsCaseInsensitiveAndKeepsAWholeMatchingSection() {
        val filtered = filterGitHubWebSections(
            sections = githubWebSections("SayanthRock"),
            query = "SECURITY"
        )

        assertTrue(filtered.any { it.id == "security" && it.destinations.size >= 6 })
    }

    @Test fun blankSearchPreservesTheCompleteCatalogue() {
        val sections = githubWebSections("SayanthRock")

        assertEquals(sections, filterGitHubWebSections(sections, "   "))
    }

    @Test fun mobileAccountCatalogueIncludesRequestedSettingsAndPlans() {
        val ids = allGitHubWebDestinations("SayanthRock").map { it.id }.toSet()

        assertTrue(setOf("copilot-settings", "enterprises", "accessibility", "feature-preview", "enterprise-trial", "github-free").all(ids::contains))
        assertEquals(45, ids.size)
    }
}
