package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import org.junit.Assert.assertEquals
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
    fun `preserves standalone markdown punctuation and parses divider syntax`() {
        val blocks = MarkdownRenderer.render("#\n##\n###\n*\n**\n***\n\nActual README")

        assertEquals(7, blocks.size)
        assertEquals(listOf("#", "##", "###", "*", "**"), blocks.take(5).map { it.text })
        assertTrue(blocks.take(5).all { it.kind == MarkdownBlockKind.Paragraph })
        assertEquals(MarkdownBlockKind.Divider, blocks[5].kind)
        assertEquals("", blocks[5].text)
        assertEquals(MarkdownBlockKind.Paragraph, blocks[6].kind)
        assertEquals("Actual README", blocks[6].text)
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
