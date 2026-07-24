package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ReleaseAsset

enum class ReleasePlatform(val label: String) {
    Android("Android"),
    MacOS("macOS"),
    Windows("Windows"),
    Linux("Linux"),
    IOS("iOS"),
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
        ".sha256sums",
        ".sha512sums",
        ".sha256sum",
        ".sha512sum",
        ".sha256.txt",
        ".sha512.txt",
        ".sha256",
        ".sha512",
        ".checksum",
        ".checksums",
        ".blockmap",
        ".minisig",
        ".sig",
        ".asc"
    )

    fun classify(name: String): ReleaseAssetInfo {
        val normalized = name.trim().lowercase()
        val supportSuffix = supportSuffixes.firstOrNull(normalized::endsWith)
        val packageName = supportSuffix?.let { normalized.removeSuffix(it) } ?: normalized
        val platform = explicitPlatform(packageName) ?: when {
            isAndroidFile(packageName) -> ReleasePlatform.Android
            isMacOSFile(packageName) -> ReleasePlatform.MacOS
            isWindowsFile(packageName) -> ReleasePlatform.Windows
            isLinuxFile(packageName) -> ReleasePlatform.Linux
            isIOSFile(packageName) -> ReleasePlatform.IOS
            else -> ReleasePlatform.Other
        }
        val format = assetFormat(normalized, platform)
        return ReleaseAssetInfo(
            platform = platform,
            format = format,
            architecture = assetArchitecture(normalized),
            isInstallablePackage = when (format) {
                "APK", "IPA", "DMG", "PKG", "MSI", "MSIX", "MSIX bundle", "EXE",
                "APPX", "APPX bundle", "AppImage", "DEB", "RPM", "Flatpak", "Snap",
                "Alpine APK", "Arch package" -> true
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

    private fun explicitPlatform(name: String): ReleasePlatform? = when {
        listOf("android").any { hasToken(name, it) } -> ReleasePlatform.Android
        listOf("macos", "macosx", "darwin", "osx").any { hasToken(name, it) } ->
            ReleasePlatform.MacOS
        listOf("windows", "win", "win32", "win64").any { hasToken(name, it) } ->
            ReleasePlatform.Windows
        listOf("linux", "ubuntu", "debian", "fedora", "alpine", "archlinux")
            .any { hasToken(name, it) } -> ReleasePlatform.Linux
        listOf("ios", "iphone", "ipad").any { hasToken(name, it) } -> ReleasePlatform.IOS
        else -> null
    }

    private fun isAndroidFile(name: String): Boolean =
        name.endsWith(".apk") ||
            name.endsWith(".aab") ||
            name.endsWith(".apks") ||
            name.endsWith(".apkm") ||
            name.endsWith(".xapk")

    private fun isMacOSFile(name: String): Boolean =
        listOf(".dmg", ".pkg", ".app.zip", ".app.tar.gz", ".app.tar.xz")
            .any(name::endsWith)

    private fun isWindowsFile(name: String): Boolean =
        listOf(".exe", ".msi", ".msix", ".msixbundle", ".appx", ".appxbundle", ".nupkg")
            .any(name::endsWith)

    private fun isLinuxFile(name: String): Boolean =
        listOf(".appimage", ".deb", ".rpm", ".snap", ".flatpak", ".pkg.tar.zst")
            .any(name::endsWith)

    private fun isIOSFile(name: String): Boolean =
        name.endsWith(".ipa") ||
            name.endsWith(".xcarchive") ||
            name.endsWith(".xcarchive.zip")

    private fun hasToken(name: String, token: String): Boolean =
        Regex("(^|[._\\- ])${Regex.escape(token)}([._\\- ]|$)").containsMatchIn(name)

    private fun assetFormat(name: String, platform: ReleasePlatform): String = when {
        name.endsWith(".appimage") -> "AppImage"
        name.endsWith(".appxbundle") -> "APPX bundle"
        name.endsWith(".msixbundle") -> "MSIX bundle"
        name.endsWith(".xcarchive.zip") -> "XCArchive ZIP"
        name.endsWith(".xcarchive") -> "XCArchive"
        name.endsWith(".pkg.tar.zst") -> "Arch package"
        name.endsWith(".flatpak") -> "Flatpak"
        name.endsWith(".tar.zst") -> "TAR.ZST"
        name.endsWith(".tar.gz") -> "TAR.GZ"
        name.endsWith(".tar.xz") -> "TAR.XZ"
        name.endsWith(".sha256") ||
            name.endsWith(".sha256sum") ||
            name.endsWith(".sha256sums") ||
            name.endsWith(".sha256.txt") -> "SHA-256"
        name.endsWith(".sha512") ||
            name.endsWith(".sha512sum") ||
            name.endsWith(".sha512sums") ||
            name.endsWith(".sha512.txt") -> "SHA-512"
        name.endsWith(".checksum") || name.endsWith(".checksums") -> "Checksum"
        name.endsWith(".blockmap") -> "Update metadata"
        name.endsWith(".minisig") -> "Signature"
        name.endsWith(".apk") && platform == ReleasePlatform.Linux -> "Alpine APK"
        name.endsWith(".apk") -> "APK"
        name.endsWith(".aab") -> "AAB"
        name.endsWith(".apks") -> "APKS"
        name.endsWith(".apkm") -> "APKM"
        name.endsWith(".xapk") -> "XAPK"
        name.endsWith(".ipa") -> "IPA"
        name.endsWith(".dmg") -> "DMG"
        name.endsWith(".pkg") -> "PKG"
        name.endsWith(".msi") -> "MSI"
        name.endsWith(".msix") -> "MSIX"
        name.endsWith(".appx") -> "APPX"
        name.endsWith(".exe") -> "EXE"
        name.endsWith(".nupkg") -> "NuGet package"
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
        listOf("riscv64", "risc-v64").any { name.contains(it) } -> "RISC-V 64"
        listOf("x86_64", "amd64", "x64").any { name.contains(it) } -> "x64"
        listOf("i386", "i686", "x86").any { name.contains(it) } -> "x86"
        listOf("universal", "universal2", "all-platforms", "noarch", "fat")
            .any { name.contains(it) } -> "Universal"
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
                info.format in setOf("APKS", "APKM", "XAPK") -> 3
                info.format == "AAB" -> 4
                else -> 20
            }
            ReleasePlatform.MacOS -> when (info.format) {
                "DMG" -> 0
                "PKG" -> 1
                "ZIP", "TAR.GZ", "TAR.XZ" -> 2
                else -> 20
            }
            ReleasePlatform.Windows -> when (info.format) {
                "MSIX", "MSIX bundle", "MSI" -> 0
                "EXE" -> 1
                "APPX", "APPX bundle" -> 2
                "ZIP" -> 3
                "NuGet package" -> 4
                else -> 20
            }
            ReleasePlatform.Linux -> when (info.format) {
                "AppImage" -> 0
                "DEB" -> 1
                "RPM" -> 2
                "Flatpak", "Snap", "Alpine APK", "Arch package" -> 3
                "TAR.GZ", "TAR.XZ", "TAR.ZST" -> 4
                else -> 20
            }
            ReleasePlatform.IOS -> if (info.format == "IPA") 0 else 20
            ReleasePlatform.Other -> if (normalized.endsWith(".zip")) 0 else 10
        }
    }
}
