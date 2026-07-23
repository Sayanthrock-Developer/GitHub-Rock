package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.GitHubContributionDay
import com.sayanthrock.githubrock.core.model.GitHubOrganization
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubProfileSnapshot
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubSocialAccount
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.screens.ConnectedProfileDashboardUiState
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun connectedProfileOpensAsDashboardAndKeepsNativeControls() {
        var openedSettings = false
        var openedAccounts = false
        var openedGitHubUrl: String? = null
        val profile = GitHubUser(
            login = "SayanthRock",
            id = 202829406,
            name = "Sayanth Rock",
            bio = "Android developer",
            publicRepos = 24,
            followers = 120,
            following = 48
        )
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner("SayanthRock"),
            description = "Native GitHub control centre",
            language = "Kotlin",
            stars = 10,
            topics = listOf("android", "compose")
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = profile,
                    explorerState = ProfileExplorerState(
                        snapshot = GitHubProfileSnapshot(
                            profile = profile,
                            details = GitHubProfileDetails(
                                contributionsLastYear = 321,
                                contributionDays = listOf(
                                    GitHubContributionDay("2026-07-18", 4, "SECOND_QUARTILE")
                                )
                            )
                        )
                    ),
                    onOpenDownloads = {},
                    onOpenFeatures = { openedAccounts = true },
                    onOpenSettings = { openedSettings = true },
                    onOpenGitHubUrl = { openedGitHubUrl = it },
                    onLogout = {},
                    dashboardStateOverride = ConnectedProfileDashboardUiState(
                        repositories = listOf(repository)
                    )
                )
            }
        }

        compose.onNodeWithText("Sayanth Rock").assertIsDisplayed()
        compose.onNodeWithText("24").assertIsDisplayed()
        compose.onNodeWithText("Followers").performClick()
        compose.runOnIdle {
            assertEquals("https://github.com/SayanthRock?tab=followers", openedGitHubUrl)
        }

        compose.onNodeWithText("Contribution activity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("321 this year").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Search repositories…").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("GitHub-Rock").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("APP").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedSettings) }
        compose.onNodeWithText("Accounts").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedAccounts) }
    }

    @Test
    fun identityAndContributionDetailsAppearInsideProfileDashboard() {
        val profile = GitHubUser(
            login = "SayanthRock",
            id = 1,
            name = "The Octocat",
            followers = 18_000,
            following = 9,
            publicRepos = 8,
            company = "@github",
            location = "San Francisco",
            blog = "github.blog"
        )
        val details = GitHubProfileDetails(
            pronouns = "they/them",
            contributionsLastYear = 321,
            contributionDays = listOf(GitHubContributionDay("2026-07-18", 4, "SECOND_QUARTILE")),
            organizations = listOf(GitHubOrganization("github", "GitHub", url = "https://github.com/github")),
            organizationCount = 1,
            socialAccounts = listOf(
                GitHubSocialAccount(
                    displayName = "0000-0002-1825-0097",
                    provider = "GENERIC",
                    url = "https://orcid.org/0000-0002-1825-0097"
                )
            ),
            highlights = listOf("GitHub Star")
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = profile,
                    explorerState = ProfileExplorerState(
                        snapshot = GitHubProfileSnapshot(profile, details)
                    ),
                    onOpenDownloads = {},
                    onOpenFeatures = {},
                    onOpenSettings = {},
                    onOpenGitHubUrl = {},
                    onLogout = {},
                    dashboardStateOverride = ConnectedProfileDashboardUiState()
                )
            }
        }

        compose.onNodeWithText("Contribution activity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("321 this year").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("@github").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("San Francisco").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("they/them").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("github.blog").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ORCID · 0000-0002-1825-0097").performScrollTo().assertIsDisplayed()
    }
}
