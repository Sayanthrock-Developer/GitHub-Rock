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
    }
}
