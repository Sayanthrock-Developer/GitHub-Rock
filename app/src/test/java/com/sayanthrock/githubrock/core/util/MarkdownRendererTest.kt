package com.sayanthrock.githubrock.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownRendererTest {

    @Test
    fun `render heading blocks`() {
        val markdown = """
            # Heading 1
            ## Heading 2
            ###### Heading 6
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(3, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 1", 1), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 2", 2), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 6", 6), result[2])
    }

    @Test
    fun `render paragraph blocks`() {
        val markdown = """
            This is a paragraph.
            It spans multiple lines.

            This is another paragraph.
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(2, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Paragraph, "This is a paragraph. It spans multiple lines."), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Paragraph, "This is another paragraph."), result[1])
    }

    @Test
    fun `render bullet points`() {
        val markdown = """
            - Item 1
            * Item 2
            + Item 3
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(3, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "Item 1"), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "Item 2"), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "Item 3"), result[2])
    }

    @Test
    fun `render ordered lists`() {
        val markdown = """
            1. First item
            2) Second item
            10. Tenth item
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(3, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "1. First item"), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "2. Second item"), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "10. Tenth item"), result[2])
    }

    @Test
    fun `render quotes`() {
        val markdown = """
            > This is a quote.
            >This is another quote.
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(2, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Quote, "This is a quote."), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Quote, "This is another quote."), result[1])
    }

    @Test
    fun `render code blocks`() {
        val markdown = """
            ```
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(1, result.size)
        val expectedCode = "fun main() {\n    println(\"Hello\")\n}"
        assertEquals(MarkdownBlock(MarkdownBlockKind.Code, expectedCode), result[0])
    }

    @Test
    fun `render dividers`() {
        val markdown = """
            ---
            ***
            ___
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(3, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Divider, ""), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Divider, ""), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Divider, ""), result[2])
    }

    @Test
    fun `clean inline links and html`() {
        val markdown = """
            # [Link](https://example.com)
            Paragraph with <br> HTML.
            - List with [Link text](https://example.com)
            > Quote with <b>bold</b> text.
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(4, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Link (https://example.com)", 1), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Paragraph, "Paragraph with  HTML."), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "List with Link text (https://example.com)"), result[2])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Quote, "Quote with bold text."), result[3])
    }

    @Test
    fun `render mixed markdown`() {
        val markdown = """
            # Title

            This is a paragraph.

            - Bullet 1
            - Bullet 2

            ```
            Code
            ```

            > Quote

            ---

            Another paragraph.
        """.trimIndent()

        val result = MarkdownRenderer.render(markdown)

        assertEquals(8, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Title", 1), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Paragraph, "This is a paragraph."), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "Bullet 1"), result[2])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Bullet, "Bullet 2"), result[3])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Code, "Code"), result[4])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Quote, "Quote"), result[5])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Divider, ""), result[6])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Paragraph, "Another paragraph."), result[7])
    }
}
