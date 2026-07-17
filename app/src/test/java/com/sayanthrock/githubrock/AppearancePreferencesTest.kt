package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
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

    @Test fun storedAccentValuesFallBackToCleanCyan() {
        assertEquals(AccentColor.Violet, AccentColor.fromStored("Violet"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored("unknown"))
        assertEquals(AccentColor.Cyan, AccentColor.fromStored(null))
    }

    @Test fun nativeFeatureControlsUseSafeVisibleDefaults() {
        val preferences = AppearancePreferences()

        assertTrue(preferences.workflowPreview)
        assertTrue(preferences.workflowStepDetails)
        assertTrue(preferences.statusColors)
        assertTrue(preferences.actionsControls)
        assertTrue(preferences.repositoryManager)
        assertTrue(preferences.fileTools)
        assertTrue(preferences.compactCards)
        assertFalse(preferences.reduceMotion)
    }
}
