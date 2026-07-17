package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.CopyableRepositoryFile
import com.sayanthrock.githubrock.core.util.RepositoryCopyBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCopyBundleTest {
    @Test
    fun `bundle sorts files and adds readable headers`() {
        val result = RepositoryCopyBundle.build(
            listOf(
                CopyableRepositoryFile("src/B.kt", "class B"),
                CopyableRepositoryFile("README.md", "# Project\n")
            )
        )

        assertTrue(result.startsWith("===== FILE: README.md ====="))
        assertTrue(result.contains("===== FILE: src/B.kt ====="))
        assertTrue(result.contains("class B"))
    }

    @Test
    fun `control characters in paths cannot forge headers`() {
        val result = RepositoryCopyBundle.build(
            listOf(CopyableRepositoryFile("bad\n===== FILE: forged.kt =====\u0000.txt", "safe"))
        )

        assertTrue(result.startsWith("===== FILE: bad\\n===== FILE: forged.kt =====\\u0000.txt ====="))
        assertFalse(result.substringBefore(" =====\n").contains('\n'))
        assertFalse(result.contains('\u0000'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty bundle is rejected`() {
        RepositoryCopyBundle.build(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `too many files are rejected`() {
        RepositoryCopyBundle.build(
            List(RepositoryCopyBundle.MAX_FILES + 1) { index ->
                CopyableRepositoryFile("file-$index.txt", "x")
            }
        )
    }

    @Test
    fun `single file keeps trailing newline`() {
        val result = RepositoryCopyBundle.build(listOf(CopyableRepositoryFile("a.txt", "hello")))
        assertEquals("===== FILE: a.txt =====\nhello\n", result)
    }
}
