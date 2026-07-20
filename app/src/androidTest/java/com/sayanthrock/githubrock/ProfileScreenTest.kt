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
import com.sayanthrock.githubrock.core.model.GitHubSocialAccount
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedProfileShowsCommandDeckAndNativeActions() {
        var openedFeatures = false
        var openedSettings = false
        var openedGitHubUrl: String? = null
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        publicRepos = 24,
                        followers = 120,
                        following = 48
                    ),
                    onOpenDownloads = {},
                    onOpenFeatures = { openedFeatures = true },
                    onOpenSettings = { openedSettings = true },
                    onOpenGitHubUrl = { openedGitHubUrl = it },
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Repositories").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("24").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Followers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("120").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Following").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("48").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Followers").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("https://github.com/SayanthRock?tab=followers", openedGitHubUrl)
        }

        compose.onNodeWithText("View profile on GitHub").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("GitHub settings").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(openedSettings) }

        compose.onNodeWithText("Show").performScrollTo().performClick()
        compose.onNodeWithText("Download profile").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("GitHub services").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(openedFeatures) }
        compose.onNodeWithText("GitHub security").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Repository library").assertDoesNotExist()
        compose.onNodeWithText("Find a GitHub profile").assertDoesNotExist()
        compose.onNodeWithText("GitHub username").assertDoesNotExist()
        compose.onNodeWithText("Visual developer control centre").assertDoesNotExist()
    }

    @Test fun ownProfileShowsMobileDetailsWithoutProfileSearch() {
        val viewedProfile = GitHubUser(
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
                    profile = viewedProfile,
                    explorerState = ProfileExplorerState(
                        snapshot = GitHubProfileSnapshot(viewedProfile, details)
                    ),
                    onOpenDownloads = {},
                    onOpenFeatures = {},
                    onOpenSettings = {},
                    onOpenGitHubUrl = {},
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("321 contributions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pronouns").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ORCID · 0000-0002-1825-0097").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View achievements on GitHub").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Repository library").assertDoesNotExist()
        compose.onNodeWithText("Find a GitHub profile").assertDoesNotExist()
        compose.onNodeWithText("GitHub username").assertDoesNotExist()
    }
}
