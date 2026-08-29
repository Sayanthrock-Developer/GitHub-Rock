package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.ui.navigation.ModernNavigationBottomBar
import com.sayanthrock.githubrock.ui.navigation.TopDestinationV2
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun modernNavigationShowsDestinationsAndRoutesSelection() {
        var selectedDestination: TopDestinationV2? = null
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ModernNavigationBottomBar(
                    selectedRoute = TopDestinationV2.Builds.route,
                    compact = false,
                    onDestinationSelected = { selectedDestination = it }
                )
            }
        }
        compose.onNodeWithText("Builds").assertIsDisplayed()
        compose.onNodeWithContentDescription("Builds").assertIsSelected()
        compose.onNodeWithContentDescription("Repositories").performClick()
        compose.runOnIdle { assertEquals(TopDestinationV2.Repositories, selectedDestination) }
    }

    @Test
    fun inactiveDestinationsRemainIconOnly() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ModernNavigationBottomBar(
                    selectedRoute = TopDestinationV2.Home.route,
                    compact = false,
                    onDestinationSelected = {}
                )
            }
        }
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repositories").assertIsDisplayed()
    }
}
