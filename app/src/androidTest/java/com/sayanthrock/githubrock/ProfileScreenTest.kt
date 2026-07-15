package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedProfileShowsRepositoryCountAndFeaturePreview() {
        var openedFeatures = false
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        publicRepos = 24,
                        followers = 120,
                        following = 48
                    ),
                    onOpenFeatures = { openedFeatures = true },
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Repositories").assertIsDisplayed()
        compose.onNodeWithText("24").assertIsDisplayed()
        compose.onNodeWithText("Followers").assertIsDisplayed()
        compose.onNodeWithText("120").assertIsDisplayed()
        compose.onNodeWithText("Following").assertIsDisplayed()
        compose.onNodeWithText("48").assertIsDisplayed()
        compose.onNodeWithText("Follow on GitHub").assertIsDisplayed()
        compose.onNodeWithText("Explore all GitHub features").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(openedFeatures) }
        compose.onNodeWithText("Security").performScrollTo().assertIsDisplayed()
    }
}
