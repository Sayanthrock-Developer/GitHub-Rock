package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.screens.AppearanceContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visualSettingsEmitSelectionsWithoutFeatureControls() {
        var selectedMode: ThemeMode? = null
        var selectedStyle: ThemeStyle? = null
        var selectedAccent: AccentColor? = null
        var dynamicColor = false
        var trueBlack = false
        var showImages = true
        var logDisplayStyle: LogDisplayStyle? = null

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppearanceContent(
                    state = AppearancePreferences(),
                    onBack = {},
                    onThemeMode = { selectedMode = it },
                    onThemeStyle = { selectedStyle = it },
                    onAccentColor = { selectedAccent = it },
                    onDynamicColor = { dynamicColor = it },
                    onTrueBlack = { trueBlack = it },
                    onShowImages = { showImages = it },
                    onLogDisplayStyle = { logDisplayStyle = it }
                )
            }
        }

        compose.onNodeWithText("Customize your experience").assertIsDisplayed()
        compose.onNodeWithText("Liquid glass").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Use Violet accent").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Toggle Show remote images").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Toggle System dynamic color").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Toggle True black").performScrollTo().performClick()
        compose.onNodeWithText("Popup dialog").performScrollTo().performClick()

        compose.onNodeWithText("Feature controls").assertDoesNotExist()
        compose.onNodeWithText("Bulk feature controls").assertDoesNotExist()
        compose.onNodeWithText("100 / 100").assertDoesNotExist()
        compose.onNodeWithText("Turn all on").assertDoesNotExist()
        compose.onNodeWithText("Turn all off").assertDoesNotExist()
        compose.onNodeWithText("Workflow code preview").assertDoesNotExist()
        compose.onNodeWithText("File tools").assertDoesNotExist()

        compose.runOnIdle {
            assertEquals(ThemeStyle.LiquidGlass, selectedStyle)
            assertEquals(ThemeMode.Dark, selectedMode)
            assertEquals(AccentColor.Violet, selectedAccent)
            assertEquals(false, showImages)
            assertEquals(true, dynamicColor)
            assertEquals(true, trueBlack)
            assertEquals(LogDisplayStyle.Dialog, logDisplayStyle)
        }
    }
}
