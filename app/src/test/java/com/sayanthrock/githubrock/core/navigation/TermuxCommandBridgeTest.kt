package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxCommandBridgeTest {
    @Test
    fun `valid command keeps explicit project directory`() {
        val execution = TermuxCommandBridge.validate("git status", "/data/data/com.termux/files/home/GitHub-Rock").getOrThrow()
        assertEquals("git status", execution.command)
        assertEquals("/data/data/com.termux/files/home/GitHub-Rock", execution.workingDirectory)
    }

    @Test
    fun `blank command is rejected`() {
        assertTrue(TermuxCommandBridge.validate("  ", "/data/data/com.termux/files/home").isFailure)
    }

    @Test
    fun `oversized command is rejected`() {
        assertTrue(TermuxCommandBridge.validate("x".repeat(8193), "/data/data/com.termux/files/home").isFailure)
    }

    @Test
    fun `line breaks in working directory are rejected`() {
        assertTrue(TermuxCommandBridge.validate("git status", "/data/data/com.termux/files/home\nunsafe").isFailure)
    }
}
