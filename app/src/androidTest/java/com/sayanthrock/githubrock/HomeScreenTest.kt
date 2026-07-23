package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeStartsDirectlyWithDiscoveryFeedWithoutWelcomeCard() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = emptyList(),
                    runs = emptyList(),
                    onOpenRepo = {},
                    onOpenBuilds = {}
                )
            }
        }

        compose.onNodeWithText("GitHub Rock").assertDoesNotExist()
        compose.onNodeWithText("A focused GitHub control centre with a store-style discovery experience for repositories, releases and Android builds.").assertDoesNotExist()
        compose.onNodeWithText("API left").assertDoesNotExist()
        compose.onNodeWithText("Open Builds").assertDoesNotExist()
        compose.onNodeWithText("Recent").assertIsDisplayed()
        compose.onNodeWithText("No repositories found").assertIsDisplayed()
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
                    repositories = emptyList(),
                    runs = emptyList(),
                    onOpenRepo = {},
                    onOpenBuilds = {},
                    isLoading = true
                )
            }
        }

        compose.onNodeWithText("Loading your workspace…").assertIsDisplayed()
        compose.onNodeWithText("No repositories found").assertDoesNotExist()
    }
}
