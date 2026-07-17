package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.components.AppBrandBanner
import com.sayanthrock.githubrock.ui.components.RepositoryGalleryCard
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepositoryCardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun visualRepositoryShowsPreviewOwnerTemplateMetricsAndWholeCardAction() {
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
            topics = listOf("template"),
            previewImageUrl = "https://opengraph.githubassets.com/test/SayanthRock/Rock-Wedding"
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryGalleryCard(repository) { opened = true }
            }
        }

        compose.onNodeWithContentDescription("SayanthRock/Rock-Wedding repository preview image").assertIsDisplayed()
        compose.onNodeWithContentDescription("SayanthRock profile logo").assertIsDisplayed()
        compose.onNodeWithText("Template repository").assertIsDisplayed()
        compose.onNodeWithText("TypeScript", substring = true).assertIsDisplayed()
        compose.onNodeWithText("174", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Premium digital wedding invitation studio").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun appBrandBannerShowsApplicationIconAndVisualFeatureScope() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppBrandBanner()
            }
        }

        compose.onNodeWithContentDescription("GitHub Rock application icon").assertIsDisplayed()
        compose.onNodeWithText("GitHub Rock").assertIsDisplayed()
        compose.onNodeWithText("Profiles • Templates • Actions • Releases", substring = true).assertIsDisplayed()
    }
}
