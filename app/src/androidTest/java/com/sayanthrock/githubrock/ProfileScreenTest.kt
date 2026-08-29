package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.GitHubProfileSnapshot
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun connectedProfileUsesCompactGroupedMenuAndOpensNativeDashboard() {
        var openedSettings = false
        var openedFeatures = false
        var openedAccounts = false
        var openedDownloads = false
        var openedAbout = false
        var inspectedLogin: String? = null
        var openedGitHubUrl: String? = null
        val profile = GitHubUser(
            login = "SayanthRock",
            id = 202829406,
            name = "Sayanth Rock",
            bio = "Android developer and GitHub Rock creator",
            publicRepos = 49,
            followers = 10,
            following = 34
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = profile,
                    explorerState = ProfileExplorerState(snapshot = GitHubProfileSnapshot(profile)),
                    onInspectProfile = { inspectedLogin = it },
                    onOpenDownloads = { openedDownloads = true },
                    onOpenFeatures = { openedFeatures = true },
                    onOpenAccounts = { openedAccounts = true },
                    onOpenSettings = { openedSettings = true },
                    onOpenAppInfo = { openedAbout = true },
                    onOpenGitHubUrl = { openedGitHubUrl = it },
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Profile").assertIsDisplayed()
        compose.onNodeWithText("Sayanth Rock").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("SayanthRock", inspectedLogin)
            assertEquals(null, openedGitHubUrl)
        }

        compose.onNodeWithText("49").assertIsDisplayed()
        compose.onNodeWithText("10").assertIsDisplayed()
        compose.onNodeWithText("34").assertIsDisplayed()
        compose.onNodeWithText("Stars").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Favourites").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Recently viewed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("What's new").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Announcements").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("GitHub settings").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedSettings) }
        compose.onNodeWithText("GitHub features").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(openedFeatures) }
        compose.onNodeWithText("Downloads").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedDownloads) }
        compose.onNodeWithText("About").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedAbout) }
        compose.onNodeWithText("Accounts & organizations").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedAccounts) }
    }

    @Test
    fun profileMetricsUseNativeCallbacksWithoutFragileOverlay() {
        var repositoriesOpened = false
        var followersOpened = false
        var followingOpened = false
        val profile = GitHubUser(login = "SayanthRock", id = 1, name = "Sayanth Rock", publicRepos = 8, followers = 18, following = 9)

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = profile,
                    onOpenDownloads = {},
                    onOpenFeatures = {},
                    onOpenAccounts = {},
                    onOpenSettings = {},
                    onOpenGitHubUrl = {},
                    onLogout = {},
                    onOpenRepositories = { repositoriesOpened = true },
                    onOpenFollowers = { followersOpened = true },
                    onOpenFollowing = { followingOpened = true }
                )
            }
        }

        compose.onNodeWithText("8").performClick()
        compose.onNodeWithText("18").performClick()
        compose.onNodeWithText("9").performClick()
        compose.runOnIdle {
            assertTrue(repositoriesOpened)
            assertTrue(followersOpened)
            assertTrue(followingOpened)
        }
    }

    @Test
    fun updatesOpenInsideProfileAndOldLongDashboardIsNotDuplicated() {
        val profile = GitHubUser(login = "SayanthRock", id = 1, name = "Sayanth Rock", publicRepos = 8, followers = 18, following = 9)

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                ProfileScreen(
                    mode = AppMode.Connected,
                    profile = profile,
                    onOpenDownloads = {},
                    onOpenFeatures = {},
                    onOpenAccounts = {},
                    onOpenSettings = {},
                    onOpenGitHubUrl = {},
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Contribution activity").assertCountEquals(0)
        compose.onNodeWithText("Search repositories…").assertCountEquals(0)
        compose.onNodeWithText("What's new").performScrollTo().performClick()
        compose.onNodeWithText("New profile experience").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Icon-first downloads").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Library").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Updates").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("App").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Account").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Logout").performScrollTo().assertIsDisplayed()
    }
}
