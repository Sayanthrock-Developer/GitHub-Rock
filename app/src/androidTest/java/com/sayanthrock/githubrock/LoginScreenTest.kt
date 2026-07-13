package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.ui.DeviceAuthState
import com.sayanthrock.githubrock.ui.screens.LoginScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun primaryLoginAndGuestNavigationAreVisible() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(true, false, DeviceAuthState(), {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Login with GitHub").assertIsDisplayed()
        compose.onNodeWithContentDescription("Create GitHub account").assertIsDisplayed()
        compose.onNodeWithText("Continue as guest").assertIsDisplayed()
        compose.onNodeWithText("Explore isolated demo mode").assertIsDisplayed()
    }
}
