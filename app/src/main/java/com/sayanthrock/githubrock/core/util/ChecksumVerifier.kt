package com.sayanthrock.githubrock.core.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object ChecksumVerifier {
    private val sha256Pattern = Regex("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])")

    fun sha256(file: File): String = file.inputStream().use(::sha256)

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts a SHA-256 digest from a raw digest or common checksum-file formats.
     * Supports formats such as:
     *   <digest>
     *   <digest>  filename.apk
     *   SHA256 (filename.apk) = <digest>
     */
    fun parseExpected(expected: String): String? =
        sha256Pattern.find(expected.trim())?.value?.lowercase()

    fun matches(actual: String, expected: String): Boolean {
        val normalizedActual = parseExpected(actual)
        val normalizedExpected = parseExpected(expected)
        if (normalizedActual != null && normalizedExpected != null) {
            return MessageDigest.isEqual(
                normalizedActual.toByteArray(Charsets.US_ASCII),
                normalizedExpected.toByteArray(Charsets.US_ASCII)
            )
        }

        // Preserve the public exact-match contract for already-normalized
        // non-SHA values without weakening SHA-256 parsing/validation.
        return actual.trim().equals(expected.trim(), ignoreCase = true)
    }
}
