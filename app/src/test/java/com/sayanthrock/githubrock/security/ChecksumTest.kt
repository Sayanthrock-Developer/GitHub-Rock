package com.sayanthrock.githubrock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChecksumTest {
    @Test
    fun calculatesKnownSha256() {
        val file = File.createTempFile("github-rock", ".txt").apply { writeText("GitHub Rock") }
        try {
            val expected = "529fd561e17e6941665bf20fb69f7aa7ea42357f27b65174abb39161d1518608"
            assertEquals(expected, Checksum.sha256(file))
            assertTrue(Checksum.matches(file, expected.uppercase()))
        } finally {
            file.delete()
        }
    }
}
