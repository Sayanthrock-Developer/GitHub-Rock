package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.sayanthrock.githubrock.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AppInformationProviderTest {

    @Test
    fun `read should return correct AppInformation`() {
        val testApplicationInfo = ApplicationInfo().apply {
            minSdkVersion = 21
            targetSdkVersion = 33
        }

        val testPackageInfo = PackageInfo().apply {
            versionName = "1.0.0"
            firstInstallTime = 1672531200000L // 2023-01-01
            lastUpdateTime = 1672531200000L // 2023-01-01
            requestedPermissions = arrayOf("android.permission.INTERNET")
            applicationInfo = testApplicationInfo

            @Suppress("DEPRECATION")
            versionCode = 1
        }

        val mockPackageManager = mock<PackageManager> {
            on { getPackageInfo(any<String>(), any<PackageManager.PackageInfoFlags>()) } doReturn testPackageInfo
            on { getPackageInfo(any<String>(), any<Int>()) } doReturn testPackageInfo
        }

        val mockContext = mock<Context> {
            on { packageManager } doReturn mockPackageManager
            on { packageName } doReturn "com.example.app"
            on { getString(R.string.app_name) } doReturn "Example App"
            on { applicationInfo } doReturn testApplicationInfo
        }

        val appInfo = AppInformationProvider.read(mockContext)

        assertEquals("Example App", appInfo.appName)
        assertEquals("1.0.0", appInfo.versionName)
        assertEquals("com.example.app", appInfo.applicationId)
        assertEquals(21, appInfo.minimumSdk)
        assertEquals(33, appInfo.targetSdk)
        assertEquals(1, appInfo.requestedPermissions)
        assertEquals(1L, appInfo.versionCode)

        // When running as unit tests, the Build class from android.jar will return nulls
        // which the provider properly maps to "Unknown Android device" due to our changes.
        assertEquals("Unknown Android device", appInfo.device)
        assertEquals(emptyList<String>(), appInfo.supportedAbis)

        assertEquals(true, appInfo.firstInstalled.contains("2023"))
        assertEquals(true, appInfo.lastUpdated.contains("2023"))
    }
}
