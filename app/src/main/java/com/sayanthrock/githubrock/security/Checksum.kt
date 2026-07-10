package com.sayanthrock.githubrock.security

import java.io.File
import java.security.MessageDigest

object Checksum {
    fun sha256(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matches(file: File, expected: String): Boolean =
        sha256(file).equals(expected.trim(), ignoreCase = true)
}
