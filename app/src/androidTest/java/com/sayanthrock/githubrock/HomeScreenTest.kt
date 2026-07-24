package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

    @Test fun homeStartsWithPlatformAndCategoryDiscovery() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = emptyList(),
                    onOpenRepo = {},
                )
            }
        }

        compose.onNodeWithText("GitHub Rock").assertIsDisplayed()
        compose.onNodeWithText("Browsing").assertIsDisplayed()
        compose.onNodeWithText("All Platforms").assertIsDisplayed()
        compose.onAllNodesWithText("All")[0].assertIsDisplayed()
        compose.onNodeWithText("AI").assertIsDisplayed()
        compose.onNodeWithText("Privacy").assertIsDisplayed()
        compose.onNodeWithText("Updated").assertIsDisplayed()
        compose.onNodeWithText("No public repositories found").assertIsDisplayed()
        compose.onNodeWithText("A focused GitHub control centre with a store-style discovery experience for repositories, releases and Android builds.")
            .assertDoesNotExist()
    }

    @Test fun platformButtonOpensOperatingSystemPicker() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = emptyList(),
                    onOpenRepo = {},
                )
            }
        }

        compose.onNodeWithText("All Platforms").performClick()
        compose.onNodeWithText("Choose your OS").assertIsDisplayed()
        compose.onNodeWithText("Android").assertIsDisplayed().performClick()
        compose.onNodeWithText("Android").assertIsDisplayed()
    }

    @Test fun repositoryDescriptionUsesFullCardTouchTarget() {
        var openedRepository = false
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner(login = "SayanthRock"),
            description = "Native Android GitHub control centre",
            language = "Kotlin",
            topics = listOf("android", "developer-tool"),
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = listOf(repository),
                    onOpenRepo = { openedRepository = it.id == repository.id },
                )
            }
        }

        compose.onNodeWithText("Native Android GitHub control centre").performClick()
        compose.runOnIdle { assertTrue(openedRepository) }
    }

    @Test fun privateRepositoriesAreNotExposedOnHome() {
        val privateRepository = GitHubRepositoryModel(
            id = 2,
            name = "Private-Rock",
            fullName = "SayanthRock/Private-Rock",
            owner = Owner(login = "SayanthRock"),
            description = "Private repository content",
            private = true,
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = listOf(privateRepository),
                    onOpenRepo = {},
                )
            }
        }

        compose.onNodeWithText("Private-Rock").assertDoesNotExist()
        compose.onNodeWithText("No public repositories found").assertIsDisplayed()
    }

    @Test fun loadingHomeShowsProgressInsteadOfFalseEmptyState() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    repositories = emptyList(),
                    onOpenRepo = {},
                    isLoading = true,
                )
            }
        }

        compose.onNodeWithText("Loading your workspace…").assertIsDisplayed()
        compose.onNodeWithText("No public repositories found").assertDoesNotExist()
    }
}
