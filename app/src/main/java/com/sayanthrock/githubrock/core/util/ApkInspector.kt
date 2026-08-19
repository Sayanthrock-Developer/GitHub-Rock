package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
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

interface PackageManagerWrapper {
    fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo?
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo
    fun loadLabel(applicationInfo: ApplicationInfo): String?
}

object ApkInspector {
    fun inspect(context: Context, file: File): ApkInspection? {
        return inspect(file, object : PackageManagerWrapper {
            override fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo? =
                context.packageManager.getPackageArchiveInfo(archiveFilePath, flags)

            override fun getPackageInfo(packageName: String, flags: Int): PackageInfo =
                context.packageManager.getPackageInfo(packageName, flags)

            override fun loadLabel(applicationInfo: ApplicationInfo): String? =
                applicationInfo.loadLabel(context.packageManager)?.toString()
        })
    }

    internal fun inspect(file: File, pm: PackageManagerWrapper): ApkInspection? {
        val flags = PackageManager.GET_PERMISSIONS or signatureFlags()
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return null
        archive.applicationInfo?.sourceDir = file.absolutePath
        archive.applicationInfo?.publicSourceDir = file.absolutePath

        val packageName = archive.packageName
        val downloadedFingerprint = archive.signingFingerprint()
        val installed = runCatching { pm.getPackageInfo(packageName, flags) }.getOrNull()

        return ApkInspection(
            appName = archive.applicationInfo?.let { pm.loadLabel(it) } ?: packageName,
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

    private fun signatureFlags(): Int =
        if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun PackageInfo.signingFingerprint(): String? {
        val signatureBytes = if (Build.VERSION.SDK_INT >= 28) {
            signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            signatures?.firstOrNull()?.toByteArray()
        } ?: return null

        return MessageDigest.getInstance("SHA-256")
            .digest(signatureBytes)
            .joinToString(":") { "%02X".format(it) }
    }
}
