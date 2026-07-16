package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubUser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubUserProfileTest {
    @Test fun profileDecodesRepositoryAndSocialCounts() {
        val user = Json { ignoreUnknownKeys = true }.decodeFromString<GitHubUser>(
            """
            {
              "login": "SayanthRock",
              "id": 202829406,
              "avatar_url": "https://avatars.githubusercontent.com/u/202829406",
              "public_repos": 24,
              "followers": 120,
              "following": 73
            }
            """.trimIndent()
        )

        assertEquals("SayanthRock", user.login)
        assertEquals(24, user.publicRepos)
        assertEquals(120, user.followers)
        assertEquals(73, user.following)
    }
}
