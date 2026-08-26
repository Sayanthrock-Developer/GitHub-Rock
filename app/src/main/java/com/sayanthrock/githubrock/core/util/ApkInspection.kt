package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkInspection(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSize: Long,
    val permissions: List<String>,
    val sha256: String
)

fun inspectApk(context: Context, file: File): ApkInspection {
    require(file.isFile) { "The downloaded APK file is no longer available." }
    require(file.extension.equals("apk", ignoreCase = true)) { "This file is not an APK." }
    val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS)
        ?: error("Android could not read this APK.")
    packageInfo.applicationInfo?.sourceDir = file.absolutePath
    packageInfo.applicationInfo?.publicSourceDir = file.absolutePath
    val appInfo = packageInfo.applicationInfo
    val appName = appInfo?.let { context.packageManager.getApplicationLabel(it).toString() }
        ?.trim()?.takeIf(String::isNotBlank) ?: file.nameWithoutExtension
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    return ApkInspection(
        appName = appName,
        packageName = packageInfo.packageName,
        versionName = packageInfo.versionName ?: "Unknown",
        versionCode = versionCode,
        minSdk = appInfo?.minSdkVersion ?: 0,
        targetSdk = appInfo?.targetSdkVersion ?: 0,
        fileSize = file.length(),
        permissions = packageInfo.requestedPermissions?.toList().orEmpty().sorted(),
        sha256 = sha256(file)
    )
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
