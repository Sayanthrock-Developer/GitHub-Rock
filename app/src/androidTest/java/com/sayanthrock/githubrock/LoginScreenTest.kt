package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.navigation.GITHUB_SIGN_UP_URL
import com.sayanthrock.githubrock.ui.DeviceAuthState
import com.sayanthrock.githubrock.ui.screens.LoginScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                    onCheckAuthorization = {},
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
                    onCheckAuthorization = {},
                    onGuest = {},
                    onDemo = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Create GitHub account").performClick()
        compose.runOnIdle {
            assertEquals(GITHUB_SIGN_UP_URL, openedUrl)
        }
    }

    @Test fun authorizedUserCanRequestAnImmediateStatusCheck() {
        var checked = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    configured = true,
                    loading = false,
                    auth = DeviceAuthState(
                        code = DeviceCodeResponse(
                            deviceCode = "device-code",
                            userCode = "ABCD-EFGH",
                            verificationUri = "https://github.com/login/device",
                            expiresIn = 900,
                            interval = 5
                        ),
                        status = "Waiting for approval on GitHub…"
                    ),
                    onLogin = {},
                    onOpenGitHubUrl = {},
                    onCheckAuthorization = { checked = true },
                    onGuest = {},
                    onDemo = {}
                )
            }
        }

        compose.onNodeWithText("I’ve authorized — check now").performClick()
        compose.runOnIdle {
            assertTrue(checked)
        }
        compose.onNodeWithText("Use guest mode instead").assertIsDisplayed()
        compose.onNodeWithText(
            "GitHub shows the approximate city and IP that requested this code. Authorize only if it matches the network you are using; otherwise cancel."
        ).assertIsDisplayed()
    }
}
