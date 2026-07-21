package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.ui.navigation.AppNavigationBar
import com.sayanthrock.githubrock.ui.navigation.TopDestination
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun commandDockShowsClearLabelsAndRoutesSelection() {
        var selectedDestination: TopDestination? = null

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                AppNavigationBar(
                    selectedRoute = TopDestination.Builds.route,
                    onDestinationSelected = { selectedDestination = it }
                )
            }
        }

        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Repos").assertIsDisplayed()
        compose.onNodeWithText("Builds").assertIsDisplayed()
        compose.onNodeWithText("Downloads").assertIsDisplayed()
        compose.onNodeWithText("Profile").assertIsDisplayed()

        compose.onNodeWithContentDescription("Builds").assertIsSelected()
        compose.onNodeWithContentDescription("Repositories").performClick()
        compose.runOnIdle {
            assertEquals(TopDestination.Repositories, selectedDestination)
        }
    }
}
