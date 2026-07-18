package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.sayanthrock.githubrock.ui.screens.FeaturePreviewScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeaturePreviewScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun featureCatalogueShowsCoreWorkspacesAndBackNavigation() {
        var wentBack = false
        var openedUrl: String? = null
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                FeaturePreviewScreen(
                    login = "SayanthRock",
                    onOpenGitHubUrl = { openedUrl = it },
                    onBack = { wentBack = true }
                )
            }
        }

        compose.onNodeWithText("Native tools plus the complete GitHub website").assertIsDisplayed()
        compose.onNodeWithText("Complete access, safely").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open GitHub.com").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("https://github.com/", openedUrl) }

        compose.onNodeWithText("Notifications").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("https://github.com/notifications", openedUrl) }

        compose.onNodeWithText("Create & code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Codespaces").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Password and authentication").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Billing and plans").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Native product coverage").performScrollTo().assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }

    @Test fun githubToolSearchFiltersTheLargeWebCatalogue() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                FeaturePreviewScreen(
                    login = "SayanthRock",
                    onOpenGitHubUrl = {},
                    onBack = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Search GitHub tools")
            .performScrollTo()
            .performTextInput("personal access token")

        compose.onNodeWithText("Access tokens").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("1 of 39 tools").assertIsDisplayed()
    }
}
