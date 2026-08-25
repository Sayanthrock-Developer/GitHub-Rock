package com.sayanthrock.githubrock.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererTest {
    @Test fun `render heading blocks`() {
        val result = MarkdownRenderer.render("# Heading 1\n## Heading 2\n###### Heading 6")
        assertEquals(3, result.size)
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 1", 1), result[0])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 2", 2), result[1])
        assertEquals(MarkdownBlock(MarkdownBlockKind.Heading, "Heading 6", 6), result[2])
    }

    @Test fun `render paragraph blocks`() {
        val result = MarkdownRenderer.render("This is a paragraph.\nIt spans multiple lines.\n\nThis is another paragraph.")
        assertEquals(2, result.size)
        assertEquals("This is a paragraph. It spans multiple lines.", result[0].text)
        assertEquals("This is another paragraph.", result[1].text)
    }

    @Test fun `render lists and quotes`() {
        val result = MarkdownRenderer.render("- Item 1\n* Item 2\n1. First\n2) Second\n> Quote")
        assertEquals(5, result.size)
        assertEquals("Item 1", result[0].text)
        assertEquals("Item 2", result[1].text)
        assertEquals("1. First", result[2].text)
        assertEquals("2. Second", result[3].text)
        assertEquals(MarkdownBlockKind.Quote, result[4].kind)
    }

    @Test fun `render code blocks and dividers`() {
        val result = MarkdownRenderer.render("```\nfun main() {\n    println(\"Hello\")\n}\n```\n\n---")
        assertEquals(2, result.size)
        assertEquals(MarkdownBlockKind.Code, result[0].kind)
        assertEquals("fun main() {\n    println(\"Hello\")\n}", result[0].text)
        assertEquals(MarkdownBlockKind.Divider, result[1].kind)
    }

    @Test fun `preserve inline markdown for native renderer`() {
        val markdown = "**bold** *italic* `code` ~~strike~~ [Link](https://example.com)"
        val result = MarkdownRenderer.render(markdown)
        assertEquals(1, result.size)
        assertEquals(markdown, result[0].text)
        assertTrue(result[0].kind == MarkdownBlockKind.Paragraph)
    }

    @Test fun `render markdown table as structured block`() {
        val markdown = "| Name | Stars |\n| :--- | ---: |\n| Rock | 42 |\n| App | 10 |"
        val result = MarkdownRenderer.render(markdown)
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Table, result[0].kind)
        assertEquals(listOf("Name", "Stars"), result[0].table?.headers)
        assertEquals(listOf(listOf("Rock", "42"), listOf("App", "10")), result[0].table?.rows)
    }

    @Test fun `render images without flattening`() {
        val result = MarkdownRenderer.render("![Logo](https://example.com/logo.png)")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Image, result[0].kind)
        assertEquals("https://example.com/logo.png", result[0].url)
    }
}
