package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.AppInformation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppInformationProvider {
    fun read(context: Context): AppInformation {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        val applicationInfo = packageInfo.applicationInfo ?: context.applicationInfo
        return AppInformation(
            appName = context.getString(R.string.app_name),
            versionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME,
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            applicationId = context.packageName,
            buildType = BuildConfig.BUILD_TYPE,
            minimumSdk = 29,
            compileSdk = 36,
            targetSdk = applicationInfo.targetSdkVersion,
            deviceSdk = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "Not reported" },
            device = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter(String::isNotBlank)
                .joinToString(" ")
                .ifBlank { "Unknown Android device" },
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            firstInstalled = packageInfo.firstInstallTime.asReadableDate(),
            lastUpdated = packageInfo.lastUpdateTime.asReadableDate(),
            requestedPermissions = packageInfo.requestedPermissions?.size ?: 0,
            gitCommit = BuildConfig.GIT_COMMIT,
            gitRef = BuildConfig.GIT_REF,
            buildTimestampUtc = BuildConfig.BUILD_TIMESTAMP_UTC
        )
    }

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        .withZone(ZoneId.systemDefault())

    private fun Long.asReadableDate(): String = runCatching {
        DATE_FORMATTER.format(Instant.ofEpochMilli(this))
    }.getOrDefault("Unknown")
}
