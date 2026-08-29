package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
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
        val archive = resolveArchive(pm, apkFile) ?: return null

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
}
