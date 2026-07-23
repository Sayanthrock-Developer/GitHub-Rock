package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.ui.screens.GitHubSettingsScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GitHubSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsOpenWithoutMobileExperienceCard() {
        var openedAppSettings = false

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                GitHubSettingsScreen(
                    login = "SayanthRock",
                    onOpenAppSettings = { openedAppSettings = true },
                    onOpenGitHubUrl = {},
                    onBack = {}
                )
            }
        }

        compose.onNodeWithText("GitHub settings").assertIsDisplayed()
        compose.onNodeWithText("Mobile settings experience").assertDoesNotExist()
        compose.onNodeWithText("45 GitHub tools available in the app").assertDoesNotExist()
        compose.onNodeWithText(
            "Profile and repositories use native screens. Every other supported GitHub setting opens inside GitHub Rock instead of an external browser."
        ).assertDoesNotExist()
        compose.onNodeWithText(
            "Password, passkey, token, session, authorization, and billing changes remain on GitHub's secure pages inside a protected in-app panel. GitHub Rock never injects your OAuth token into web content."
        ).assertDoesNotExist()

        compose.onNodeWithText("App appearance & interface")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertTrue(openedAppSettings) }
    }
}
