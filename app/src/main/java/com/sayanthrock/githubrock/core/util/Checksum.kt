package com.sayanthrock.githubrock.core.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object Checksum {
    /**
         * Computes the SHA-256 checksum of the provided bytes.
         *
         * @param bytes The bytes to hash.
         * @return The checksum as a lowercase hexadecimal string.
         */
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    /**
 * Computes the SHA-256 checksum of a file.
 *
 * @param file The file to checksum.
 * @return The checksum as a lowercase hexadecimal string.
 */
fun sha256(file: File): String = file.inputStream().use(::sha256)

    /**
     * Computes the SHA-256 checksum of the stream contents.
     *
     * @param input The stream to read.
     * @return The checksum as a lowercase hexadecimal string.
     */
    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
