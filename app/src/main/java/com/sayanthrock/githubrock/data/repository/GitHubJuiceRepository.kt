package com.sayanthrock.githubrock.data.repository

import android.content.Context
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.network.GitHubGraphQlApi
import com.sayanthrock.githubrock.core.network.GraphQlRequest
import com.sayanthrock.githubrock.core.network.GitHubProfileApi
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.ui.screens.GitHubJuiceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.intOrNull
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubJuiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApi: GitHubRestApi,
    private val gitHubGraphQlApi: GitHubGraphQlApi,
    private val gitHubProfileApi: GitHubProfileApi,
    private val json: Json
) {
    private val cacheFile = File(context.cacheDir, "github_juice_cache.json")

    suspend fun getCachedState(): GitHubJuiceState? = withContext(Dispatchers.IO) {
        try {
            if (cacheFile.exists()) {
                val contents = cacheFile.readText()
                return@withContext json.decodeFromString<GitHubJuiceState>(contents)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveCachedState(state: GitHubJuiceState) = withContext(Dispatchers.IO) {
        try {
            cacheFile.writeText(json.encodeToString(state))
        } catch (e: Exception) {
            // Ignore cache save errors
        }
    }

    suspend fun fetchJuiceState(): GitHubJuiceState = withContext(Dispatchers.IO) {
        val meDeferred = async { gitHubApi.me() }
        val reposDeferred = async { gitHubApi.repositories(perPage = 100) }
        val starredReposDeferred = async { gitHubApi.starredRepositories(perPage = 20) }

        val oneWeekAgo = LocalDate.now().minusDays(7).toString()
        val trendingQuery = "created:>$oneWeekAgo sort:stars-desc"
        val trendingReposDeferred = async { gitHubApi.searchRepositories(query = trendingQuery, perPage = 10) }

        val trendingDevQuery = "followers:>0 sort:joined-desc"
        val trendingDevsDeferred = async {
            try {
                gitHubApi.searchUsers(query = trendingDevQuery, perPage = 5).items.map { it.login }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val graphQlDeferred = async { fetchGraphQlStats() }

        val me = meDeferred.await()
        val repos = reposDeferred.await()
        val starredRepos = starredReposDeferred.await()
        val trendingReposResult = trendingReposDeferred.await()
        val trendingDevs = trendingDevsDeferred.await()
        val graphQlStats = graphQlDeferred.await()

        val topContributors = mutableListOf<String>()
        val recentContributors = mutableListOf<String>()

        if (repos.isNotEmpty()) {
            val topRepo = repos.maxByOrNull { it.stars }
            if (topRepo != null && !topRepo.fork) {
                try {
                    val contribs = gitHubApi.contributors(owner = topRepo.owner.login, repo = topRepo.name)
                    topContributors.addAll(contribs.take(5).map { it.login })
                } catch (e: Exception) { }
            }

            val recentRepo = repos.maxByOrNull { it.updatedAt }
            if (recentRepo != null && !recentRepo.fork) {
                 try {
                    val contribs = gitHubApi.contributors(owner = recentRepo.owner.login, repo = recentRepo.name)
                    recentContributors.addAll(contribs.take(5).map { it.login })
                } catch (e: Exception) { }
            }
        }

        var totalStars = 0
        var totalForks = 0
        var totalIssues = 0
        val languageCounts = mutableMapOf<String, Int>()

        repos.forEach { repo ->
            totalStars += repo.stars
            totalForks += repo.forks
            totalIssues += repo.openIssues

            if (repo.language != null) {
                languageCounts[repo.language] = languageCounts.getOrDefault(repo.language, 0) + 1
            }
        }

        val calculatedHealthScore = if (repos.isEmpty()) 100 else {
            val baseScore = 100
            val issuePenalty = (totalIssues * 2).coerceAtMost(50)
            val starBonus = (totalStars / 10).coerceAtMost(20)
            (baseScore - issuePenalty + starBonus).coerceIn(0, 100)
        }

        val totalRepos = repos.size
        val languageBreakdown = languageCounts.mapValues { (it.value.toDouble() / totalRepos) * 100.0 }
        val leaderboard = topContributors.take(5)

        GitHubJuiceState(
            isLoading = false,
            dailySummary = "Welcome, ${me.name ?: me.login}! You manage $totalRepos active repositories with $totalStars stars and $totalForks forks.",
            repositoryHealthScore = calculatedHealthScore,
            commitActivity = "Analyzed ${repos.size} repos for recent changes",
            openIssuesSummary = "Total open issues: $totalIssues across your repositories.",
            pullRequestStatus = "Tracking ${repos.count { r -> r.fork }} active forks.",
            workflowStatus = "All systems operational.",
            recentlyUpdatedRepositories = repos.take(10),
            recentlyStarredRepositories = starredRepos,
            savedRepositories = starredRepos.take(5),
            trendingRepositories = trendingReposResult.items,
            trendingDevelopers = trendingDevs,
            starGrowth = "Total Stars: $totalStars",
            forkGrowth = "Total Forks: $totalForks",
            repositoryGrowth = "Total Repos: $totalRepos",
            languageBreakdown = languageBreakdown,
            repositoryInsights = "Your most popular language is ${languageCounts.maxByOrNull { e -> e.value }?.key ?: "Unknown"}",
            topContributors = topContributors.distinct(),
            recentContributors = recentContributors.distinct(),
            contributorLeaderboard = leaderboard,

            // GraphQL integrated
            commitStreak = graphQlStats.streak,
            repositorySize = graphQlStats.repoSize,
            licenseDetection = graphQlStats.licenses,
            readmeStatus = graphQlStats.readmeStatus,
            latestTags = graphQlStats.tags,
            securityAdvisories = graphQlStats.securityAdvisories,
            codeFrequency = "Active timeline via GraphQL",
            activityTimeline = graphQlStats.tags.map { "Tag pushed: $it" }
        )
    }

    suspend fun fetchGraphQlStats(): GraphQlStats {
        val query = """
            query {
              viewer {
                contributionsCollection {
                  contributionCalendar {
                    weeks {
                      contributionDays {
                        contributionCount
                        date
                      }
                    }
                  }
                }
                repositories(first: 100, ownerAffiliations: OWNER, orderBy: {field: UPDATED_AT, direction: DESC}) {
                  nodes {
                    diskUsage
                    hasIssuesEnabled
                    licenseInfo {
                      name
                    }
                    refs(refPrefix: "refs/tags/", first: 1, orderBy: {field: TAG_COMMIT_DATE, direction: DESC}) {
                      nodes {
                        name
                      }
                    }
                    object(expression: "HEAD:README.md") {
                      ... on Blob {
                        byteSize
                      }
                    }
                    vulnerabilityAlerts(first: 1) {
                      totalCount
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return try {
            val response = gitHubGraphQlApi.query(GraphQlRequest(query))
            val viewer = response.data?.get("viewer")?.jsonObject

            // Streak
            val weeks = viewer?.get("contributionsCollection")?.jsonObject
                ?.get("contributionCalendar")?.jsonObject
                ?.get("weeks")?.jsonArray

            var currentStreak = 0
            if (weeks != null) {
                // Loop backwards to find current streak
                val allDays = weeks.flatMap { it.jsonObject["contributionDays"]!!.jsonArray }
                for (i in allDays.size - 1 downTo 0) {
                    val day = allDays[i].jsonObject
                    val count = day["contributionCount"]?.jsonPrimitive?.intOrNull ?: 0
                    if (count > 0) {
                        currentStreak++
                    } else if (i < allDays.size - 1) { // allow today to be 0
                        break
                    }
                }
            }

            // Repo stats
            val repos = viewer?.get("repositories")?.jsonObject?.get("nodes")?.jsonArray
            var totalSize = 0L
            val licenses = mutableSetOf<String>()
            val latestTags = mutableListOf<String>()
            var reposWithReadme = 0
            var totalReposChecked = 0
            var totalVulnerabilities = 0

            repos?.forEach { repoElement ->
                val repo = repoElement.jsonObject
                totalSize += repo["diskUsage"]?.jsonPrimitive?.long ?: 0L
                repo["licenseInfo"]?.jsonObject?.get("name")?.jsonPrimitive?.content?.let { licenses.add(it) }

                val tags = repo["refs"]?.jsonObject?.get("nodes")?.jsonArray
                if (!tags.isNullOrEmpty()) {
                    tags[0].jsonObject["name"]?.jsonPrimitive?.content?.let { latestTags.add(it) }
                }

                if (repo["object"]?.jsonObject != null) {
                    reposWithReadme++
                }

                totalVulnerabilities += repo["vulnerabilityAlerts"]?.jsonObject?.get("totalCount")?.jsonPrimitive?.intOrNull ?: 0

                totalReposChecked++
            }

            val readmeStatus = if (totalReposChecked > 0) "${(reposWithReadme * 100) / totalReposChecked}% coverage" else "N/A"
            val repoSizeMb = totalSize / 1024

            val secAdvisories = if (totalVulnerabilities > 0) listOf("$totalVulnerabilities unresolved alerts") else emptyList()

            GraphQlStats(
                streak = currentStreak,
                repoSize = if (repoSizeMb > 1000) "${repoSizeMb / 1024} GB" else "$repoSizeMb MB",
                licenses = if (licenses.isNotEmpty()) licenses.joinToString(", ") else "None detected",
                tags = latestTags.take(5),
                readmeStatus = readmeStatus,
                securityAdvisories = secAdvisories
            )
        } catch (e: Exception) {
            GraphQlStats()
        }
    }
}

data class GraphQlStats(
    val streak: Int = 0,
    val repoSize: String = "Unknown",
    val licenses: String = "Unknown",
    val tags: List<String> = emptyList(),
    val readmeStatus: String = "Unknown",
    val securityAdvisories: List<String> = emptyList()
)
