package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceFileDecoderTest {
    @Test
    fun `decodes GitHub base64 content with line breaks`() {
        val encoded = java.util.Base64.getEncoder().encodeToString("name: GitHub Rock\n".toByteArray())
        val wrapped = encoded.chunked(4).joinToString("\n")
        val entry = ContentEntry(
            name = "workflow.yml",
            path = ".github/workflows/workflow.yml",
            content = wrapped,
            encoding = "base64"
        )

        assertEquals("name: GitHub Rock\n", SourceFileDecoder.decode(entry))
        assertEquals("name: GitHub Rock\n", SourceFileDecoder.decodeStrictText(entry))
    }

    @Test
    fun `decoder preserves a legitimate empty file`() {
        assertEquals(
            "",
            SourceFileDecoder.decode(
                ContentEntry(name = "empty.txt", path = "empty.txt", content = "", encoding = "base64")
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decoder rejects unavailable content`() {
        SourceFileDecoder.decode(
            ContentEntry(name = "missing.txt", path = "missing.txt", content = null, encoding = "base64")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decoder rejects malformed Base64 characters`() {
        SourceFileDecoder.decode(
            ContentEntry(name = "bad-base64.txt", path = "bad-base64.txt", content = "YQ$==", encoding = "base64")
        )
    }

    @Test(expected = java.nio.charset.CharacterCodingException::class)
    fun `decoder rejects malformed UTF-8`() {
        val encoded = java.util.Base64.getEncoder().encodeToString(byteArrayOf(0xC3.toByte(), 0x28))
        SourceFileDecoder.decode(
            ContentEntry(name = "bad.txt", path = "bad.txt", content = encoded, encoding = "base64")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decoder rejects binary controls`() {
        val encoded = java.util.Base64.getEncoder().encodeToString(byteArrayOf('A'.code.toByte(), 0, 'B'.code.toByte()))
        SourceFileDecoder.decode(
            ContentEntry(name = "binary.json", path = "binary.json", content = encoded, encoding = "base64")
        )
    }
}
