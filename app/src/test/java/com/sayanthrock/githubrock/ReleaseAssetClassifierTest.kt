package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.ReleaseAssetClassifier
import com.sayanthrock.githubrock.core.util.ReleasePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAssetClassifierTest {
    @Test fun classifiesRequestedPlatformsFormatsAndArchitectures() {
        val cases = listOf(
            Case("github-rock-android-arm64-v8a.apk", ReleasePlatform.Android, "APK", "ARM64"),
            Case("github-rock-windows-x64.msi", ReleasePlatform.Windows, "MSI", "x64"),
            Case("github-rock-linux-x86_64.AppImage", ReleasePlatform.Linux, "AppImage", "x64"),
            Case("github-rock-ios-arm64.ipa", ReleasePlatform.IOS, "IPA", "ARM64"),
            Case("github-rock-macos-universal.dmg", ReleasePlatform.MacOS, "DMG", "Universal")
        )

        cases.forEach { case ->
            val info = ReleaseAssetClassifier.classify(case.name)
            assertEquals(case.platform, info.platform)
            assertEquals(case.format, info.format)
            assertEquals(case.architecture, info.architecture)
            assertTrue(info.isInstallablePackage)
            assertFalse(info.isSupportFile)
        }
    }

    @Test fun detectsCommonPlatformArchiveNamesWithoutConfusingDarwinAndWin() {
        assertEquals(
            ReleasePlatform.Windows,
            ReleaseAssetClassifier.classify("github-rock-win-x64.zip").platform
        )
        assertEquals(
            ReleasePlatform.MacOS,
            ReleaseAssetClassifier.classify("github-rock-darwin-arm64.zip").platform
        )
        assertEquals(
            ReleasePlatform.Linux,
            ReleaseAssetClassifier.classify("github-rock-linux-arm64.tar.gz").platform
        )
    }

    @Test fun explicitLinuxPackagingWinsOverTheAmbiguousApkExtension() {
        val alpinePackage = ReleaseAssetClassifier.classify("github-rock-alpine-x64.apk")
        val androidPackage = ReleaseAssetClassifier.classify("github-rock-android-x64.apk")

        assertEquals(ReleasePlatform.Linux, alpinePackage.platform)
        assertEquals("Alpine APK", alpinePackage.format)
        assertTrue(alpinePackage.isInstallablePackage)
        assertEquals(ReleasePlatform.Android, androidPackage.platform)
        assertEquals("APK", androidPackage.format)
    }

    @Test fun identifiesSupportFilesAndDoesNotCallThemInstallers() {
        val info = ReleaseAssetClassifier.classify("github-rock-v1.4.0.checksums")
        val androidChecksum = ReleaseAssetClassifier.classify("github-rock-arm64-v8a.apk.sha256")
        val macUpdateMetadata = ReleaseAssetClassifier.classify("github-rock-macos.dmg.blockmap")

        assertEquals(ReleasePlatform.Other, info.platform)
        assertEquals("Checksum", info.format)
        assertTrue(info.isSupportFile)
        assertFalse(info.isInstallablePackage)
        assertEquals(ReleasePlatform.Android, androidChecksum.platform)
        assertEquals("SHA-256", androidChecksum.format)
        assertTrue(androidChecksum.isSupportFile)
        assertEquals(ReleasePlatform.MacOS, macUpdateMetadata.platform)
        assertEquals("Update metadata", macUpdateMetadata.format)
        assertTrue(macUpdateMetadata.isSupportFile)
        assertFalse(macUpdateMetadata.isInstallablePackage)
    }

    @Test fun choosesTheMostUsefulFileForEachPlatform() {
        val assets = listOf(
            asset(1, "github-rock-universal.apk"),
            asset(2, "github-rock-arm64-v8a.apk"),
            asset(3, "github-rock-windows-x64.exe"),
            asset(4, "github-rock-windows-x64.msi")
        )

        assertEquals(
            "github-rock-arm64-v8a.apk",
            ReleaseAssetClassifier.preferredAsset(assets, ReleasePlatform.Android)?.name
        )
        assertEquals(
            "github-rock-windows-x64.msi",
            ReleaseAssetClassifier.preferredAsset(assets, ReleasePlatform.Windows)?.name
        )
        assertEquals(ReleasePlatform.Android, ReleaseAssetClassifier.preferredPlatform(assets))
        assertEquals(ReleasePlatform.Android, ReleaseAssetClassifier.preferredPlatform(emptyList()))
    }

    @Test fun presentsPlatformsInTheRequestedDownloadOrder() {
        assertEquals(
            listOf(
                ReleasePlatform.Android,
                ReleasePlatform.MacOS,
                ReleasePlatform.Windows,
                ReleasePlatform.Linux,
                ReleasePlatform.IOS,
                ReleasePlatform.Other
            ),
            ReleasePlatform.entries
        )
    }

    private fun asset(id: Long, name: String) = ReleaseAsset(
        id = id,
        name = name,
        size = 1_024,
        downloadUrl = "https://example.com/$name"
    )

    private data class Case(
        val name: String,
        val platform: ReleasePlatform,
        val format: String,
        val architecture: String
    )
}
