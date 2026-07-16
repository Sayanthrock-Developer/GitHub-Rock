package com.sayanthrock.githubrock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.ui.screens.RepositoryHubContent
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RepositoryHubScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repositorySelectsAPlatformSpecificReleaseAsset() {
        var downloadedAsset: ReleaseAsset? = null
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = Owner(login = "SayanthRock"),
            description = "Premium GitHub developer control centre",
            htmlUrl = "https://github.com/SayanthRock/GitHub-Rock",
            language = "Kotlin",
            stars = 386,
            forks = 42,
            openIssues = 8,
            topics = listOf("android", "jetpack-compose")
        )
        val androidAsset = ReleaseAsset(
            id = 10,
            name = "github-rock-arm64-v8a.apk",
            size = 25_900_000,
            downloadUrl = "https://github.com/SayanthRock/GitHub-Rock/releases/download/v1.4.0/app.apk"
        )
        val windowsAsset = ReleaseAsset(
            id = 11,
            name = "github-rock-windows-x64.msi",
            size = 46_800_000,
            downloadUrl = "https://github.com/SayanthRock/GitHub-Rock/releases/download/v1.4.0/app.msi"
        )
        val linuxAsset = ReleaseAsset(
            id = 12,
            name = "github-rock-linux-x86_64.AppImage",
            size = 52_300_000,
            downloadUrl = "https://github.com/SayanthRock/GitHub-Rock/releases/download/v1.4.0/app.AppImage"
        )
        val iosAsset = ReleaseAsset(
            id = 13,
            name = "github-rock-ios-arm64.ipa",
            size = 39_700_000,
            downloadUrl = "https://github.com/SayanthRock/GitHub-Rock/releases/download/v1.4.0/app.ipa"
        )
        val macosAsset = ReleaseAsset(
            id = 14,
            name = "github-rock-macos-universal.dmg",
            size = 55_100_000,
            downloadUrl = "https://github.com/SayanthRock/GitHub-Rock/releases/download/v1.4.0/app.dmg"
        )
        val release = Release(
            id = 2,
            tagName = "v1.4.0",
            name = "GitHub Rock 1.4",
            body = "## What changed\n\n- Unified repository page",
            publishedAt = "2026-07-14T00:00:00Z",
            assets = listOf(androidAsset, windowsAsset, linuxAsset, iosAsset, macosAsset)
        )

        compose.setContent {
            GitHubRockTheme(dynamicColor = false) {
                RepositoryHubContent(
                    repository = repository,
                    releases = listOf(release),
                    readme = "# GitHub Rock\n\nRepository documentation.",
                    loading = false,
                    releasesLoading = false,
                    readmeLoading = false,
                    error = null,
                    releasesError = null,
                    readmeError = null,
                    onRetry = {},
                    onOpenUrl = {},
                    onDownload = { downloadedAsset = it }
                )
            }
        }

        compose.onNodeWithContentDescription("GitHub-Rock application icon").assertIsDisplayed()
        compose.onNodeWithText("Repository tools").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Get the app").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Select Windows downloads")
            .performScrollTo()
            .performClick()
        compose.onNodeWithContentDescription("Download selected release asset")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(windowsAsset, downloadedAsset) }
        compose.onNodeWithText("Standard download protection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Stats").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("What’s New").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("README.md").performScrollTo().assertIsDisplayed()
    }
}
