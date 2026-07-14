package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.RepositoryShowcaseContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class RepositoryShowcaseScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun applicationRepositoryShowsPreviewWithoutDeveloperWorkspaceOption() {
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner(
                login = "SayanthRock",
                avatarUrl = "https://avatars.githubusercontent.com/u/202829406?v=4"
            ),
            description = "Premium GitHub developer control centre",
            htmlUrl = "https://github.com/SayanthRock/GitHub-Rock",
            language = "Kotlin",
            stars = 386,
            forks = 42,
            openIssues = 8,
            topics = listOf("android", "jetpack-compose"),
            isTemplate = false
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryShowcaseContent(
                    repository = repository,
                    readme = null,
                    loading = false,
                    readmeLoading = true,
                    error = null,
                    readmeError = null,
                    onRetry = {},
                    onOpenGitHub = {}
                )
            }
        }

        compose.onNodeWithContentDescription("GitHub-Rock application icon").assertIsDisplayed()
        compose.onNodeWithText("Application").assertIsDisplayed()
        compose.onNodeWithText("About this project").assertIsDisplayed()
        compose.onNodeWithText("Open on GitHub").assertIsDisplayed()
        compose.onNodeWithText("Developer workspace").assertDoesNotExist()
    }

    @Test fun readmeSectionRendersProjectDocumentation() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryShowcaseContent(
                    repository = null,
                    readme = "# GitHub Rock\n\nNative Android repository management.",
                    loading = false,
                    readmeLoading = false,
                    error = null,
                    readmeError = null,
                    onRetry = {},
                    onOpenGitHub = {}
                )
            }
        }

        compose.onNodeWithText("README.md").assertIsDisplayed()
        compose.onNodeWithText("Project documentation").assertIsDisplayed()
        compose.onNodeWithText("GitHub Rock").assertIsDisplayed()
        compose.onNodeWithText("Native Android repository management.").assertIsDisplayed()
    }
}
