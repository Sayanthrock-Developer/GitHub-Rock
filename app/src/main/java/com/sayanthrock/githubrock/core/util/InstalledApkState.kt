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

        // Installation is an explicit action. Do not silently open an already-installed
        // application from this method: Downloads UI owns the separate Open action.
        // This keeps Download -> saved APK and Install -> Android package installer distinct.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)

        // ACTION_VIEW is the portable Android package-install handoff. Some Android builds do
        // not expose ACTION_INSTALL_PACKAGE to third-party apps even though their system package
        // installer can handle APK files. ClipData makes the URI grant survive stricter Android
        // URI-permission handling on newer releases.
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = android.content.ClipData.newRawUri("APK", uri)
        }

        val installerActivities = packageManager.queryIntentActivities(
            installIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        require(installerActivities.isNotEmpty()) {
            "Android package installer is unavailable for APK files on this device."
        }

        // Grant the URI only to the actual installer targets.
        installerActivities.forEach { resolveInfo ->
            val targetPackage = resolveInfo.activityInfo?.packageName ?: return@forEach
            context.grantUriPermission(
                targetPackage,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        context.startActivity(installIntent)
    }
}
