package com.sayanthrock.githubrock.core.apk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.sayanthrock.githubrock.core.util.Checksum
import java.io.File
import java.security.MessageDigest


data class ApkInspection(
    val applicationName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val fileSize: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val requestedPermissions: List<String>,
    val signingSubject: String,
    val signingFingerprint: String,
    val sha256: String,
    val installedFingerprint: String?,
    val signaturesMatch: Boolean?
)

class ApkInspector(private val context: Context) {
    fun inspect(file: File): ApkInspection {
        require(file.exists() && file.extension.equals("apk", ignoreCase = true)) {
            "Select a valid APK file."
        }
        val packageManager = context.packageManager
        val archive = archiveInfo(packageManager, file)
            ?: error("Android could not parse this APK.")
        val appInfo = archive.applicationInfo ?: error("The APK does not contain application metadata.")
        appInfo.sourceDir = file.absolutePath
        appInfo.publicSourceDir = file.absolutePath

        val downloadedCertificate = archive.signingInfo?.apkContentsSigners?.firstOrNull()
        val downloadedFingerprint = downloadedCertificate?.toByteArray()?.let(::fingerprint).orEmpty()
        val installedFingerprint = installedFingerprint(packageManager, archive.packageName)

        return ApkInspection(
            applicationName = appInfo.loadLabel(packageManager).toString(),
            packageName = archive.packageName,
            versionName = archive.versionName.orEmpty(),
            versionCode = archive.longVersionCode,
            fileSize = file.length(),
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion,
            requestedPermissions = archive.requestedPermissions?.sorted().orEmpty(),
            signingSubject = downloadedCertificate?.toCharsString().orEmpty(),
            signingFingerprint = downloadedFingerprint,
            sha256 = Checksum.sha256(file),
            installedFingerprint = installedFingerprint,
            signaturesMatch = installedFingerprint?.let { it == downloadedFingerprint }
        )
    }

    fun install(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(packageManager: PackageManager, file: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        }

    @Suppress("DEPRECATION")
    private fun installedFingerprint(packageManager: PackageManager, packageName: String): String? =
        runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let(::fingerprint)
        }.getOrNull()

    private fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(":") { "%02X".format(it) }
}
