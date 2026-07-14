package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.ui.screens.FeaturePreviewScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeaturePreviewScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun featureCatalogueShowsCoreWorkspacesAndBackNavigation() {
        var wentBack = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                FeaturePreviewScreen(onBack = { wentBack = true })
            }
        }

        compose.onNodeWithText("One mobile control centre for the complete GitHub workflow").assertIsDisplayed()
        compose.onNodeWithText("Access & identity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Repositories & code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Actions & Android builds").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Customer workflows").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear feature status").performScrollTo().assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }
}
