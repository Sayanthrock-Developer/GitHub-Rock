package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.screens.AppearanceContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visualSettingsAndAccentCustomizationEmitSelections() {
        var selectedMode: ThemeMode? = null
        var selectedStyle: ThemeStyle? = null
        var selectedAccent: AccentColor? = null
        var customHex = "#E60023"
        var savedHex: String? = null
        var showImages = true
        var logDisplayStyle: LogDisplayStyle? = null

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppearanceContent(
                    state = AppearancePreferences(), onBack = {},
                    onThemeMode = { selectedMode = it }, onAccentColor = { selectedAccent = it },
                    onCustomAccentHex = { customHex = it }, onSaveCustomAccent = { savedHex = it },
                    onDynamicColor = {}, onTrueBlack = {}, onThemeStyle = { selectedStyle = it },
                    onShowImages = { showImages = it }, onLogDisplayStyle = { logDisplayStyle = it }
                )
            }
        }

        compose.onNodeWithText("Customize your experience").assertIsDisplayed()
        compose.onNodeWithText("Liquid glass").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithText("Purple").performScrollTo().performClick()
        compose.onNodeWithText("Custom Color").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Custom accent HEX input").assertIsDisplayed().performTextReplacement("#FF3366")
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithContentDescription("Toggle Show remote images").performScrollTo().performClick()
        compose.onNodeWithText("Popup dialog").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(ThemeStyle.LiquidGlass, selectedStyle)
            assertEquals(ThemeMode.Dark, selectedMode)
            assertEquals(AccentColor.Custom, selectedAccent)
            assertEquals("#FF3366", customHex)
            assertEquals("#FF3366", savedHex)
            assertFalse(showImages)
            assertEquals(LogDisplayStyle.Dialog, logDisplayStyle)
        }
    }
}
