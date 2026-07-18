package com.sayanthrock.githubrock.core.model

data class AppInformation(
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val applicationId: String,
    val buildType: String,
    val minimumSdk: Int,
    val targetSdk: Int,
    val deviceSdk: Int,
    val androidVersion: String,
    val securityPatch: String,
    val device: String,
    val supportedAbis: List<String>,
    val firstInstalled: String,
    val lastUpdated: String,
    val requestedPermissions: Int
)
