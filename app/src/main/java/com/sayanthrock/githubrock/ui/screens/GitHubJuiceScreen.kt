package com.sayanthrock.githubrock.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.buildAnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubJuiceScreen(
    viewModel: GitHubJuiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Juice") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                JuiceOverviewSection(
                    dailySummary = state.dailySummary,
                    healthScore = state.repositoryHealthScore,
                    commitActivity = state.commitActivity
                )
            }
            item {
                JuiceStatusSection(
                    openIssues = state.openIssuesSummary,
                    pullRequests = state.pullRequestStatus,
                    workflowStatus = state.workflowStatus,
                    recentReleases = state.recentReleases
                )
            }
            item {
                JuiceTrendingSection(
                    trendingRepos = state.trendingRepositories,
                    trendingDevs = state.trendingDevelopers
                )
            }
            item {
                JuiceGrowthSection(
                    repoGrowth = state.repositoryGrowth,
                    starGrowth = state.starGrowth,
                    forkGrowth = state.forkGrowth
                )
            }
            item {
                JuiceContributorsSection(
                    topContributors = state.topContributors,
                    recentContributors = state.recentContributors,
                    commitStreak = state.commitStreak
                )
            }
            item {
                JuiceCodeStatsSection(
                    languageBreakdown = state.languageBreakdown,
                    repoSize = state.repositorySize,
                    license = state.licenseDetection,
                    readmeStatus = state.readmeStatus,
                    latestTags = state.latestTags,
                    securityAdvisories = state.securityAdvisories,
                    codeFreq = state.codeFrequency,
                    timeline = state.activityTimeline
                )
            }
            item {
                JuiceListsSection(
                    leaderboard = state.contributorLeaderboard,
                    updatedRepos = state.recentlyUpdatedRepositories,
                    starredRepos = state.recentlyStarredRepositories,
                    savedRepos = state.savedRepositories
                )
            }
        }
    }
}

@Composable
fun JuiceOverviewSection(dailySummary: String, healthScore: Int, commitActivity: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Overview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Daily Summary: $dailySummary", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Health Score: $healthScore / 100", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Commit Activity: $commitActivity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun JuiceStatusSection(openIssues: String, pullRequests: String, workflowStatus: String, recentReleases: List<String>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Open Issues: $openIssues", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Pull Requests: $pullRequests", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Workflows: $workflowStatus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Recent Releases: ${recentReleases.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun JuiceTrendingSection(trendingRepos: List<GitHubRepositoryModel>, trendingDevs: List<String>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Trending", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Trending Repositories:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (trendingRepos.isEmpty()) {
                Text(text = "No trending repositories found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(trendingRepos) { repo ->
                        ElevatedCard(modifier = Modifier.width(160.dp).padding(4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = repo.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "⭐ ${repo.stars}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Trending Developers:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = if (trendingDevs.isEmpty()) "None currently" else trendingDevs.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun JuiceGrowthSection(repoGrowth: String, starGrowth: String, forkGrowth: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Growth", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Repository Growth: $repoGrowth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Star Growth: $starGrowth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Fork Growth: $forkGrowth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun JuiceContributorsSection(topContributors: List<String>, recentContributors: List<String>, commitStreak: Int) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Contributors", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Commit Streak: $commitStreak days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Top Contributors:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = if (topContributors.isEmpty()) "None currently" else topContributors.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Recent Contributors:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = if (recentContributors.isEmpty()) "None currently" else recentContributors.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun JuiceCodeStatsSection(
    languageBreakdown: Map<String, Double>,
    repoSize: String,
    license: String,
    readmeStatus: String,
    latestTags: List<String>,
    securityAdvisories: List<String>,
    codeFreq: String,
    timeline: List<String>
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Code & Repository Stats", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Languages:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (languageBreakdown.isEmpty()) {
                Text(text = "No language data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(text = languageBreakdown.entries.joinToString(", ") { "${it.key}: ${it.value}%" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Size: $repoSize", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "License: $license", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "README: $readmeStatus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "Code Frequency: $codeFreq", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Latest Tags:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = if (latestTags.isEmpty()) "None" else latestTags.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Security Advisories:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Text(text = if (securityAdvisories.isEmpty()) "None found" else securityAdvisories.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Activity Timeline:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (timeline.isEmpty()) {
                Text(text = "No recent activity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                timeline.forEach { event ->
                    Text(text = "- $event", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun JuiceListsSection(
    leaderboard: List<String>,
    updatedRepos: List<GitHubRepositoryModel>,
    starredRepos: List<GitHubRepositoryModel>,
    savedRepos: List<GitHubRepositoryModel>
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Lists & Actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Contributor Leaderboard:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = if (leaderboard.isEmpty()) "None currently" else leaderboard.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Recently Updated Repositories:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (updatedRepos.isEmpty()) {
                Text(text = "No recent updates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(updatedRepos) { repo ->
                        RepositoryCardWithActions(repo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Recently Starred Repositories:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (starredRepos.isEmpty()) {
                Text(text = "No recent stars.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(starredRepos) { repo ->
                        RepositoryCardWithActions(repo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Saved Repositories:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (savedRepos.isEmpty()) {
                Text(text = "No saved repositories.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(savedRepos) { repo ->
                        RepositoryCardWithActions(repo)
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryCardWithActions(repo: GitHubRepositoryModel) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    ElevatedCard(modifier = Modifier.width(240.dp).padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = repo.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            Text(text = repo.description ?: "No description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.height(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { /* ViewModel Star */ }, contentPadding = PaddingValues(4.dp)) {
                    Text("Star", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { /* ViewModel Watch */ }, contentPadding = PaddingValues(4.dp)) {
                    Text("Watch", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { /* ViewModel Fork */ }, contentPadding = PaddingValues(4.dp)) {
                    Text("Fork", style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    coroutineScope.launch { clipboardManager.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("Clone URL", repo.cloneUrl))) }
                }, contentPadding = PaddingValues(4.dp)) {
                    Text("Clone", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = {
                    if (repo.htmlUrl.isNotEmpty()) uriHandler.openUri(repo.htmlUrl)
                }, contentPadding = PaddingValues(4.dp)) {
                    Text("Browser", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
