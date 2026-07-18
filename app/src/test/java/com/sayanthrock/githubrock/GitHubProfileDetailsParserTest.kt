package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.repository.GitHubProfileDetailsParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubProfileDetailsParserTest {
    @Test fun graphQlProfileDetailsExposeContributionOrganizationsPronounsAndOrcid() {
        val data = Json.parseToJsonElement(
            """
            {"user":{
              "pronouns":"he/him",
              "isGitHubStar":true,
              "isDeveloperProgramMember":true,
              "isCampusExpert":false,
              "isBountyHunter":false,
              "isEmployee":false,
              "isHireable":true,
              "contributionsCollection":{"contributionCalendar":{"totalContributions":321,"weeks":[{"contributionDays":[{"date":"2026-07-18","contributionCount":4,"contributionLevel":"SECOND_QUARTILE"}]}]}},
              "organizations":{"totalCount":1,"nodes":[{"login":"open-source","name":"Open Source","avatarUrl":"https://example/avatar","url":"https://github.com/open-source"}]},
              "socialAccounts":{"nodes":[{"displayName":"0000-0002-1825-0097","provider":"GENERIC","url":"https://orcid.org/0000-0002-1825-0097"}]}
            }}
            """.trimIndent()
        ).jsonObject

        val details = requireNotNull(GitHubProfileDetailsParser.parse(data))

        assertEquals("he/him", details.pronouns)
        assertEquals(321, details.contributionsLastYear)
        assertEquals(4, details.contributionDays.single().count)
        assertEquals("open-source", details.organizations.single().login)
        assertEquals("0000-0002-1825-0097", details.orcid?.displayName)
        assertTrue("GitHub Star" in details.highlights)
        assertTrue("Available for hire" in details.highlights)
    }
}
