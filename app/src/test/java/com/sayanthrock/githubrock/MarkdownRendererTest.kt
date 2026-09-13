package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererTest {
    @Test
    fun `renders headings bullets quotes and fenced code safely`() {
        val blocks = MarkdownRenderer.render(
            "# Title\n\n- One\n> Note\n\n```kotlin\nval value = 1\n```"
        )

        assertEquals(MarkdownBlockKind.Heading, blocks[0].kind)
        assertEquals(MarkdownBlockKind.Bullet, blocks[1].kind)
        assertEquals(MarkdownBlockKind.Quote, blocks[2].kind)
        assertEquals(MarkdownBlockKind.Code, blocks[3].kind)
        assertEquals("val value = 1", blocks[3].text)
    }

    @Test
    fun `ignores empty markdown markers instead of rendering them as README content`() {
        val blocks = MarkdownRenderer.render("#\n##\n###\n*\n**\n***\n\nActual README")

        assertEquals(2, blocks.size)
        assertEquals(MarkdownBlockKind.Divider, blocks[0].kind)
        assertEquals(MarkdownBlockKind.Paragraph, blocks[1].kind)
        assertEquals("Actual README", blocks[1].text)
        assertFalse(blocks.any { it.text == "#" || it.text == "##" || it.text == "###" || it.text == "*" || it.text == "**" })
    }

    @Test
    fun `preserves inline markdown instead of flattening it`() {
        val blocks = MarkdownRenderer.render("[Open](https://example.com) <script>alert(1)</script>")

        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlockKind.Paragraph, blocks.single().kind)
        assertEquals(
            "[Open](https://example.com) <script>alert(1)</script>",
            blocks.single().text
        )
    }

    @Test
    fun `cleanInline remains available for explicit plain text conversion`() {
        assertEquals(
            "Open alert(1)",
            MarkdownRenderer.cleanInline("[Open](https://example.com) <script>alert(1)</script>")
        )
    }
}
