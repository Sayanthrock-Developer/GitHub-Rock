package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.RepositoryCard
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepositoryCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repositoryShowsOwnerMetricsAndUsesWholeCardAction() {
        var opened = false
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "Rock-Wedding",
            fullName = "SayanthRock/Rock-Wedding",
            owner = Owner(
                login = "SayanthRock",
                avatarUrl = "https://avatars.githubusercontent.com/u/202829406?v=4"
            ),
            description = "Premium digital wedding invitation studio",
            isTemplate = true,
            language = "TypeScript",
            stars = 174,
            forks = 19,
            openIssues = 4,
            topics = listOf("template")
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryCard(repository) { opened = true }
            }
        }

        compose.onNodeWithContentDescription("SayanthRock avatar").assertIsDisplayed()
        compose.onNodeWithText("TypeScript", substring = true).assertIsDisplayed()
        compose.onNodeWithText("174", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Premium digital wedding invitation studio").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
