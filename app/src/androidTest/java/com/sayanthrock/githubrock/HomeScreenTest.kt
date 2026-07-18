package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.model.RateLimit
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedDashboardShowsRealHealthAndBuildAction() {
        var openedBuilds = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        name = "Sayanth Rock"
                    ),
                    rateLimit = RateLimit(limit = 5_000, remaining = 4_862, reset = 0),
                    repositories = emptyList(),
                    runs = emptyList(),
                    onOpenRepo = {},
                    onOpenBuilds = { openedBuilds = true }
                )
            }
        }

        compose.onNodeWithText("Workspace levels").assertIsDisplayed()
        compose.onNodeWithText("Account data").assertIsDisplayed()
        compose.onNodeWithText("100 / 100").assertIsDisplayed()
        compose.onNodeWithText("GitHub API capacity").assertIsDisplayed()
        compose.onNodeWithText("97 / 100").assertIsDisplayed()
        compose.onNodeWithText("GitHub Rock").assertDoesNotExist()
        compose.onNodeWithText("CONNECTED").assertDoesNotExist()
        compose.onNodeWithText("Build APK").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedBuilds) }
    }

    @Test fun repositoryDescriptionUsesFullCardTouchTarget() {
        var openedRepository = false
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner(login = "SayanthRock"),
            description = "Native Android GitHub control centre"
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    profile = null,
                    rateLimit = null,
                    repositories = listOf(repository),
                    runs = emptyList(),
                    onOpenRepo = { openedRepository = it.id == repository.id },
                    onOpenBuilds = {}
                )
            }
        }

        compose.onNodeWithText("Native Android GitHub control centre").performClick()
        compose.runOnIdle { assertTrue(openedRepository) }
    }

    @Test fun loadingDashboardShowsProgressInsteadOfFalseEmptyState() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    profile = null,
                    rateLimit = null,
                    repositories = emptyList(),
                    runs = emptyList(),
                    onOpenRepo = {},
                    onOpenBuilds = {},
                    isLoading = true
                )
            }
        }

        compose.onNodeWithText("Loading your GitHub workspace…").assertIsDisplayed()
        compose.onNodeWithText("No repositories to show. Pull down to refresh.").assertDoesNotExist()
    }
}
