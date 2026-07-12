package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.DiffLineKind
import com.sayanthrock.githubrock.core.util.TextDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffTest {
    @Test
    fun `unchanged content has no diff`() {
        assertTrue(TextDiff.unified("one\ntwo", "one\ntwo").isEmpty())
    }

    @Test
    fun `changed content preserves context and marks additions and removals`() {
        val diff = TextDiff.unified("one\ntwo\nthree", "one\nupdated\nthree\nfour")

        assertEquals(DiffLineKind.Context, diff[0].kind)
        assertEquals(DiffLineKind.Removed, diff[1].kind)
        assertEquals(DiffLineKind.Added, diff[2].kind)
        assertEquals(DiffLineKind.Context, diff[3].kind)
        assertEquals(DiffLineKind.Added, diff[4].kind)
    }

    @Test
    fun `large content triggers bounded diff`() {
        val before = (1..450).joinToString("\n") { "line $it" }
        val after = (1..450).map { if (it == 100) "line 100 updated" else "line $it" }.joinToString("\n")
        val diff = TextDiff.unified(before, after)

        assertTrue(diff.any { it.kind == DiffLineKind.Removed })
        assertTrue(diff.any { it.kind == DiffLineKind.Added })
    }
}
