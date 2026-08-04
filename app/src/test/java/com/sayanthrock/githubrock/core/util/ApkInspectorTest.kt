package com.sayanthrock.githubrock.core.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import android.os.Build

class ApkInspectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockPackageManager: FakePackageManagerWrapper
    private lateinit var dummyApk: File

    @Before
    fun setup() {
        mockPackageManager = FakePackageManagerWrapper()
        dummyApk = tempFolder.newFile("dummy.apk")
        dummyApk.writeText("dummy content for sha256")
    }

    @Test
    fun `inspect returns null when package archive info is null`() {
        val result = ApkInspector.inspect(dummyApk, mockPackageManager)
        assertNull(result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `inspect extracts package details successfully`() {
        val archiveInfo = PackageInfo().apply {
            packageName = "com.example.app"
            versionName = "1.0.0"
            versionCode = 123
            requestedPermissions = arrayOf("android.permission.INTERNET")
            applicationInfo = ApplicationInfo().apply {
                minSdkVersion = 21
                targetSdkVersion = 33
            }
        }

        val signatureBytes = "dummy_signature".toByteArray()
        archiveInfo.signatures = arrayOf(FakeSignature(signatureBytes))
        mockPackageManager.setArchiveInfo(archiveInfo)

        val installedInfo = PackageInfo().apply {
            packageName = "com.example.app"
            signatures = arrayOf(FakeSignature(signatureBytes))
        }
        mockPackageManager.setInstalledInfo("com.example.app", installedInfo)
        mockPackageManager.setAppLabel("Example App")

        val result = ApkInspector.inspect(dummyApk, mockPackageManager)

        assertNotNull(result)
        assertEquals("Example App", result!!.appName)
        assertEquals("com.example.app", result.packageName)
        assertEquals("1.0.0", result.versionName)
        assertEquals(123L, result.versionCode)
        assertEquals(21, result.minSdk)
        assertEquals(33, result.targetSdk)
        assertEquals(listOf("android.permission.INTERNET"), result.permissions)

        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest(signatureBytes)
            .joinToString(":") { "%02X".format(it) }

        assertEquals(expectedHash, result.signingSha256)
        assertTrue(result.installedSignatureMatches == true)
        assertEquals(dummyApk.length(), result.fileSize)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `inspect works correctly without signing info`() {
        val archiveInfo = PackageInfo().apply {
            packageName = "com.example.app"
            versionName = "1.0.0"
            versionCode = 123
        }
        mockPackageManager.setArchiveInfo(archiveInfo)

        val result = ApkInspector.inspect(dummyApk, mockPackageManager)

        assertNotNull(result)
        assertEquals("com.example.app", result!!.appName)
        assertEquals("com.example.app", result.packageName)
        assertEquals("1.0.0", result.versionName)
        assertEquals(123L, result.versionCode)
        assertEquals(0, result.minSdk)
        assertEquals(0, result.targetSdk)
        assertEquals(emptyList<String>(), result.permissions)
        assertNull(result.signingSha256)
        assertNull(result.installedSignatureMatches)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `inspect installed signature returns false if mismatch`() {
        val archiveInfo = PackageInfo().apply {
            packageName = "com.example.app"
        }

        val signatureBytes1 = "dummy_signature_1".toByteArray()
        archiveInfo.signatures = arrayOf(FakeSignature(signatureBytes1))
        mockPackageManager.setArchiveInfo(archiveInfo)

        val installedInfo = PackageInfo().apply {
            packageName = "com.example.app"
        }
        val signatureBytes2 = "dummy_signature_2".toByteArray()
        installedInfo.signatures = arrayOf(FakeSignature(signatureBytes2))

        mockPackageManager.setInstalledInfo("com.example.app", installedInfo)

        val result = ApkInspector.inspect(dummyApk, mockPackageManager)

        assertNotNull(result)
        assertEquals(false, result!!.installedSignatureMatches)
    }
}

class FakeSignature(private val bytes: ByteArray) : Signature("") {
    override fun toByteArray(): ByteArray = bytes
}

class FakePackageManagerWrapper : PackageManagerWrapper {
    private var archiveInfo: PackageInfo? = null
    private var installedInfo: MutableMap<String, PackageInfo> = mutableMapOf()
    private var appLabel: String? = null

    fun setArchiveInfo(info: PackageInfo?) {
        archiveInfo = info
    }

    fun setInstalledInfo(packageName: String, info: PackageInfo) {
        installedInfo[packageName] = info
    }

    fun setAppLabel(label: String) {
        appLabel = label
    }

    override fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo? {
        return archiveInfo
    }

    override fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
        if (!installedInfo.containsKey(packageName)) {
            throw Exception("Package $packageName not found")
        }
        return installedInfo[packageName]!!
    }

    override fun loadLabel(applicationInfo: ApplicationInfo): String? {
        return appLabel
    }
}
