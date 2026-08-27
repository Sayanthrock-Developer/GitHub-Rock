package com.sayanthrock.githubrock.core.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

data class ApkInspection(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSize: Long,
    val permissions: List<String>,
    val sha256: String,
    val certificateSha256: String?,
    val signatureSchemes: List<String>,
    val architectures: List<String>,
    val debugSigned: Boolean,
    val permissionDelta: List<String>,
    val certificateChanged: Boolean,
    val downgrade: Boolean,
    val riskReasons: List<String>
)

fun inspectApk(
    context: Context,
    file: File,
    expectedPackage: String? = null,
    previousVersionCode: Long? = null,
    previousPermissions: List<String> = emptyList(),
    previousCertificateSha256: String? = null
): ApkInspection {
    require(file.isFile) { "The downloaded APK file is no longer available." }
    require(file.extension.equals("apk", ignoreCase = true)) { "This file is not an APK." }
    val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA or
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        ?: error("Android could not read this APK.")
    val packageName = packageInfo.packageName
    require(expectedPackage == null || expectedPackage == packageName) {
        "APK package mismatch: expected $expectedPackage, found $packageName"
    }
    packageInfo.applicationInfo?.sourceDir = file.absolutePath
    packageInfo.applicationInfo?.publicSourceDir = file.absolutePath
    val appInfo = packageInfo.applicationInfo
    val appName = appInfo?.let { context.packageManager.getApplicationLabel(it).toString() }
        ?.trim()?.takeIf(String::isNotBlank) ?: file.nameWithoutExtension
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    val permissions = packageInfo.requestedPermissions?.toList().orEmpty().sorted()
    val certificates = signingCertificates(packageInfo)
    val certificate = certificates.firstOrNull()?.let(::sha256)
    val signatureSchemes = detectSignatureSchemes(file, packageInfo)
    val architectures = detectArchitectures(file)
    val permissionDelta = permissions.filterNot(previousPermissions.toSet()::contains)
    val certificateChanged = previousCertificateSha256 != null && certificate != null &&
        !ChecksumVerifier.matches(certificate, previousCertificateSha256)
    val downgrade = previousVersionCode != null && versionCode < previousVersionCode
    val debugSigned = certificates.any(::isDebugCertificate)
    val riskReasons = buildList {
        if (downgrade) add("APK version is lower than the previously downloaded version")
        if (certificateChanged) add("Signing certificate changed from the previously observed certificate")
        if (permissionDelta.isNotEmpty()) add("${permissionDelta.size} new permission(s) requested")
        if (debugSigned) add("APK appears to use a debug signing certificate")
        if (signatureSchemes.isEmpty()) add("Android could not report a recognized signing scheme for this APK")
    }
    return ApkInspection(
        appName = appName,
        packageName = packageName,
        versionName = packageInfo.versionName ?: "Unknown",
        versionCode = versionCode,
        minSdk = appInfo?.minSdkVersion ?: 0,
        targetSdk = appInfo?.targetSdkVersion ?: 0,
        fileSize = file.length(),
        permissions = permissions,
        sha256 = ChecksumVerifier.sha256(file),
        certificateSha256 = certificate,
        signatureSchemes = signatureSchemes,
        architectures = architectures,
        debugSigned = debugSigned,
        permissionDelta = permissionDelta,
        certificateChanged = certificateChanged,
        downgrade = downgrade,
        riskReasons = riskReasons
    )
}

private fun signingCertificates(info: PackageInfo): List<Signature> =
    if (Build.VERSION.SDK_INT >= 28) {
        info.signingInfo?.apkContentsSigners?.toList().orEmpty()
    } else {
        @Suppress("DEPRECATION") info.signatures?.toList().orEmpty()
    }

private fun sha256(signature: Signature): String =
    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        .joinToString("") { "%02x".format(it) }

private fun isDebugCertificate(signature: Signature): Boolean = runCatching {
    val certificate = CertificateFactory.getInstance("X.509")
        .generateCertificate(signature.toByteArray().inputStream()) as X509Certificate
    certificate.subjectX500Principal.name.contains("CN=Android Debug", ignoreCase = true)
}.getOrDefault(false)

private fun detectArchitectures(file: File): List<String> = buildSet {
    ZipFile(file).use { zip ->
        zip.entries().asSequence().filter { !it.isDirectory && it.name.startsWith("lib/") }
            .mapNotNull { it.name.split('/').getOrNull(1) }
            .forEach(::add)
    }
}.sorted()

/** Reports v1 from the APK container and Android's platform-level signing verification for modern APKs. */
private fun detectSignatureSchemes(file: File, info: PackageInfo): List<String> = buildList {
    ZipFile(file).use { zip ->
        if (zip.entries().asSequence().any { entry ->
                entry.name.startsWith("META-INF/") &&
                    (entry.name.endsWith(".RSA", true) || entry.name.endsWith(".DSA", true) || entry.name.endsWith(".EC", true))
            }) add("v1")
    }
    if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) add("platform-verified")
}.distinct()
