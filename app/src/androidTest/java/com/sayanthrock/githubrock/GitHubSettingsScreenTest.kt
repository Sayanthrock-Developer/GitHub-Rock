package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
        var openedAppearance = false

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                GitHubSettingsScreen(
                    profile = null,
                    onOpenProfile = {},
                    onOpenAccounts = {},
                    onOpenAppearance = { openedAppearance = true },
                    onOpenDownloads = {},
                    onOpenAbout = {},
                    onOpenGitHubUrl = {},
                    onBack = {}
                )
            }
        }

        compose.onNodeWithText("Account settings").assertIsDisplayed()
        assertTextDoesNotExist("Mobile settings experience")
        assertTextDoesNotExist("45 GitHub tools available in the app")
        assertTextDoesNotExist("Profile and repositories use native screens. Every other supported GitHub setting opens inside GitHub Rock instead of an external browser.")
        assertTextDoesNotExist("Password, passkey, token, session, authorization, and billing changes remain on GitHub's secure pages inside a protected in-app panel. GitHub Rock never injects your OAuth token into web content.")

        compose.onNodeWithText("Theme & interface")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertTrue(openedAppearance) }
    }

    private fun assertTextDoesNotExist(text: String) {
        assertTrue(compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
