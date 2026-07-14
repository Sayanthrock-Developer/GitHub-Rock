package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.RepositoryShowcaseContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepositoryShowcaseScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun applicationRepositoryShowsIdentityDetailsAndReadme() {
        var openedWorkspace = false
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner(
                login = "SayanthRock",
                avatarUrl = "https://avatars.githubusercontent.com/u/202829406?v=4"
            ),
            description = "Premium GitHub developer control centre",
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
                    readme = "# GitHub Rock\n\nNative Android repository management.",
                    loading = false,
                    readmeLoading = false,
                    error = null,
                    readmeError = null,
                    onRetry = {},
                    onOpenWorkspace = { openedWorkspace = true },
                    onOpenGitHub = {}
                )
            }
        }

        compose.onNodeWithContentDescription("GitHub-Rock application icon").assertIsDisplayed()
        compose.onNodeWithText("Application").assertIsDisplayed()
        compose.onNodeWithText("About this project").assertIsDisplayed()
        compose.onNodeWithText("Developer workspace").performClick()
        compose.runOnIdle { assertTrue(openedWorkspace) }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("README.md"))
        compose.onNodeWithText("README.md").assertIsDisplayed()
        compose.onNodeWithText("Project documentation").assertIsDisplayed()
    }
}
