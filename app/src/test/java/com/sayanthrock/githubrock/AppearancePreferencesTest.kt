package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test fun storedThemeValuesFallBackSafely() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromStored("Dark"))
        assertEquals(ThemeMode.System, ThemeMode.fromStored("unknown"))
        assertEquals(ThemeMode.System, ThemeMode.fromStored(null))
    }

    @Test fun storedStyleValuesFallBackToClean() {
        assertEquals(ThemeStyle.LiquidGlass, ThemeStyle.fromStored("LiquidGlass"))
        assertEquals(ThemeStyle.Midnight, ThemeStyle.fromStored("Midnight"))
        assertEquals(ThemeStyle.Obsidian, ThemeStyle.fromStored("Obsidian"))
        assertEquals(ThemeStyle.Clean, ThemeStyle.fromStored("unknown"))
        assertEquals(ThemeStyle.Clean, ThemeStyle.fromStored(null))
    }

    @Test fun storedAccentValuesSupportCompletePaletteAndSafeFallback() {
        assertEquals(AccentColor.DefaultGitHubRock, AccentColor.fromStored("DefaultGitHubRock"))
        assertEquals(AccentColor.Red, AccentColor.fromStored("Red"))
        assertEquals(AccentColor.Orange, AccentColor.fromStored("Orange"))
        assertEquals(AccentColor.Yellow, AccentColor.fromStored("Yellow"))
        assertEquals(AccentColor.Green, AccentColor.fromStored("Green"))
        assertEquals(AccentColor.Teal, AccentColor.fromStored("Teal"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored("Cyan"))
        assertEquals(AccentColor.Blue, AccentColor.fromStored("Blue"))
        assertEquals(AccentColor.Indigo, AccentColor.fromStored("Indigo"))
        assertEquals(AccentColor.Purple, AccentColor.fromStored("Purple"))
        assertEquals(AccentColor.Pink, AccentColor.fromStored("Pink"))
        assertEquals(AccentColor.Custom, AccentColor.fromStored("Custom"))
        assertEquals(AccentColor.Violet, AccentColor.fromStored("Violet"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored("unknown"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored(null))
    }

    @Test fun appearanceDefaultsUseDefaultGitHubRockAccent() {
        val preferences = AppearancePreferences()
        assertEquals(AccentColor.DefaultGitHubRock, preferences.accentColor)
        assertEquals("", preferences.customAccentHex)
        assertTrue(preferences.recentAccentColors.isEmpty())
        assertTrue(preferences.dynamicColor)
    }

    @Test fun accentHexValidationAcceptsSixAndEightDigitHex() {
        fun valid(value: String): Boolean {
            val raw = value.trim().removePrefix("#")
            return (raw.length == 6 || raw.length == 8) && raw.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
        assertTrue(valid("#52D3DC"))
        assertTrue(valid("#FF52D3DC"))
        assertFalse(valid("#52D3D"))
        assertFalse(valid("#GGGGGG"))
        assertFalse(valid("#123456789"))
    }

    @Test fun displayAndTypographyValuesUseStandardFallbacks() {
        assertEquals(DisplaySize.Standard, DisplaySize.fromStored("unknown"))
        assertEquals(FontSize.Default, FontSize.fromStored(null))
        assertEquals(FontWeightStyle.Default, FontWeightStyle.fromStored("unknown"))
        assertEquals(AppFontFamily.SystemSans, AppFontFamily.fromStored(null))
        assertEquals(LoadingStyle.Spinner, LoadingStyle.fromStored("unknown"))
        assertEquals(LoadingStyle.Pulse, LoadingStyle.fromStored("Pulse"))
        assertEquals(LoadingStyle.Liquid, LoadingStyle.fromStored("Liquid"))
        assertEquals(LoadingStyle.Orbit, LoadingStyle.fromStored("Orbit"))
        assertEquals(LoadingStyle.Shimmer, LoadingStyle.fromStored("Shimmer"))
        assertEquals(LoadingStyle.Morph, LoadingStyle.fromStored("Morph"))
        assertEquals(CodeColorStyle.Classic, CodeColorStyle.fromStored(null))
        assertEquals(LogDisplayStyle.Terminal, LogDisplayStyle.fromStored(null))
        assertEquals(LogDisplayStyle.Dialog, LogDisplayStyle.fromStored("Dialog"))
    }

    @Test fun allExistingAndNewLoadingOptionsRemainAvailable() {
        assertEquals(
            listOf("Spinner", "Linear", "Pulse", "Skeleton", "Liquid", "Orbit", "Shimmer", "Morph"),
            LoadingStyle.entries.map { it.name }
        )
    }

    @Test fun nativeToolsAreAlwaysAvailableWithoutFeatureControls() {
        val preferences = AppearancePreferences()
        assertEquals(ThemeStyle.Clean, preferences.themeStyle)
        assertEquals(DisplaySize.Standard, preferences.displaySize)
        assertEquals(FontSize.Default, preferences.fontSize)
        assertEquals(FontWeightStyle.Default, preferences.fontWeight)
        assertEquals(AppFontFamily.SystemSans, preferences.fontFamily)
        assertEquals(LoadingStyle.Spinner, preferences.loadingStyle)
        assertEquals(CodeColorStyle.Classic, preferences.codeColorStyle)
        assertEquals(LogDisplayStyle.Terminal, preferences.logDisplayStyle)
        assertTrue(preferences.dynamicColor)
        assertTrue(preferences.showImages)
        assertTrue(preferences.workflowPreview)
        assertTrue(preferences.workflowStepDetails)
        assertTrue(preferences.statusColors)
        assertTrue(preferences.actionsControls)
        assertTrue(preferences.repositoryManager)
        assertTrue(preferences.fileTools)
        assertFalse(preferences.compactCards)
        assertFalse(preferences.reduceMotion)
    }
}
