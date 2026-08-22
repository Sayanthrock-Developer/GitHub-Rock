package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.ui.screens.RepositoryAppPackageState
import com.sayanthrock.githubrock.ui.screens.findRepositoryDownloadedApk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryAppPackageStateTest {
    private val release = Release(
        id = 10,
        tagName = "v2.0.0",
        assets = listOf(
            ReleaseAsset(
                id = 11,
                name = "echo-music-universal.apk",
                size = 42_000_000,
                downloadUrl = "https://github.com/example/echo/releases/download/v2.0.0/echo-music-universal.apk"
            ),
            ReleaseAsset(
                id = 12,
                name = "echo-music-windows.zip",
                size = 50_000_000,
                downloadUrl = "https://github.com/example/echo/releases/download/v2.0.0/echo-music-windows.zip"
            )
        )
    )

    @Test fun newestCompletedMatchingApkIsSelected() {
        val older = DownloadEntity(id = 1, fileName = "echo-music-universal.apk", sourceUrl = "https://example.com/old.apk", localPath = "/tmp/old.apk", status = "completed", createdAt = 100)
        val newer = DownloadEntity(id = 2, fileName = "Echo-Music-Universal.APK", sourceUrl = "https://example.com/new.apk", localPath = "/tmp/new.apk", status = "completed", createdAt = 200)
        val failed = DownloadEntity(id = 3, fileName = "echo-music-universal.apk", sourceUrl = "https://example.com/failed.apk", localPath = "/tmp/failed.apk", status = "failed", createdAt = 300)
        val unrelated = DownloadEntity(id = 4, fileName = "another-app.apk", sourceUrl = "https://example.com/another.apk", localPath = "/tmp/another.apk", status = "completed", createdAt = 400)

        assertEquals(newer, findRepositoryDownloadedApk(listOf(older, newer, failed, unrelated), listOf(release)))
    }

    @Test fun noStateIsCreatedWithoutAMatchingCompletedApk() {
        assertNull(findRepositoryDownloadedApk(
            listOf(DownloadEntity(id = 1, fileName = "echo-music-universal.apk", sourceUrl = "https://example.com/partial.apk", localPath = "/tmp/partial.apk", status = "downloading")),
            listOf(release)
        ))
    }

    @Test fun packageStatusLabelsReflectAndroidState() {
        val installed = RepositoryAppPackageState(
            appName = "Echo Music",
            packageName = "com.echo.music",
            versionName = "2.0.0",
            apkPath = "/tmp/echo.apk",
            installed = true,
            openable = true,
            installedVersionName = "1.0.0",
            updateAvailable = true,
            canRequestInstall = true
        )
        val downloaded = installed.copy(installed = false, openable = false, updateAvailable = false)

        assertEquals("Update available", installed.statusLabel)
        assertEquals("Install update", installed.actionLabel)
        assertEquals("Ready to install", downloaded.statusLabel)
        assertTrue(installed.updateAvailable)
        assertFalse(downloaded.updateAvailable)
    }
}
