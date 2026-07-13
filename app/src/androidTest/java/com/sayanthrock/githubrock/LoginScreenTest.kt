package com.sayanthrock.githubrock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        compose.onNodeWithText("Continue with public repositories").assertIsDisplayed()
        compose.onNodeWithText("Explore isolated demo mode").assertIsDisplayed()
    }

    @Test fun createAccountButtonOpensOfficialSignupPageAndOffersConnection() {
        var openedUrl: String? = null
        var loginStarted = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    configured = true,
                    loading = false,
                    auth = DeviceAuthState(),
                    onLogin = { loginStarted = true },
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
        compose.onNodeWithContentDescription("Connect new GitHub account").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(loginStarted)
        }
    }

    @Test fun aFreshDeviceCodeReopensGitHubOnce() {
        var authState by mutableStateOf(
            DeviceAuthState(
                code = DeviceCodeResponse(
                    deviceCode = "first-device-code",
                    userCode = "ABCD-EFGH",
                    verificationUri = "https://github.com/login/device",
                    expiresIn = 900
                )
            )
        )
        val openedUrls = mutableListOf<String>()

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                LoginScreen(
                    configured = true,
                    loading = false,
                    auth = authState,
                    onLogin = {},
                    onOpenGitHubUrl = { openedUrls += it },
                    onCheckAuthorization = {},
                    onGuest = {},
                    onDemo = {}
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, openedUrls.size)
            authState = authState.copy(
                code = requireNotNull(authState.code).copy(
                    deviceCode = "second-device-code",
                    userCode = "IJKL-MNOP"
                )
            )
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(2, openedUrls.size)
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

        compose.onNodeWithText(
            "After GitHub says you’re all set, return with Android Back or the app switcher. GitHub Rock checks automatically; the button below is a backup."
        ).assertIsDisplayed()
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
