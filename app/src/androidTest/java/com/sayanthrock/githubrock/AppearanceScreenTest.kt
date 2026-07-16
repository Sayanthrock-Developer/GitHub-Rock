package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.ui.screens.AppearanceContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cleanStandardAppearanceControlsEmitSelections() {
        var selectedMode: ThemeMode? = null
        var selectedAccent: AccentColor? = null
        var dynamicColor = false
        var trueBlack = false

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppearanceContent(
                    state = AppearancePreferences(),
                    onBack = {},
                    onThemeMode = { selectedMode = it },
                    onAccentColor = { selectedAccent = it },
                    onDynamicColor = { dynamicColor = it },
                    onTrueBlack = { trueBlack = it }
                )
            }
        }

        compose.onNodeWithText("Clean standard").assertIsDisplayed()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Use Violet accent").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Toggle dynamic color").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Toggle true black").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(ThemeMode.Dark, selectedMode)
            assertEquals(AccentColor.Violet, selectedAccent)
            assertEquals(true, dynamicColor)
            assertEquals(true, trueBlack)
        }
    }
}
