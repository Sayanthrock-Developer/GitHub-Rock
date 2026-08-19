package com.sayanthrock.githubrock.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledApkStateTest {
    @Test
    fun updateIsAvailableOnlyWhenDownloadedVersionIsNewer() {
        val state = InstalledApkState(
            packageName = "com.example.app",
            installed = true,
            installedVersionCode = 10,
            installedVersionName = "1.0",
            label = "Example",
            icon = null,
            launchIntent = null,
            downloadedVersionCode = 11,
            downloadedVersionName = "1.1"
        )

        assertTrue(state.isUpdateAvailable)
        assertFalse(state.isSameOrOlderVersion)
    }

    @Test
    fun installedStateIsNotAnUpdateWhenVersionsMatch() {
        val state = InstalledApkState(
            packageName = "com.example.app",
            installed = true,
            installedVersionCode = 10,
            installedVersionName = "1.0",
            label = "Example",
            icon = null,
            launchIntent = null,
            downloadedVersionCode = 10,
            downloadedVersionName = "1.0"
        )

        assertFalse(state.isUpdateAvailable)
        assertTrue(state.isSameOrOlderVersion)
    }

    @Test
    fun notInstalledCannotBeReportedAsUpdate() {
        val state = InstalledApkState(
            packageName = "com.example.app",
            installed = false,
            installedVersionCode = null,
            installedVersionName = null,
            label = "Example",
            icon = null,
            launchIntent = null,
            downloadedVersionCode = 10,
            downloadedVersionName = "1.0"
        )

        assertFalse(state.isUpdateAvailable)
        assertFalse(state.isSameOrOlderVersion)
    }
}
