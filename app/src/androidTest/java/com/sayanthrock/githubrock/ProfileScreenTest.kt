package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedProfileShowsFollowerDistribution() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        publicRepos = 24,
                        followers = 120,
                        following = 73
                    ),
                    followers = listOf(Owner("octo-demo")),
                    following = listOf(Owner("github")),
                    socialLoading = false,
                    socialError = null,
                    onRetrySocial = {},
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("120").assertIsDisplayed()
        compose.onNodeWithText("73").assertIsDisplayed()
        compose.onNodeWithText("@octo-demo").assertIsDisplayed()
        compose.onNodeWithText("@github").assertIsDisplayed()
    }
}
