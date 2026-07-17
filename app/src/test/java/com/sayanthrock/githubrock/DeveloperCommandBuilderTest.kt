package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.DeveloperCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperCommandBuilderTest {
    @Test
    fun `builds checkout command for a validated repository`() {
        assertEquals(
            "gh pr checkout 91 --repo Sayanthrock-Developer/GitHub-Rock",
            DeveloperCommandBuilder.checkout("Sayanthrock-Developer", "GitHub-Rock", "91")
        )
    }

    @Test
    fun `rejects shell metacharacters in repository fields`() {
        assertEquals("", DeveloperCommandBuilder.clone("owner; rm -rf ~", "repo"))
        assertEquals("", DeveloperCommandBuilder.checkout("owner", "repo && echo bad", "91"))
        assertEquals("", DeveloperCommandBuilder.checkout("owner", "repo", "0"))
    }

    @Test
    fun `normalizes and validates API key environment variables`() {
        assertEquals("OPENAI_API_KEY", DeveloperCommandBuilder.environmentVariable("openai_api_key"))
        assertEquals(null, DeveloperCommandBuilder.environmentVariable("API-KEY;echo"))
    }

    @Test
    fun `API key commands never contain a secret value and enforce mode 600`() {
        val session = DeveloperCommandBuilder.sessionApiKey("OPENAI_API_KEY")
        val persistent = DeveloperCommandBuilder.persistentApiKey("OPENAI_API_KEY")

        assertTrue(session.contains("read -rsp"))
        assertTrue(persistent.contains("read -rsp"))
        assertTrue(persistent.contains("umask 077"))
        assertTrue(persistent.contains("chmod 600"))
        assertFalse(session.contains("PASTE_KEY_HERE"))
        assertFalse(persistent.contains("PASTE_KEY_HERE"))
    }

    @Test
    fun `Termux bridge command only enables the documented external app setting`() {
        val command = DeveloperCommandBuilder.ENABLE_TERMUX_BRIDGE
        assertTrue(command.contains("allow-external-apps=true"))
        assertTrue(command.contains("termux-reload-settings"))
        assertFalse(command.contains("chmod 777"))
    }
}