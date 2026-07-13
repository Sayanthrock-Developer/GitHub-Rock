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
    /**
     * Inspects an APK and extracts its metadata, permissions, checksums, and signing details.
     *
     * @param file The APK file to inspect.
     * @return The inspection results for the APK.
     */
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

    /**
     * Opens the APK file in the system installer.
     *
     * @param file The APK file to install.
     */
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

    /**
         * Reads package information and signing certificate details from an APK archive.
         *
         * @param packageManager The package manager used to parse the archive.
         * @param file The APK file to inspect.
         * @return The archive's package information, or `null` if it cannot be parsed.
         */
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

    /**
         * Retrieves the SHA-256 fingerprint of the installed application's signing certificate.
         *
         * @param packageManager The package manager used to retrieve application information.
         * @param packageName The package name of the installed application.
         * @return The signing certificate fingerprint, or `null` if it cannot be retrieved.
         */
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

    /**
         * Computes a colon-separated uppercase SHA-256 fingerprint for the provided bytes.
         *
         * @param bytes The bytes to fingerprint.
         * @return The SHA-256 digest formatted as uppercase hexadecimal pairs separated by colons.
         */
        private fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(":") { "%02X".format(it) }
}
