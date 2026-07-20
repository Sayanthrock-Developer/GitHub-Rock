package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.ui.screens.GitHubSettingsScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GitHubSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allGitHubSettingsRemainAvailableWithoutFeatureSwitches() {
        var openedAppSettings = false
        var openedUrl: String? = null

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                GitHubSettingsScreen(
                    login = "SayanthRock",
                    onOpenAppSettings = { openedAppSettings = true },
                    onOpenGitHubUrl = { openedUrl = it },
                    onBack = {}
                )
            }
        }

        compose.onNodeWithText("Everything in one place").assertIsDisplayed()
        compose.onNodeWithText("App appearance & interface").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedAppSettings) }

        compose.onNodeWithText("Password and authentication").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("https://github.com/settings/security", openedUrl)
        }

        compose.onNodeWithText("Feature controls").assertDoesNotExist()
        compose.onNodeWithText("Turn all on").assertDoesNotExist()
        compose.onNodeWithText("100 / 100").assertDoesNotExist()
    }
}
