package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileExportFormatterTest {
    @Test
    fun `profile export contains identity and counts`() {
        val profile = GitHubUser(
            login = "SayanthRock",
            id = 202829406,
            avatarUrl = "https://avatars.example/profile.png",
            name = "Sayanth Rock",
            bio = "Android developer",
            location = "India",
            blog = "https://sayanthrock.com",
            publicRepos = 24,
            followers = 120,
            following = 48
        )

        val json = ProfileExportFormatter.toJson(profile)

        assertTrue(json.contains("\"login\": \"SayanthRock\""))
        assertTrue(json.contains("\"public_repos\": 24"))
        assertTrue(json.contains("\"followers\": 120"))
        assertEquals("SayanthRock-profile.json", ProfileExportFormatter.fileName(profile))
    }

    @Test
    fun `profile filename is safe`() {
        val profile = GitHubUser(login = " rock user / test ", id = 1)
        assertEquals("rock-user-test-profile.json", ProfileExportFormatter.fileName(profile))
    }
}
