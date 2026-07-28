package com.sayanthrock.githubrock.core.util

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

    @Test
    fun `repository validates owner and name`() {
        assertEquals("owner/repo", DeveloperCommandBuilder.repository("owner", "repo"))
        assertNull(DeveloperCommandBuilder.repository("owner_with_underscore", "repo"))
        assertNull(DeveloperCommandBuilder.repository("owner", "repo name"))
        assertNull(DeveloperCommandBuilder.repository("owner", ""))
    }

    @Test
    fun `pullRequest validates integer`() {
        assertEquals(123, DeveloperCommandBuilder.pullRequest("123"))
        assertNull(DeveloperCommandBuilder.pullRequest("-1"))
        assertNull(DeveloperCommandBuilder.pullRequest("0"))
        assertNull(DeveloperCommandBuilder.pullRequest("abc"))
        assertNull(DeveloperCommandBuilder.pullRequest(""))
    }

    @Test
    fun `viewPullRequest returns correct gh command`() {
        assertEquals(
            "gh pr view 123 --repo owner/repo --web",
            DeveloperCommandBuilder.viewPullRequest("owner", "repo", "123")?.value
        )
        assertNull(DeveloperCommandBuilder.viewPullRequest("owner", "repo", "abc"))
        assertNull(DeveloperCommandBuilder.viewPullRequest("owner_with_underscore", "repo", "123"))
    }

    @Test
    fun `pullRequestApi returns correct gh api command`() {
        assertEquals(
            "gh api repos/owner/repo/pulls/123",
            DeveloperCommandBuilder.pullRequestApi("owner", "repo", "123")?.value
        )
        assertNull(DeveloperCommandBuilder.pullRequestApi("owner", "repo", "abc"))
    }

    @Test
    fun `fullGitHubSetup combines setup commands`() {
        val command = DeveloperCommandBuilder.fullGitHubSetup().value
        assertTrue(command.contains(DeveloperCommandBuilder.INSTALL_TOOLCHAIN.value))
        assertTrue(command.contains(DeveloperCommandBuilder.GITHUB_LOGIN.value))
        assertTrue(command.contains(DeveloperCommandBuilder.GITHUB_SETUP_GIT.value))
    }

    @Test
    fun `loadPersistentApiKey creates correct source command`() {
        assertEquals(
            "source \"\$HOME/.config/github-rock/openai-api-key.env\"",
            DeveloperCommandBuilder.loadPersistentApiKey("OPENAI_API_KEY")?.value
        )
        assertNull(DeveloperCommandBuilder.loadPersistentApiKey("INVALID-KEY!"))
    }
}
