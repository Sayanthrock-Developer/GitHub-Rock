package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.*
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

    @Test fun storedAccentValuesSupportEveryPreset() {
        assertEquals(AccentColor.SystemDynamic, AccentColor.fromStored("SystemDynamic"))
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
        assertEquals(AccentColor.Purple, AccentColor.fromStored("Violet"))
        assertEquals(AccentColor.Green, AccentColor.fromStored("Emerald"))
        assertEquals(AccentColor.Pink, AccentColor.fromStored("Rose"))
        assertEquals(AccentColor.Red, AccentColor.fromStored("Coral"))
        assertEquals(AccentColor.Yellow, AccentColor.fromStored("Amber"))
        assertEquals(AccentColor.DefaultGitHubRock, AccentColor.fromStored("unknown"))
        assertEquals(AccentColor.DefaultGitHubRock, AccentColor.fromStored(null))
    }

    @Test fun expandedAccentSetIsStable() {
        assertEquals(
            listOf("SystemDynamic", "DefaultGitHubRock", "Red", "Orange", "Yellow", "Green", "Teal", "Cyan", "Blue", "Indigo", "Purple", "Pink", "Custom"),
            AccentColor.entries.map { it.name }
        )
    }

    @Test fun defaultAppearanceUsesStaticGitHubRockAccent() {
        val preferences = AppearancePreferences()
        assertEquals(AccentColor.DefaultGitHubRock, preferences.accentColor)
        assertEquals("#E60023", preferences.customAccentHex)
        assertTrue(preferences.recentCustomColors.isEmpty())
        assertFalse(preferences.dynamicColor)
        assertTrue(preferences.showImages)
        assertFalse(preferences.reduceMotion)
    }
}
