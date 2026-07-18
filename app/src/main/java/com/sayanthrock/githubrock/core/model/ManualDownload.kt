package com.sayanthrock.githubrock.core.model

import java.net.URI
import java.util.Locale

enum class ManualDownloadType { Image, File }

data class ManualDownloadRequest(
    val type: ManualDownloadType,
    val url: String,
    val fileName: String
)

data class ManualDownloadValidation(
    val request: ManualDownloadRequest? = null,
    val error: String? = null
)

fun validateManualDownload(
    type: ManualDownloadType,
    sourceUrl: String,
    requestedFileName: String
): ManualDownloadValidation {
    val url = sourceUrl.trim()
    val uri = runCatching { URI(url) }.getOrNull()
        ?: return ManualDownloadValidation(error = "Enter a valid GitHub HTTPS download link.")
    val host = uri.host?.lowercase(Locale.US)
    if (
        uri.scheme?.lowercase(Locale.US) != "https" ||
        host == null ||
        !host.isTrustedGitHubDownloadHost() ||
        uri.userInfo != null ||
        uri.port !in setOf(-1, 443)
    ) {
        return ManualDownloadValidation(
            error = "Use an HTTPS link hosted by GitHub or GitHubusercontent."
        )
    }

    val fallback = if (type == ManualDownloadType.Image) "github-image.jpg" else "github-file.bin"
    val candidate = requestedFileName.trim().ifBlank {
        uri.path?.substringAfterLast('/').orEmpty().ifBlank { fallback }
    }
    var fileName = candidate
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
        .trim('.', '-', '_')
        .take(120)
        .ifBlank { fallback }

    if (type == ManualDownloadType.Image) {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)
        if (extension.isBlank()) fileName = "${fileName.take(115)}.jpg"
        else if (extension !in IMAGE_EXTENSIONS) {
            return ManualDownloadValidation(error = "Image downloads must use PNG, JPG, JPEG, WebP, or GIF.")
        }
    } else if ('.' !in fileName) {
        fileName = "${fileName.take(116)}.bin"
    }

    return ManualDownloadValidation(
        request = ManualDownloadRequest(type = type, url = uri.toASCIIString(), fileName = fileName)
    )
}

private fun String.isTrustedGitHubDownloadHost(): Boolean =
    this == "github.com" || endsWith(".github.com") ||
        this == "githubusercontent.com" || endsWith(".githubusercontent.com")

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
