package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.AppInformation
import com.sayanthrock.githubrock.ui.screens.AppInformationContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class AppInformationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun appAndSdkDetailsRemainReadableOnAPhone() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppInformationContent(
                    information = AppInformation(
                        appName = "GitHub Rock",
                        versionName = "1.0",
                        versionCode = 10,
                        applicationId = "com.sayanthrock.githubrock",
                        buildType = "debug",
                        minimumSdk = 29,
                        targetSdk = 36,
                        deviceSdk = 36,
                        androidVersion = "16",
                        securityPatch = "2026-07-01",
                        device = "Google Pixel",
                        supportedAbis = listOf("arm64-v8a"),
                        firstInstalled = "18 Jul 2026, 12:00",
                        lastUpdated = "18 Jul 2026, 12:30",
                        requestedPermissions = 11
                    ),
                    onBack = {},
                    onOpenCapabilities = {},
                    onOpenSystemSettings = {}
                )
            }
        }

        compose.onNodeWithText("SDK information").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Target Android").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("API 36", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Android capabilities & permissions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open Android app settings").performScrollTo().assertIsDisplayed()
    }
}
