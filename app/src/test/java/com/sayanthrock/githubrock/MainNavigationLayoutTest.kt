package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.navigation.MainNavigationLayout
import com.sayanthrock.githubrock.ui.navigation.mainNavigationLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationLayoutTest {
    @Test
    fun compactWidthsUseBottomNavigation() {
        assertEquals(MainNavigationLayout.BottomBar, mainNavigationLayout(599f))
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertEquals(MainNavigationLayout.NavigationRail, mainNavigationLayout(600f))
        assertEquals(MainNavigationLayout.NavigationRail, mainNavigationLayout(1_200f))
    }
}
