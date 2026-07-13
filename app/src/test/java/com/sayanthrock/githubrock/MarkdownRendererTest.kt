package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import org.junit.Assert.assertEquals
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
    fun `does not render html tags or link destinations`() {
        val blocks = MarkdownRenderer.render("[Open](https://example.com) <script>alert(1)</script>")

        assertEquals("Open alert(1)", blocks.single().text)
    }
}
