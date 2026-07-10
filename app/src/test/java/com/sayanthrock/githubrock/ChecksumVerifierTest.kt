package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumVerifierTest {
    @Test fun computesKnownSha256() {
        val result = ChecksumVerifier.sha256("GitHub Rock".byteInputStream())
        assertEquals("529fd561e17e6941665bf20fb69f7aa7ea42357f27b65174abb39161d1518608", result)
    }

    @Test fun comparisonIsCaseInsensitiveButExact() {
        assertTrue(ChecksumVerifier.matches("AABB", "aabb"))
        assertFalse(ChecksumVerifier.matches("aabb", "aabc"))
    }
}
