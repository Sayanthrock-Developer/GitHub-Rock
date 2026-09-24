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

    @Test fun `preserve standalone markdown punctuation as literal paragraphs`() {
        val result = MarkdownRenderer.render("#\n##\n###\n*\n**\n_\n__")
        assertEquals(7, result.size)
        assertEquals(listOf("#", "##", "###", "*", "**", "_", "__"), result.map { it.text })
        assertTrue(result.all { it.kind == MarkdownBlockKind.Paragraph })
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
        assertEquals("First", result[2].text)
        assertTrue(result[2].ordered)
        assertEquals(1, result[2].level)
        assertEquals("Second", result[3].text)
        assertTrue(result[3].ordered)
        assertEquals(2, result[3].level)
        assertEquals(MarkdownBlockKind.Quote, result[4].kind)
    }

    @Test fun `render code blocks and dividers`() {
        val result = MarkdownRenderer.render("```\nfun main() {\n    println(\"Hello\")\n}\n```\n\n---")
        assertEquals(2, result.size)
        assertEquals(MarkdownBlockKind.Code, result[0].kind)
        assertEquals("fun main() {\n    println(\"Hello\")\n}", result[0].text)
        assertEquals(MarkdownBlockKind.Divider, result[1].kind)
    }

    @Test fun `render common GitHub task and alert blocks`() {
        val result = MarkdownRenderer.render("- [x] Done\n- [ ] Todo\n> [!WARNING] Check this first")
        assertEquals(MarkdownBlockKind.Task, result[0].kind)
        assertTrue(result[0].checked)
        assertEquals(MarkdownBlockKind.Task, result[1].kind)
        assertTrue(!result[1].checked)
        assertEquals(MarkdownBlockKind.Alert, result[2].kind)
        assertEquals("Check this first", result[2].text)
    }

    @Test fun `preserve fenced code language and whitespace`() {
        val result = MarkdownRenderer.render("```kotlin\n  first\n    second\nvery-long-line\n```")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Code, result[0].kind)
        assertEquals("kotlin", result[0].codeLanguage)
        assertEquals("  first\n    second\nvery-long-line", result[0].text)
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

    @Test fun `table does not swallow following prose containing a pipe`() {
        val markdown = "| Name | Stars |\n| --- | --- |\n| Rock | 42 |\n\nA link with a | pipe"
        val result = MarkdownRenderer.render(markdown)
        assertEquals(2, result.size)
        assertEquals(MarkdownBlockKind.Table, result[0].kind)
        assertEquals("A link with a | pipe", result[1].text)
    }

    @Test fun `render images without flattening`() {
        val result = MarkdownRenderer.render("![Logo](https://example.com/logo.png)")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Image, result[0].kind)
        assertEquals("https://example.com/logo.png", result[0].url)
        assertEquals("Logo", result[0].text)
    }

    @Test fun `render html image alt text`() {
        val result = MarkdownRenderer.render("<img src=\"https://example.com/logo.png\" alt=\"GitHub Rock\">")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Image, result[0].kind)
        assertEquals("https://example.com/logo.png", result[0].url)
        assertEquals("GitHub Rock", result[0].text)
    }

    @Test fun `render README html wrappers without exposing tags`() {
        val markdown = "<div align=\"center\">\n<img src=\"https://example.com/logo.png\" alt=\"Logo\" />\n</div>\n\n# GitHub Rock\n\n<strong>Native GitHub client</strong>"
        val result = MarkdownRenderer.render(markdown)
        assertEquals(3, result.size)
        assertEquals(MarkdownBlockKind.Image, result[0].kind)
        assertEquals("https://example.com/logo.png", result[0].url)
        assertEquals(MarkdownBlockKind.Heading, result[1].kind)
        assertEquals("GitHub Rock", result[1].text)
        assertEquals(MarkdownBlockKind.Paragraph, result[2].kind)
        assertEquals("Native GitHub client", result[2].text)
    }

    @Test fun `render multiple html images from README line`() {
        val markdown = "<p><a href=\"https://example.com/release\"><img src=\"https://example.com/release.png\" alt=\"Release\"></a><a href=\"https://example.com/build\"><img src=\"https://example.com/build.png\" alt=\"Build\"></a></p>"
        val result = MarkdownRenderer.render(markdown)
        assertEquals(2, result.size)
        assertEquals("https://example.com/release.png", result[0].url)
        assertEquals("Release", result[0].text)
        assertEquals("https://example.com/build.png", result[1].url)
        assertEquals("Build", result[1].text)
    }

    @Test fun `normalize CRLF and CR line endings`() {
        val crlf = MarkdownRenderer.render("# Title\r\n\r\nParagraph\r\nNext")
        val cr = MarkdownRenderer.render("# Title\r\rParagraph\rNext")
        assertEquals(listOf("Title", "Paragraph Next"), crlf.map { it.text })
        assertEquals(listOf("Title", "Paragraph Next"), cr.map { it.text })
    }

    @Test fun `cleanInline removes markdown without corrupting escapes`() {
        val markdown = "**bold** *italic* ~~strike~~ `code` [Link](https://example.com)"
        assertEquals("bold italic strike code Link", MarkdownRenderer.cleanInline(markdown))
    }

    @Test fun `parse GitHub picture light and dark image sources`() {
        val result = MarkdownRenderer.render("""
            <picture>
            <source media="(prefers-color-scheme: dark)" srcset="dark.svg">
            <source media="(prefers-color-scheme: light)" srcset="light.svg">
            <img src="fallback.svg" alt="Logo">
            </picture>
        """.trimIndent())
        val image = result.single()
        assertEquals(MarkdownBlockKind.Image, image.kind)
        assertEquals("fallback.svg", image.image?.fallbackUrl)
        assertEquals("light.svg", image.image?.lightUrl)
        assertEquals("dark.svg", image.image?.darkUrl)
        assertEquals("Logo", image.image?.alt)
    }

    @Test fun `render details as expandable metadata with parsed children`() {
        val result = MarkdownRenderer.render("""
            <details>
            <summary>More information</summary>
            **Hello**
            </details>
        """.trimIndent())
        val details = result.single()
        assertEquals(MarkdownBlockKind.Details, details.kind)
        assertEquals("More information", details.details?.summary)
        assertEquals("**Hello**", details.details?.blocks?.single()?.text)
    }

    @Test fun `group multiple README images into an inline row`() {
        val result = MarkdownRenderer.render("![One](one.svg) ![Two](two.svg)")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.ImageRow, result[0].kind)
        assertEquals(listOf("one.svg", "two.svg"), result[0].imageRow.map { it.fallbackUrl })
    }

    @Test fun `decode html entities and convert sup sub markup`() {
        assertEquals("A & B < 2² H₂O", MarkdownRenderer.cleanInline("A &amp; B &lt; 2<sup>2</sup> H<sub>2</sub>O"))
    }

    @Test fun `render html pre code with language class and entities`() {
        val result = MarkdownRenderer.render("""<pre><code class="language-kotlin">fun main() {
    println(&quot;Hello &amp; Rock&quot;)
}</code></pre>""")
        assertEquals(1, result.size)
        assertEquals(MarkdownBlockKind.Code, result[0].kind)
        assertEquals("kotlin", result[0].codeLanguage)
        assertEquals("fun main() {\n    println(\"Hello & Rock\")\n}", result[0].text)
    }

    @Test fun `render html blockquote as native quote`() {
        val result = MarkdownRenderer.render("<blockquote><p>Quoted &amp; text</p><p>Second line</p></blockquote>")
        assertEquals(2, result.size)
        assertTrue(result.all { it.kind == MarkdownBlockKind.Quote })
        assertEquals("Quoted & text", result[0].text)
        assertEquals("Second line", result[1].text)
    }

    @Test fun `decode numeric and hexadecimal html entities`() {
        assertEquals("A-©-😀", MarkdownRenderer.decodeHtmlEntities("A&#45;&#169;&#x1F600;"))
    }

    @Test fun `details summary decodes html entities`() {
        val result = MarkdownRenderer.render("<details><summary>More &amp; info</summary>Body</details>")
        assertEquals(MarkdownBlockKind.Details, result.single().kind)
        assertEquals("More & info", result.single().details?.summary)
    }

}
