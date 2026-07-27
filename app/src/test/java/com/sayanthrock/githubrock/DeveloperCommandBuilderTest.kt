package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.DeveloperCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperCommandBuilderTest {
    @Test
    fun `builds checkout command for a validated repository`() {
        assertEquals(
            "gh pr checkout 91 --repo Sayanthrock-Developer/GitHub-Rock",
            DeveloperCommandBuilder.checkout("Sayanthrock-Developer", "GitHub-Rock", "91")?.value
        )
    }

    @Test
    fun `rejects shell metacharacters in repository fields`() {
        assertNull(DeveloperCommandBuilder.clone("owner; rm -rf ~", "repo"))
        assertNull(DeveloperCommandBuilder.checkout("owner", "repo && echo bad", "91"))
        assertNull(DeveloperCommandBuilder.checkout("owner", "repo", "0"))
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

        assertNotNull(session)
        assertNotNull(persistent)

        assertTrue(session!!.value.contains("read -rsp"))
        assertTrue(persistent!!.value.contains("read -rsp"))
        assertTrue(persistent.value.contains("umask 077"))
        assertTrue(persistent.value.contains("chmod 600"))
        assertFalse(session.value.contains("PASTE_KEY_HERE"))
        assertFalse(persistent.value.contains("PASTE_KEY_HERE"))
    }

    @Test
    fun `Termux bridge command only enables the documented external app setting`() {
        val command = DeveloperCommandBuilder.ENABLE_TERMUX_BRIDGE.value
        assertTrue(command.contains("allow-external-apps=true"))
        assertTrue(command.contains("termux-reload-settings"))
        assertFalse(command.contains("chmod 777"))
    }
}