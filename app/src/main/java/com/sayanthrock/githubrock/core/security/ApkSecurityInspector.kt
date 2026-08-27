package com.sayanthrock.githubrock.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

/** Performs local APK inspection before Android's package installer is opened. */
object ApkSecurityInspector {
    data class Result(
        val packageName: String,
        val versionCode: Long,
        val versionName: String?,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List<String>,
        val certificateSha256: String?,
        val signatureSchemes: List<String>,
        val architectures: List<String>,
        val debugSigned: Boolean,
        val checksum: String,
        val permissionDelta: List<String>,
        val certificateChanged: Boolean,
        val downgrade: Boolean,
        val riskReasons: List<String>
    )

    fun inspect(
        context: Context,
        file: File,
        expectedPackage: String? = null,
        previousVersionCode: Long? = null,
        previousPermissions: List<String> = emptyList(),
        previousCertificateSha256: String? = null
    ): Result {
        val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS or
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw InvalidApkException("Unable to parse APK")
        val app = info.applicationInfo ?: throw InvalidApkException("APK has no application info")
        val packageName = info.packageName.takeIf { it.isNotBlank() }
            ?: throw InvalidApkException("APK has no package name")
        if (expectedPackage != null && packageName != expectedPackage) {
            throw InvalidApkException("Package mismatch: expected $expectedPackage, found $packageName")
        }

        val permissions = info.requestedPermissions?.toList()?.sorted().orEmpty()
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        val certs = signingCertificates(info)
        val certificate = certs.firstOrNull()?.let(::sha256)
        val schemes = detectSignatureSchemes(file, info)
        val architectures = detectArchitectures(file)
        val permissionDelta = permissions.filterNot(previousPermissions.toSet()::contains)
        val downgrade = previousVersionCode != null && versionCode < previousVersionCode
        val certificateChanged = previousCertificateSha256 != null &&
            certificate != null && !ChecksumVerifier.matches(certificate, previousCertificateSha256)
        val debugSigned = certs.any(::looksLikeDebugCertificate)
        val reasons = buildList {
            if (downgrade) add("APK version is lower than the previously downloaded version")
            if (certificateChanged) add("Signing certificate changed from the previously observed certificate")
            if (permissionDelta.isNotEmpty()) add("${permissionDelta.size} new permission(s) requested")
            if (debugSigned) add("APK appears to use a debug signing certificate")
            if (schemes.isEmpty()) add("Android could not report a recognized signing scheme for this APK")
        }

        return Result(
            packageName = packageName,
            versionCode = versionCode,
            versionName = info.versionName,
            minSdk = app.minSdkVersion,
            targetSdk = app.targetSdkVersion,
            permissions = permissions,
            certificateSha256 = certificate,
            signatureSchemes = schemes,
            architectures = architectures,
            debugSigned = debugSigned,
            checksum = ChecksumVerifier.sha256(file),
            permissionDelta = permissionDelta,
            certificateChanged = certificateChanged,
            downgrade = downgrade,
            riskReasons = reasons
        )
    }

    private fun signingCertificates(info: android.content.pm.PackageInfo): List<Signature> =
        if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION") info.signatures?.toList().orEmpty()
        }

    private fun sha256(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun looksLikeDebugCertificate(signature: Signature): Boolean = runCatching {
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(signature.toByteArray().inputStream()) as X509Certificate
        certificate.subjectX500Principal.name.contains("CN=Android Debug", ignoreCase = true)
    }.getOrDefault(false)

    private fun detectArchitectures(file: File): List<String> = buildSet {
        ZipFile(file).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory && it.name.startsWith("lib/") }
                .mapNotNull { it.name.split('/').getOrNull(1) }
                .forEach { add(it) }
        }
    }.sorted()

    /** Reports v1 from the APK container and Android's platform-level signing verification for modern APKs. */
    private fun detectSignatureSchemes(file: File, info: android.content.pm.PackageInfo): List<String> = buildList {
        ZipFile(file).use { zip ->
            if (zip.entries().asSequence().any { entry ->
                    entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".RSA", true) || entry.name.endsWith(".DSA", true) || entry.name.endsWith(".EC", true))
                }) add("v1")
        }
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) add("platform-verified")
    }.distinct()

    class InvalidApkException(message: String) : Exception(message)
}
