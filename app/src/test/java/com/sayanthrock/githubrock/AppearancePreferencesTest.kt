package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.ThemeMode
import org.junit.Assert.assertEquals
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
}
