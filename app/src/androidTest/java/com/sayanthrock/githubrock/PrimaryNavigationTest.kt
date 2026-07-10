package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import com.sayanthrock.githubrock.navigation.BottomNavigationBar
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Rule
import org.junit.Test

class PrimaryNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun fivePrimaryDestinationsAreVisible() {
        composeRule.setContent {
            GitHubRockTheme(dynamicColor = false) {
                BottomNavigationBar(rememberNavController())
            }
        }

        listOf("Home", "Repositories", "Builds", "Downloads", "Profile").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }
}
