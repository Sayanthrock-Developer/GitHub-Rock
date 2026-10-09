package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.NativeProfileDestination
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.ui.screens.ProfilePersonCard
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NativeProfileNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun followerCardNavigatesToThatUsersNativeProfileRoute() {
        val person = GitHubUser(login = "octocat", id = 583231, name = "The Octocat")
        var currentLogin: String? = null
        var currentSection: String? = null

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "followers") {
                    composable("followers") {
                        ProfilePersonCard(person) {
                            navController.navigate(
                                NativeProfileDestination(person.login, NativeProfileSection.Repositories).route
                            )
                        }
                    }
                    composable(
                        route = "native-profile/{login}/{section}",
                        arguments = listOf(
                            navArgument("login") { type = NavType.StringType },
                            navArgument("section") { type = NavType.StringType }
                        )
                    ) { entry ->
                        currentLogin = entry.arguments?.getString("login")
                        currentSection = entry.arguments?.getString("section")
                        androidx.compose.material3.Text("Native profile: " + currentLogin.orEmpty())
                    }
                }
            }
        }

        compose.onNodeWithText("@octocat").assertIsDisplayed().performClick()
        compose.onNodeWithText("Native profile: octocat").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("octocat", currentLogin)
            assertEquals("repositories", currentSection)
        }
    }
}
