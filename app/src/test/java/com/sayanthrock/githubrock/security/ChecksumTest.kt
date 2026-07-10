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
            assertEquals("d217a297eceb96d08e9418f1cc4df2d830c8cc69b78e06518b5d76a18bfc48cc", Checksum.sha256(file))
            assertTrue(Checksum.matches(file, "D217A297ECEB96D08E9418F1CC4DF2D830C8CC69B78E06518B5D76A18BFC48CC"))
        } finally {
            file.delete()
        }
    }
}
