package com.sayanthrock.githubrock.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ChecksumVerifierTest {

    private val knownDigest = "529fd561e17e6941665bf20fb69f7aa7ea42357f27b65174abb39161d1518608"

    @Test
    fun computesKnownSha256_fromInputStream() {
        val result = ChecksumVerifier.sha256("GitHub Rock".byteInputStream())
        assertEquals(knownDigest, result)
    }

    @Test
    fun computesKnownSha256_fromFile() {
        val tempFile = File.createTempFile("test_checksum", ".txt")
        try {
            tempFile.writeText("GitHub Rock")
            val result = ChecksumVerifier.sha256(tempFile)
            assertEquals(knownDigest, result)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun computesSha256_emptyInput() {
        val result = ChecksumVerifier.sha256("".byteInputStream())
        val expected = MessageDigest.getInstance("SHA-256").digest(ByteArray(0)).joinToString("") { "%02x".format(it) }
        assertEquals(expected, result)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result)
    }

    @Test
    fun computesSha256_largeInput() {
        val size = 16 * 1024
        val data = ByteArray(size) { it.toByte() }
        val result = ChecksumVerifier.sha256(data.inputStream())
        val expected = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
        assertEquals(expected, result)
    }

    @Test
    fun comparisonIsCaseInsensitiveButExact() {
        assertTrue(ChecksumVerifier.matches("AABB", "aabb"))
        assertTrue(ChecksumVerifier.matches("aabb", "AABB"))
        assertFalse(ChecksumVerifier.matches("aabb", "aabc"))
    }

    @Test
    fun matches_rawSha256Digest() {
        assertTrue(ChecksumVerifier.matches(knownDigest, knownDigest.uppercase()))
    }

    @Test
    fun matches_gnuChecksumFileFormat() {
        assertTrue(ChecksumVerifier.matches(knownDigest, "$knownDigest  GitHub-Rock.apk"))
    }

    @Test
    fun matches_openSslChecksumFileFormat() {
        assertTrue(ChecksumVerifier.matches(knownDigest, "SHA256 (GitHub-Rock.apk) = $knownDigest"))
    }

    @Test
    fun parseExpected_rejectsInvalidChecksum() {
        assertNull(ChecksumVerifier.parseExpected("not-a-sha256-checksum"))
        assertNull(ChecksumVerifier.parseExpected("aabb"))
    }

    @Test
    fun matches_rejectsDifferentDigestInsideValidChecksumFormat() {
        val different = "0".repeat(64)
        assertFalse(ChecksumVerifier.matches(knownDigest, "$different  GitHub-Rock.apk"))
    }
}
