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
        assertEquals(ThemeStyle.Clean, ThemeStyle.fromStored("unknown"))
        assertEquals(ThemeStyle.Clean, ThemeStyle.fromStored(null))
    }

    @Test fun storedAccentValuesFallBackToCleanCyan() {
        assertEquals(AccentColor.Violet, AccentColor.fromStored("Violet"))
        assertEquals(AccentColor.Emerald, AccentColor.fromStored("Emerald"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored("unknown"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored(null))
    }

    @Test fun displayAndTypographyValuesUseStandardFallbacks() {
        assertEquals(DisplaySize.Standard, DisplaySize.fromStored("unknown"))
        assertEquals(FontSize.Default, FontSize.fromStored(null))
        assertEquals(FontWeightStyle.Default, FontWeightStyle.fromStored("unknown"))
        assertEquals(AppFontFamily.SystemSans, AppFontFamily.fromStored(null))
        assertEquals(LoadingStyle.Spinner, LoadingStyle.fromStored("unknown"))
        assertEquals(CodeColorStyle.Classic, CodeColorStyle.fromStored(null))
        assertEquals(LogDisplayStyle.Terminal, LogDisplayStyle.fromStored(null))
        assertEquals(LogDisplayStyle.Dialog, LogDisplayStyle.fromStored("Dialog"))
    }

    @Test fun nativeFeatureControlsUseSafeVisibleDefaults() {
        val preferences = AppearancePreferences()

        assertEquals(ThemeStyle.Clean, preferences.themeStyle)
        assertEquals(DisplaySize.Standard, preferences.displaySize)
        assertEquals(FontSize.Default, preferences.fontSize)
        assertEquals(FontWeightStyle.Default, preferences.fontWeight)
        assertEquals(AppFontFamily.SystemSans, preferences.fontFamily)
        assertEquals(LoadingStyle.Spinner, preferences.loadingStyle)
        assertEquals(CodeColorStyle.Classic, preferences.codeColorStyle)
        assertEquals(LogDisplayStyle.Terminal, preferences.logDisplayStyle)
        assertTrue(preferences.showImages)
        assertFalse(preferences.workflowPreview)
        assertFalse(preferences.workflowStepDetails)
        assertFalse(preferences.statusColors)
        assertFalse(preferences.actionsControls)
        assertFalse(preferences.repositoryManager)
        assertFalse(preferences.fileTools)
        assertFalse(preferences.compactCards)
        assertEquals(0, preferences.enabledFeatureControlCount)
        assertEquals(0, preferences.featureControlLevel)
        assertFalse(preferences.reduceMotion)
    }

    @Test fun featureControlLevelReachesOneHundredWhenAllOptionalToolsAreOn() {
        val preferences = AppearancePreferences(
            workflowPreview = true,
            workflowStepDetails = true,
            statusColors = true,
            actionsControls = true,
            repositoryManager = true,
            fileTools = true,
            compactCards = true
        )

        assertEquals(7, preferences.enabledFeatureControlCount)
        assertEquals(100, preferences.featureControlLevel)
    }
}
