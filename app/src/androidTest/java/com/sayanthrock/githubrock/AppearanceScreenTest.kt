package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.NavigationStyle
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.screens.AppearanceContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visualSettingsEmitSelections() {
        var selectedMode: ThemeMode? = null
        var selectedStyle: ThemeStyle? = null
        var selectedNavigation: NavigationStyle? = null
        var selectedAccent: AccentColor? = null
        var dynamicColor = true
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
                    onNavigationStyle = { selectedNavigation = it },
                    onAccentColor = { selectedAccent = it },
                    onDynamicColor = { dynamicColor = it },
                    onTrueBlack = { trueBlack = it },
                    onShowImages = { showImages = it },
                    onLogDisplayStyle = { logDisplayStyle = it },
                    onDisplaySize = {},
                    onFontSize = {},
                    onFontWeight = {},
                    onFontFamily = {},
                    onLoadingStyle = {},
                    onCodeColorStyle = {},
                    onReset = {}
                )
            }
        }

        compose.onNodeWithText("Make GitHub Rock yours").assertIsDisplayed()
        compose.onNodeWithText("Liquid Glass").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithText("Pill").performScrollTo().performClick()
        compose.onNodeWithText("Violet").performScrollTo().performClick()
        compose.onNodeWithText("On").performScrollTo().performClick()
        compose.onNodeWithText("Popup dialog").performScrollTo().performClick()

        compose.onNodeWithText("Feature controls").assertDoesNotExist()
        compose.onNodeWithText("Bulk feature controls").assertDoesNotExist()

        compose.runOnIdle {
            assertEquals(ThemeStyle.LiquidGlass, selectedStyle)
            assertEquals(ThemeMode.Dark, selectedMode)
            assertEquals(NavigationStyle.Pill, selectedNavigation)
            assertEquals(AccentColor.Violet, selectedAccent)
            assertEquals(true, dynamicColor)
            assertEquals(LogDisplayStyle.Dialog, logDisplayStyle)
            assertEquals(true, showImages)
            assertEquals(false, trueBlack)
        }
    }
}
