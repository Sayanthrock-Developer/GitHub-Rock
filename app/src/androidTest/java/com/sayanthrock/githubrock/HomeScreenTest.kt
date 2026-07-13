package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RateLimit
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedDashboardShowsRealHealthAndBuildAction() {
        var openedBuilds = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                HomeScreen(
                    mode = AppMode.Connected,
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        name = "Sayanth Rock",
                        followers = 248
                    ),
                    rateLimit = RateLimit(limit = 5_000, remaining = 4_862, reset = 0),
                    repositories = emptyList(),
                    runs = emptyList(),
                    onOpenRepo = {},
                    onOpenBuilds = { openedBuilds = true }
                )
            }
        }

        compose.onNodeWithText("CONNECTED").assertIsDisplayed()
        compose.onNodeWithText("Sayanth Rock").assertIsDisplayed()
        compose.onNodeWithText("GitHub API health").assertIsDisplayed()
        compose.onNodeWithText("4862 / 5000").assertIsDisplayed()
        compose.onNodeWithText("Build APK").performClick()
        compose.runOnIdle { assertTrue(openedBuilds) }
    }
}
