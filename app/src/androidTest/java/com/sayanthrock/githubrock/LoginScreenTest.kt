package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.ui.DeviceAuthState
import com.sayanthrock.githubrock.ui.screens.LoginScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun primaryLoginAndGuestNavigationAreVisible() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    configured = true,
                    loading = false,
                    auth = DeviceAuthState(),
                    onLogin = {},
                    onOpenGitHubUrl = {},
                    onGuest = {},
                    onDemo = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Login with GitHub").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create GitHub account").assertIsDisplayed()
        compose.onNodeWithText("Continue as guest").assertIsDisplayed()
        compose.onNodeWithText("Explore isolated demo mode").assertIsDisplayed()
    }

    @Test fun createAccountButtonOpensOfficialSignupPage() {
        var openedUrl: String? = null
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    configured = true,
                    loading = false,
                    auth = DeviceAuthState(),
                    onLogin = {},
                    onOpenGitHubUrl = { openedUrl = it },
                    onGuest = {},
                    onDemo = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Create GitHub account").performClick()
        compose.runOnIdle {
            assertEquals("https://github.com/signup", openedUrl)
        }
    }
}
