package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RateLimit
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun HomeScreen(
    mode: AppMode,
    profile: GitHubUser?,
    rateLimit: RateLimit?,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    onOpenBuilds: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Developer control centre", style = MaterialTheme.typography.headlineSmall)
            Text(
                when (mode) { AppMode.Demo -> "DEMO • isolated sample workspace"; AppMode.Guest -> "GUEST • public repositories"; AppMode.Connected -> "CONNECTED • ${profile?.login.orEmpty()}" },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Welcome${profile?.name?.let { ", $it" }.orEmpty()}", style = MaterialTheme.typography.titleLarge)
                    Text(profile?.bio ?: "Browse public repositories or connect GitHub for private data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    rateLimit?.let {
                        LinearProgressIndicator(
                            progress = { if (it.limit == 0) 0f else it.remaining.toFloat() / it.limit },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("GitHub API: ${it.remaining} of ${it.limit} requests remaining", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Text("Quick actions", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpenBuilds, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Build, null); Spacer(Modifier.width(6.dp)); Text("Build APK") }
                FilledTonalButton(onClick = { repositories.firstOrNull()?.let(onOpenRepo) }, enabled = repositories.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Open repo")
                }
            }
        }
        if (runs.isNotEmpty()) {
            item { Text("Recent workflows", style = MaterialTheme.typography.titleMedium) }
            items(runs.take(3), key = { it.id }) { run ->
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(run.displayTitle.ifBlank { run.name ?: "Workflow" }, fontWeight = FontWeight.SemiBold)
                            Text(run.headBranch.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(run.displayState().name, color = workflowColor(run))
                    }
                }
            }
        }
        item { Text("Recently updated repositories", style = MaterialTheme.typography.titleMedium) }
        if (repositories.isEmpty()) item {
            GlassCard { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.ErrorOutline, null); Text("No repositories to show.") } }
        }
        items(repositories.take(6), key = { it.id }) { repo ->
            RepositoryCard(repo = repo, onClick = { onOpenRepo(repo) })
        }
    }
}

@Composable
private fun workflowColor(run: WorkflowRun) = when (run.displayState().name) {
    "Success" -> MaterialTheme.colorScheme.tertiary
    "Failed" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun RepositoryCard(repo: GitHubRepositoryModel, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Folder, null); Spacer(Modifier.width(8.dp)); Text(repo.fullName, fontWeight = FontWeight.Bold)
            }
            Text(repo.description ?: "No description", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(repo.language ?: "—", style = MaterialTheme.typography.bodyMedium)
                Text("★ ${repo.stars}", style = MaterialTheme.typography.bodyMedium)
                Text("⑂ ${repo.forks}", style = MaterialTheme.typography.bodyMedium)
                if (repo.private) Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)) {
                    Text("Private", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
