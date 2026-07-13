package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.SyntaxHighlighter
import com.sayanthrock.githubrock.core.util.SyntaxTokenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {
    @Test
    fun `highlights Kotlin keywords strings comments and numbers`() {
        val source = "val answer = 42 // keep this\nprintln(\"rock\")"
        val spans = SyntaxHighlighter.highlight("Main.kt", source)

        assertEquals(SyntaxTokenKind.Keyword, spans.first { source.substring(it.start, it.end) == "val" }.kind)
        assertEquals(SyntaxTokenKind.Number, spans.first { source.substring(it.start, it.end) == "42" }.kind)
        assertEquals(SyntaxTokenKind.Comment, spans.first { source.substring(it.start, it.end).startsWith("//") }.kind)
        assertEquals(SyntaxTokenKind.String, spans.first { source.substring(it.start, it.end) == "\"rock\"" }.kind)
    }

    @Test
    fun `highlights JSON properties without double colouring the value`() {
        val source = "{\"name\": \"GitHub Rock\", \"enabled\": true}"
        val spans = SyntaxHighlighter.highlight("config.json", source)

        assertTrue(spans.any { it.kind == SyntaxTokenKind.Property && source.substring(it.start, it.end) == "\"name\"" })
        assertTrue(spans.any { it.kind == SyntaxTokenKind.String && source.substring(it.start, it.end) == "\"GitHub Rock\"" })
        assertTrue(spans.any { it.kind == SyntaxTokenKind.Keyword && source.substring(it.start, it.end) == "true" })
        assertEquals(1, spans.count { it.kind == SyntaxTokenKind.String })
    }
}
