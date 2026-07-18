package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubContributionDay
import com.sayanthrock.githubrock.core.model.GitHubOrganization
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubSocialAccount
import com.sayanthrock.githubrock.core.network.GitHubGraphQlApi
import com.sayanthrock.githubrock.core.network.GraphQlRequest
import com.sayanthrock.githubrock.core.security.TokenStore
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubProfileDetailsResolver @Inject constructor(
    private val graphQlApi: GitHubGraphQlApi,
    private val tokenStore: TokenStore
) {
    suspend fun resolve(login: String): GitHubProfileDetails? {
        if (tokenStore.read()?.accessToken.isNullOrBlank() || !LOGIN_PATTERN.matches(login)) return null
        val response = graphQlApi.query(GraphQlRequest(profileQuery(login)))
        return response.data?.let(GitHubProfileDetailsParser::parse)
    }

    private fun profileQuery(login: String) = """
        query MobileProfile {
          user(login: "$login") {
            pronouns
            isBountyHunter
            isCampusExpert
            isDeveloperProgramMember
            isEmployee
            isGitHubStar
            isHireable
            contributionsCollection {
              contributionCalendar {
                totalContributions
                weeks {
                  contributionDays { date contributionCount contributionLevel }
                }
              }
            }
            organizations(first: 12) {
              totalCount
              nodes { login name avatarUrl url }
            }
            socialAccounts(first: 12) {
              nodes { displayName provider url }
            }
          }
        }
    """.trimIndent()

    private companion object {
        val LOGIN_PATTERN = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?")
    }
}

internal object GitHubProfileDetailsParser {
    fun parse(data: JsonObject): GitHubProfileDetails? {
        val user = data.objectValue("user") ?: return null
        val calendar = user.objectValue("contributionsCollection")?.objectValue("contributionCalendar")
        val organizations = user.objectValue("organizations")
        val socialAccounts = user.objectValue("socialAccounts")
        return GitHubProfileDetails(
            pronouns = user.stringValue("pronouns")?.takeIf(String::isNotBlank),
            contributionsLastYear = calendar?.intValue("totalContributions"),
            contributionDays = calendar?.arrayValue("weeks").orEmpty().flatMap { week ->
                (week as? JsonObject)?.arrayValue("contributionDays").orEmpty().mapNotNull { day ->
                    val item = day as? JsonObject ?: return@mapNotNull null
                    GitHubContributionDay(
                        date = item.stringValue("date") ?: return@mapNotNull null,
                        count = item.intValue("contributionCount") ?: 0,
                        level = item.stringValue("contributionLevel").orEmpty()
                    )
                }
            },
            organizations = organizations?.arrayValue("nodes").orEmpty().mapNotNull { node ->
                val item = node as? JsonObject ?: return@mapNotNull null
                GitHubOrganization(
                    login = item.stringValue("login") ?: return@mapNotNull null,
                    name = item.stringValue("name"),
                    avatarUrl = item.stringValue("avatarUrl").orEmpty(),
                    url = item.stringValue("url").orEmpty()
                )
            },
            organizationCount = organizations?.intValue("totalCount") ?: 0,
            socialAccounts = socialAccounts?.arrayValue("nodes").orEmpty().mapNotNull { node ->
                val item = node as? JsonObject ?: return@mapNotNull null
                GitHubSocialAccount(
                    displayName = item.stringValue("displayName") ?: return@mapNotNull null,
                    provider = item.stringValue("provider").orEmpty(),
                    url = item.stringValue("url") ?: return@mapNotNull null
                )
            },
            highlights = buildList {
                if (user.booleanValue("isGitHubStar") == true) add("GitHub Star")
                if (user.booleanValue("isDeveloperProgramMember") == true) add("Developer Program")
                if (user.booleanValue("isCampusExpert") == true) add("Campus Expert")
                if (user.booleanValue("isBountyHunter") == true) add("Security Bug Bounty")
                if (user.booleanValue("isEmployee") == true) add("GitHub staff")
                if (user.booleanValue("isHireable") == true) add("Available for hire")
            }
        )
    }

    private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.arrayValue(key: String): JsonArray? = this[key] as? JsonArray
    private fun JsonObject.stringValue(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.intValue(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.booleanValue(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
}
