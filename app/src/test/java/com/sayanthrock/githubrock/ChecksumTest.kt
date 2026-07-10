package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.Checksum
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecksumTest {
    @Test
    fun `sha256 matches known vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Checksum.sha256("abc".encodeToByteArray())
        )
    }
}
