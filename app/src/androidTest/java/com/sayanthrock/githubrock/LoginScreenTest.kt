package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.feature.auth.AuthUiState
import com.sayanthrock.githubrock.feature.auth.LoginScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun loginAndGuestActionsAreVisible() {
        composeRule.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    state = AuthUiState(initializing = false, clientConfigured = true),
                    onLogin = {},
                    onGuest = {},
                    onDemo = {}
                )
            }
        }

        composeRule.onNodeWithText("Login with GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as guest").assertIsDisplayed()
        composeRule.onNodeWithText("Explore demo mode").assertIsDisplayed()
    }
}
