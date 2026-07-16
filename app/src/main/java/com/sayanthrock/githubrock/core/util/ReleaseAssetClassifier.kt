package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ReleaseAsset

enum class ReleasePlatform(val label: String) {
    Android("Android"),
    Windows("Windows"),
    Linux("Linux"),
    IOS("iOS"),
    MacOS("macOS"),
    Other("Other")
}

data class ReleaseAssetInfo(
    val platform: ReleasePlatform,
    val format: String,
    val architecture: String,
    val isInstallablePackage: Boolean,
    val isSupportFile: Boolean
)

object ReleaseAssetClassifier {
    private val supportSuffixes = listOf(
        ".sha256",
        ".sha512",
        ".checksum",
        ".checksums",
        ".sig",
        ".asc"
    )

    fun classify(name: String): ReleaseAssetInfo {
        val normalized = name.trim().lowercase()
        val supportSuffix = supportSuffixes.firstOrNull(normalized::endsWith)
        val packageName = supportSuffix?.let { normalized.removeSuffix(it) } ?: normalized
        val platform = when {
            isAndroid(packageName) -> ReleasePlatform.Android
            isWindows(packageName) -> ReleasePlatform.Windows
            isLinux(packageName) -> ReleasePlatform.Linux
            isIOS(packageName) -> ReleasePlatform.IOS
            isMacOS(packageName) -> ReleasePlatform.MacOS
            else -> ReleasePlatform.Other
        }
        val format = assetFormat(normalized)
        return ReleaseAssetInfo(
            platform = platform,
            format = format,
            architecture = assetArchitecture(normalized),
            isInstallablePackage = when (format) {
                "APK", "IPA", "DMG", "PKG", "MSI", "MSIX", "MSIX bundle", "EXE",
                "APPX", "APPX bundle", "AppImage", "DEB", "RPM", "Flatpak", "Snap" -> true
                else -> false
            },
            isSupportFile = supportSuffix != null
        )
    }

    fun assetsForPlatform(
        assets: List<ReleaseAsset>,
        platform: ReleasePlatform
    ): List<ReleaseAsset> = assets.filter { classify(it.name).platform == platform }

    fun preferredPlatform(assets: List<ReleaseAsset>): ReleasePlatform =
        ReleasePlatform.entries.firstOrNull { platform ->
            assets.any { classify(it.name).platform == platform }
        } ?: ReleasePlatform.Android

    fun preferredAsset(
        assets: List<ReleaseAsset>,
        platform: ReleasePlatform
    ): ReleaseAsset? = assetsForPlatform(assets, platform)
        .minByOrNull { assetPreferenceScore(it.name, platform) }

    private fun isAndroid(name: String): Boolean =
        name.endsWith(".apk") ||
            name.endsWith(".aab") ||
            name.endsWith(".apks") ||
            hasToken(name, "android")

    private fun isWindows(name: String): Boolean =
        listOf(".exe", ".msi", ".msix", ".msixbundle", ".appx", ".appxbundle")
            .any(name::endsWith) ||
            listOf("windows", "win", "win32", "win64").any { hasToken(name, it) }

    private fun isLinux(name: String): Boolean =
        listOf(".appimage", ".deb", ".rpm", ".snap", ".flatpak")
            .any(name::endsWith) ||
            listOf("linux", "ubuntu", "debian", "fedora").any { hasToken(name, it) }

    private fun isIOS(name: String): Boolean =
        name.endsWith(".ipa") ||
            name.endsWith(".xcarchive") ||
            listOf("ios", "iphone", "ipad").any { hasToken(name, it) }

    private fun isMacOS(name: String): Boolean =
        name.endsWith(".dmg") ||
            name.endsWith(".pkg") ||
            name.endsWith(".app.zip") ||
            listOf("macos", "macosx", "darwin", "osx").any { hasToken(name, it) }

    private fun hasToken(name: String, token: String): Boolean =
        Regex("(^|[._\\- ])${Regex.escape(token)}([._\\- ]|$)").containsMatchIn(name)

    private fun assetFormat(name: String): String = when {
        name.endsWith(".appimage") -> "AppImage"
        name.endsWith(".appxbundle") -> "APPX bundle"
        name.endsWith(".msixbundle") -> "MSIX bundle"
        name.endsWith(".xcarchive") -> "XCArchive"
        name.endsWith(".flatpak") -> "Flatpak"
        name.endsWith(".tar.gz") -> "TAR.GZ"
        name.endsWith(".tar.xz") -> "TAR.XZ"
        name.endsWith(".sha256") -> "SHA-256"
        name.endsWith(".sha512") -> "SHA-512"
        name.endsWith(".checksum") || name.endsWith(".checksums") -> "Checksum"
        name.endsWith(".apk") -> "APK"
        name.endsWith(".aab") -> "AAB"
        name.endsWith(".apks") -> "APKS"
        name.endsWith(".ipa") -> "IPA"
        name.endsWith(".dmg") -> "DMG"
        name.endsWith(".pkg") -> "PKG"
        name.endsWith(".msi") -> "MSI"
        name.endsWith(".msix") -> "MSIX"
        name.endsWith(".appx") -> "APPX"
        name.endsWith(".exe") -> "EXE"
        name.endsWith(".deb") -> "DEB"
        name.endsWith(".rpm") -> "RPM"
        name.endsWith(".snap") -> "Snap"
        name.endsWith(".zip") -> "ZIP"
        name.endsWith(".7z") -> "7Z"
        name.endsWith(".gz") -> "GZIP"
        name.endsWith(".sig") -> "Signature"
        name.endsWith(".asc") -> "Signature"
        else -> "File"
    }

    private fun assetArchitecture(name: String): String = when {
        listOf("arm64", "aarch64", "arm64-v8a").any { name.contains(it) } -> "ARM64"
        listOf("armeabi", "armv7", "arm32").any { name.contains(it) } -> "ARMv7"
        listOf("x86_64", "amd64", "x64").any { name.contains(it) } -> "x64"
        listOf("i386", "i686", "x86").any { name.contains(it) } -> "x86"
        listOf("universal", "all-platforms", "noarch").any { name.contains(it) } -> "Universal"
        else -> "Architecture not specified"
    }

    private fun assetPreferenceScore(name: String, platform: ReleasePlatform): Int {
        val info = classify(name)
        val normalized = name.lowercase()
        if (info.isSupportFile) return 1_000
        return when (platform) {
            ReleasePlatform.Android -> when {
                info.format == "APK" && info.architecture == "ARM64" -> 0
                info.format == "APK" && info.architecture == "Universal" -> 1
                info.format == "APK" -> 2
                info.format == "AAB" -> 3
                else -> 20
            }
            ReleasePlatform.Windows -> when (info.format) {
                "MSIX", "MSIX bundle", "MSI" -> 0
                "EXE" -> 1
                "APPX", "APPX bundle" -> 2
                "ZIP" -> 3
                else -> 20
            }
            ReleasePlatform.Linux -> when (info.format) {
                "AppImage" -> 0
                "DEB" -> 1
                "RPM" -> 2
                "Flatpak", "Snap" -> 3
                "TAR.GZ", "TAR.XZ" -> 4
                else -> 20
            }
            ReleasePlatform.IOS -> if (info.format == "IPA") 0 else 20
            ReleasePlatform.MacOS -> when (info.format) {
                "DMG" -> 0
                "PKG" -> 1
                "ZIP" -> 2
                else -> 20
            }
            ReleasePlatform.Other -> if (normalized.endsWith(".zip")) 0 else 10
        }
    }
}
