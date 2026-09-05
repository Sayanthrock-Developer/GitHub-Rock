package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/** Immutable package state used by the Downloads/Library UI. */
data class InstalledApkState(
    val packageName: String,
    val installed: Boolean,
    val installedVersionCode: Long?,
    val installedVersionName: String?,
    val label: String?,
    val icon: Drawable?,
    val launchIntent: Intent?,
    val downloadedVersionCode: Long?,
    val downloadedVersionName: String?
) {
    val isUpdateAvailable: Boolean
        get() = installed && downloadedVersionCode != null && installedVersionCode != null && downloadedVersionCode > installedVersionCode

    val isSameOrOlderVersion: Boolean
        get() = installed && downloadedVersionCode != null && installedVersionCode != null && downloadedVersionCode <= installedVersionCode
}

/** PackageManager-backed resolver. All calls should be made off the main thread. */
object InstalledApkStateResolver {
    @Suppress("DEPRECATION")
    fun resolve(context: Context, apkFile: File): InstalledApkState? {
        if (!apkFile.isFile || apkFile.length() <= 0L || !apkFile.extension.equals("apk", ignoreCase = true)) return null
        val pm = context.packageManager
        val archive = resolveArchive(pm, apkFile)

        if (archive == null) {
            return InstalledApkState(
                packageName = apkFile.nameWithoutExtension,
                installed = false,
                installedVersionCode = null,
                installedVersionName = null,
                label = apkFile.nameWithoutExtension,
                icon = null,
                launchIntent = null,
                downloadedVersionCode = null,
                downloadedVersionName = null
            )
        }

        archive.applicationInfo?.let {
            it.sourceDir = apkFile.absolutePath
            it.publicSourceDir = apkFile.absolutePath
        }

        val packageName = archive.packageName
        val downloadedVersionCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            else pm.getPackageInfo(packageName, 0)
        }.getOrNull()
        val installedVersionCode = installed?.let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() }
        val installedAppInfo = installed?.applicationInfo
        val label = installedAppInfo?.let { pm.getApplicationLabel(it).toString() }
            ?: archive.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
        val icon = installedAppInfo?.loadIcon(pm) ?: archive.applicationInfo?.loadIcon(pm)
        val launchIntent = installed?.let { pm.getLaunchIntentForPackage(packageName) }

        return InstalledApkState(
            packageName = packageName,
            installed = installed != null,
            installedVersionCode = installedVersionCode,
            installedVersionName = installed?.versionName,
            label = label,
            icon = icon,
            launchIntent = launchIntent,
            downloadedVersionCode = downloadedVersionCode,
            downloadedVersionName = archive.versionName
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveArchive(pm: PackageManager, file: File): PackageInfo? {
        val baseFlags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
        val flags = if (Build.VERSION.SDK_INT >= 28) {
            baseFlags or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            baseFlags or PackageManager.GET_SIGNATURES
        }
        return runCatching { pm.getPackageArchiveInfo(file.absolutePath, flags) }.getOrNull()
            ?: runCatching { pm.getPackageArchiveInfo(file.absolutePath, 0) }.getOrNull()
    }

    fun launchInstalledApp(context: Context, state: InstalledApkState): Boolean {
        val intent = state.launchIntent ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /**
     * Opens the downloaded APK with Android's package installer.
     *
     * The method deliberately does not require PackageManager archive metadata to succeed:
     * a valid APK can still be installable when archive inspection fails on some Android builds.
     * If the exact same or an older version is already installed, the installed application is
     * opened instead of incorrectly launching the installer again.
     */
    fun launchInstaller(context: Context, apkFile: File): Result<Unit> = runCatching {
        require(apkFile.isFile && apkFile.length() > 0L) {
            "The downloaded APK file is no longer available. Download it again."
        }

        val packageManager = context.packageManager
        val archive = resolveArchive(packageManager, apkFile)
        if (archive != null) {
            val packageName = archive.packageName
            val downloadedVersionCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
            val installed = runCatching {
                if (Build.VERSION.SDK_INT >= 33) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    packageManager.getPackageInfo(packageName, 0)
                }
            }.getOrNull()
            val installedVersionCode = installed?.let {
                if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
            }

            // The Downloads UI may still be waiting for its async package-state refresh.
            // Resolve the decision here too, so the primary action can never install an APK
            // that is already installed at the same or a newer version.
            if (installed != null && installedVersionCode != null && downloadedVersionCode <= installedVersionCode) {
                packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@runCatching
                }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            type = "application/vnd.android.package-archive"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolvedIntent = if (packageManager.resolveActivity(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            ) != null
        ) {
            installIntent
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                data = uri
                type = "application/vnd.android.package-archive"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also { fallback ->
                require(
                    packageManager.resolveActivity(fallback, PackageManager.MATCH_DEFAULT_ONLY) != null
                ) {
                    "No Android package installer is available on this device."
                }
            }
        }

        val targetPackage = packageManager
            .resolveActivity(resolvedIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

        if (targetPackage != null) {
            context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(resolvedIntent)
    }
}
