package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubRepositoryTemplateTest {
    @Test fun repositoryDecodesTemplateFlag() {
        val repository = Json { ignoreUnknownKeys = true }.decodeFromString<GitHubRepositoryModel>(
            """
            {
              "id": 1295411691,
              "name": "Rock-Wedding",
              "full_name": "SayanthRock/Rock-Wedding",
              "owner": {
                "login": "SayanthRock",
                "avatar_url": "https://avatars.githubusercontent.com/u/202829406?v=4"
              },
              "is_template": true,
              "private": false,
              "fork": false
            }
            """.trimIndent()
        )

        assertTrue(repository.isTemplate)
    }
}
