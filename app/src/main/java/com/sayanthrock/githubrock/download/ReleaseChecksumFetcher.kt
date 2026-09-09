package com.sayanthrock.githubrock.download

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches an optional SHA-256 entry without making checksum availability a
 * prerequisite for the binary download.
 */
internal object ReleaseChecksumFetcher {
    fun tryFetch(client: OkHttpClient, checksumUrl: String, targetName: String): String? =
        runCatching { fetch(client, checksumUrl, targetName) }.getOrNull()

    private fun fetch(client: OkHttpClient, checksumUrl: String, targetName: String): String {
        val request = Request.Builder()
            .url(checksumUrl)
            .header("User-Agent", "GitHub-Rock/1.0")
            .header("Accept", "text/plain, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Checksum download failed: HTTP ${response.code}")
            val text = response.body?.string()?.takeIf { it.isNotBlank() }
                ?: error("Checksum file is empty")
            return parseSha256(text, targetName)
                ?: error("No SHA-256 entry found for $targetName")
        }
    }

    private fun parseSha256(text: String, targetName: String): String? {
        val normalizedTarget = targetName.trim()
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        val entries = lines.mapNotNull { line ->
            val match = Regex("^([A-Fa-f0-9]{64})\\s+(?:\\*|)(.+)$").matchEntire(line) ?: return@mapNotNull null
            match.groupValues[1].lowercase() to match.groupValues[2].trim().removePrefix("*")
        }.toList()
        entries.firstOrNull { it.second == normalizedTarget || it.second.substringAfterLast('/') == normalizedTarget }?.let { return it.first }
        return if (entries.size == 1) entries.first().first else null
    }
}
