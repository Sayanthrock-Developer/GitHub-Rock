package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubIssue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubIssueMarkerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun pullRequestMarkerIsDecodedWithoutInspectingIssueBody() {
        val pull = json.decodeFromString<GitHubIssue>(
            """
            {
              "id": 1,
              "number": 7,
              "title": "Improve login",
              "state": "open",
              "body": "This body does not mention pull requests.",
              "user": { "login": "octocat" },
              "pull_request": { "html_url": "https://github.com/example/project/pull/7" }
            }
            """.trimIndent()
        )
        val issue = json.decodeFromString<GitHubIssue>(
            """
            {
              "id": 2,
              "number": 8,
              "title": "Document pull request workflow",
              "state": "open",
              "body": "This real issue mentions pull request several times.",
              "user": { "login": "octocat" }
            }
            """.trimIndent()
        )

        assertNotNull(pull.pullRequest)
        assertNull(issue.pullRequest)
        assertEquals(listOf(issue), listOf(pull, issue).filter { it.pullRequest == null })
    }
}
