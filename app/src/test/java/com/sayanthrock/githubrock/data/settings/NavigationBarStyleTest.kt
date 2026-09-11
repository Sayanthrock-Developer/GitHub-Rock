package com.sayanthrock.githubrock.data.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationBarStyleTest {
    @Test
    fun keeps_existing_styles_and_adds_ios_style() {
        assertEquals(
            listOf(
                NavigationBarStyle.FloatingCapsule,
                NavigationBarStyle.Classic,
                NavigationBarStyle.Minimal,
                NavigationBarStyle.Glass,
                NavigationBarStyle.Compact,
                NavigationBarStyle.Ios,
            ),
            NavigationBarStyle.entries,
        )
    }

    @Test
    fun stored_ios_value_restores_ios_style() {
        assertEquals(NavigationBarStyle.Ios, NavigationBarStyle.fromStored("Ios"))
        assertTrue(NavigationBarStyle.fromStored("unknown") == NavigationBarStyle.FloatingCapsule)
    }
}
