package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedProfileShowsRepositoryCountWithoutSocialSections() {
        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = GitHubUser(
                        login = "SayanthRock",
                        id = 202829406,
                        publicRepos = 24
                    ),
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Public repositories").assertIsDisplayed()
        compose.onNodeWithText("24").assertIsDisplayed()
        compose.onNodeWithText("Followers").assertDoesNotExist()
        compose.onNodeWithText("Following").assertDoesNotExist()
    }
}
