package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.screens.AndroidCapabilityState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCapabilityContractTest {
    @Test fun manifestIncludesOnlyPermissionsUsedByImplementedCapabilities() {
        val manifest = androidManifestText()

        assertTrue("android.permission.INTERNET" in manifest)
        assertTrue("android.permission.ACCESS_NETWORK_STATE" in manifest)
        assertTrue("android.permission.POST_NOTIFICATIONS" in manifest)
        assertTrue("android.permission.WAKE_LOCK" in manifest)
        assertTrue("android.permission.FOREGROUND_SERVICE" in manifest)
        assertTrue("android.permission.FOREGROUND_SERVICE_DATA_SYNC" in manifest)
        assertTrue("android.permission.RECEIVE_BOOT_COMPLETED" in manifest)
        assertTrue("android.permission.REQUEST_INSTALL_PACKAGES" in manifest)
        assertTrue("android.permission.REQUEST_DELETE_PACKAGES" in manifest)
        assertTrue("com.termux.permission.RUN_COMMAND" in manifest)
    }

    @Test fun installedAppControlsUseTargetedLauncherVisibility() {
        val manifest = androidManifestText()

        assertTrue("android.intent.action.MAIN" in manifest)
        assertTrue("android.intent.category.LAUNCHER" in manifest)
        assertFalse("android.permission.QUERY_ALL_PACKAGES" in manifest)
    }

    @Test fun broadOrUnimplementedPrivilegesRemainAbsent() {
        val manifest = androidManifestText()

        assertFalse("android.permission.QUERY_ALL_PACKAGES" in manifest)
        assertFalse("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in manifest)
        assertFalse("moe.shizuku.manager.permission.API_V23" in manifest)
        assertFalse("com.rosan.dhizuku.permission.API" in manifest)
    }

    @Test fun capabilityReadinessCountsCoreDownloadSupportAndGrantedOptions() {
        assertEquals(
            5,
            AndroidCapabilityState(
                notificationsEnabled = true,
                apkInstallAllowed = true,
                batteryUnrestricted = true,
                termuxAvailable = true
            ).readyCount
        )
        assertEquals(
            1,
            AndroidCapabilityState(
                notificationsEnabled = false,
                apkInstallAllowed = false,
                batteryUnrestricted = false,
                termuxAvailable = false
            ).readyCount
        )
    }

    private fun androidManifestText(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("AndroidManifest.xml was not found from ${File(".").absolutePath}")
    }
}
