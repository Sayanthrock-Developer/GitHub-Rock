package com.sayanthrock.githubrock.core.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInformationProviderTest {

    @Test
    fun read_returnsValidAppInformation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appInfo = AppInformationProvider.read(context)

        assertNotNull(appInfo)

        // Assert some basic properties we expect to be present in the test context
        assertEquals(context.packageName, appInfo.applicationId)
        assertTrue(appInfo.appName.isNotBlank())
        assertTrue(appInfo.versionName.isNotBlank())
        assertTrue(appInfo.versionCode > 0)

        // Assert Build properties are reflected
        assertTrue(appInfo.device.isNotBlank())

        // Assert dates are parsed
        assertTrue(appInfo.firstInstalled != "Unknown")
        assertTrue(appInfo.lastUpdated != "Unknown")
    }
}
