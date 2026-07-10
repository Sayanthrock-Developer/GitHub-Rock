package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkInspection(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val fileSize: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val signingSha256: String?,
    val fileSha256: String,
    val installedSignatureMatches: Boolean?
)

object ApkInspector {
    @Suppress("DEPRECATION")
    fun inspect(context: Context, file: File): ApkInspection? {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: return null
        archive.applicationInfo?.sourceDir = file.absolutePath
        archive.applicationInfo?.publicSourceDir = file.absolutePath
        val packageName = archive.packageName
        val downloadedFingerprint = archive.signingFingerprint()
        val installed = runCatching { context.packageManager.getPackageInfo(packageName, flags) }.getOrNull()
        return ApkInspection(
            appName = archive.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: packageName,
            packageName = packageName,
            versionName = archive.versionName.orEmpty(),
            versionCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong(),
            fileSize = file.length(),
            minSdk = archive.applicationInfo?.minSdkVersion ?: 0,
            targetSdk = archive.applicationInfo?.targetSdkVersion ?: 0,
            permissions = archive.requestedPermissions?.sorted().orEmpty(),
            signingSha256 = downloadedFingerprint,
            fileSha256 = ChecksumVerifier.sha256(file),
            installedSignatureMatches = installed?.signingFingerprint()?.let { it == downloadedFingerprint }
        )
    }

    private fun PackageInfo.signingFingerprint(): String? {
        val signature = signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString(":") { "%02X".format(it) }
    }
}

