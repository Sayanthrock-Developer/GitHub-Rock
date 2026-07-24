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

    @Test fun supportsThePublishedCrossPlatformPackageMatrix() {
        val cases = listOf(
            Case("GitHub-Rock-0.2.2.apk", ReleasePlatform.Android, "APK", "Architecture not specified"),
            Case("GitHub-Rock-0.2.2-macos-arm64.dmg", ReleasePlatform.MacOS, "DMG", "ARM64"),
            Case("GitHub-Rock-0.2.2-macos-x64.pkg", ReleasePlatform.MacOS, "PKG", "x64"),
            Case("GitHub-Rock-0.2.2-windows-x64.msi", ReleasePlatform.Windows, "MSI", "x64"),
            Case("GitHub-Rock-0.2.2-windows-x64-setup.exe", ReleasePlatform.Windows, "EXE", "x64"),
            Case(
                "GitHub-Rock-0.2.2-windows-x64-portable.zip",
                ReleasePlatform.Windows,
                "ZIP",
                "x64"
            ),
            Case("GitHub-Rock-0.2.2-linux-x64.AppImage", ReleasePlatform.Linux, "AppImage", "x64"),
            Case("GitHub-Rock-0.2.2-linux-x64.deb", ReleasePlatform.Linux, "DEB", "x64"),
            Case("GitHub-Rock-0.2.2-linux-x64.rpm", ReleasePlatform.Linux, "RPM", "x64"),
            Case(
                "GitHub-Rock-0.2.2-linux-x64.pkg.tar.zst",
                ReleasePlatform.Linux,
                "Arch package",
                "x64"
            ),
            Case("GitHub-Rock-0.2.2-ios-arm64.ipa", ReleasePlatform.IOS, "IPA", "ARM64")
        )

        cases.forEach { case ->
            val info = ReleaseAssetClassifier.classify(case.name)
            assertEquals(case.name, case.platform, info.platform)
            assertEquals(case.name, case.format, info.format)
            assertEquals(case.name, case.architecture, info.architecture)
            assertTrue(case.name, info.isInstallablePackage)
            assertFalse(case.name, info.isSupportFile)
        }
    }

    @Test fun supportsThePackageNamesShownInTheReferenceRelease() {
        val expected = mapOf(
            "komi-store-1.9.2-1-x86_64.pkg.tar.zst" to ReleasePlatform.Linux,
            "Komi-Store-1.9.2-arm64.dmg" to ReleasePlatform.MacOS,
            "Komi-Store-1.9.2-arm64.pkg" to ReleasePlatform.MacOS,
            "Komi-Store-1.9.2-windows-portable.zip" to ReleasePlatform.Windows,
            "Komi-Store-1.9.2-x64.dmg" to ReleasePlatform.MacOS,
            "Komi-Store-1.9.2-x64.pkg" to ReleasePlatform.MacOS,
            "Komi-Store-1.9.2.apk" to ReleasePlatform.Android,
            "Komi-Store-1.9.2.exe" to ReleasePlatform.Windows,
            "Komi-Store-1.9.2.msi" to ReleasePlatform.Windows,
            "Komi-Store-x86_64.AppImage" to ReleasePlatform.Linux
        )

        expected.forEach { (name, platform) ->
            assertEquals(name, platform, ReleaseAssetClassifier.classify(name).platform)
        }
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
