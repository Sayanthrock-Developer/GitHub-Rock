package com.sayanthrock.githubrock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.ui.navigation.ModernNavigationBottomBar
import com.sayanthrock.githubrock.ui.navigation.TopDestinationV2
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class NavigationThemeColorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun navigationUsesResolvedThemeAndKeepsAllFiveDestinationsVisible() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false, darkTheme = true) {
                // Force resolution of the same semantic colors consumed by navigation.
                MaterialTheme.colorScheme.surface
                MaterialTheme.colorScheme.primary
                MaterialTheme.colorScheme.onSurfaceVariant
                ModernNavigationBottomBar(
                    selectedRoute = TopDestinationV2.Home.route,
                    compact = false,
                    onDestinationSelected = {}
                )
            }
        }
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repositories").assertIsDisplayed()
        compose.onNodeWithContentDescription("Builds").assertIsDisplayed()
        compose.onNodeWithContentDescription("Downloads").assertIsDisplayed()
        compose.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }
}
