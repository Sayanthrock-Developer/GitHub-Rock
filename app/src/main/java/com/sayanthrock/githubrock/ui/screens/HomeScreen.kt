package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RateLimit
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
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
    val activeWorkflows = remember(runs) { runs.count { it.status != "completed" } }
    val failedWorkflows = remember(runs) {
        runs.count { it.displayState() == WorkflowDisplayState.Failed }
    }
    val workflowRepositoryLabel = repositories.firstOrNull()?.name

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHero(mode = mode, profile = profile, rateLimit = rateLimit) }
        item {
            Text("Loaded workspace snapshot", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { DashboardMetric("Loaded repos", repositories.size.toString(), Icons.Default.Folder) }
                item { DashboardMetric("Followers", (profile?.followers ?: 0).toString(), Icons.Default.People) }
                item { DashboardMetric(workflowRepositoryLabel?.let { "Active · $it" } ?: "Active workflows", activeWorkflows.toString(), Icons.Default.CloudQueue) }
                item {
                    DashboardMetric(
                        workflowRepositoryLabel?.let { "Failed · $it" } ?: "Failed workflows",
                        failedWorkflows.toString(),
                        Icons.Default.ErrorOutline,
                        isWarning = failedWorkflows > 0
                    )
                }
            }
        }
        item {
            Text("Quick actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenBuilds, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Build APK")
                }
                FilledTonalButton(
                    onClick = { repositories.firstOrNull()?.let(onOpenRepo) },
                    enabled = repositories.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Open repo")
                }
            }
        }
        if (runs.isNotEmpty()) {
            item { SectionHeading("Workflow activity", "${runs.size} recent") }
            items(runs.take(4), key = { it.id }) { run -> WorkflowSummaryCard(run) }
        }
        item {
            SectionHeading(
                "Recently updated repositories",
                if (repositories.isEmpty()) "No repositories" else "${repositories.size} available"
            )
        }
        if (repositories.isEmpty()) {
            item {
                GlassCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null)
                        Text("No repositories to show.")
                    }
                }
            }
        }
        items(repositories.take(6), key = { it.id }) { repo ->
            RepositoryCard(repo = repo, onClick = { onOpenRepo(repo) })
        }
    }
}

@Composable
private fun DashboardHero(mode: AppMode, profile: GitHubUser?, rateLimit: RateLimit?) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "Connected GitHub profile avatar",
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            Text(
                                profile?.login?.take(2)?.uppercase() ?: "GR",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    ModeBadge(mode)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        profile?.name ?: profile?.login ?: "Developer control centre",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: "Browse public repositories securely",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            profile?.bio?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            rateLimit?.let {
                val progress = if (it.limit == 0) 0f else it.remaining.toFloat() / it.limit
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GitHub API health", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${it.remaining} / ${it.limit}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ModeBadge(mode: AppMode) {
    val label = when (mode) {
        AppMode.Connected -> "CONNECTED"
        AppMode.Guest -> "GUEST · PUBLIC ONLY"
        AppMode.Demo -> "DEMO · ISOLATED DATA"
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, icon: ImageVector, isWarning: Boolean = false) {
    val accent = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.width(138.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(title: String, supporting: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WorkflowSummaryCard(run: WorkflowRun) {
    val state = run.displayState()
    val color = workflowColor(run)
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = color.copy(alpha = .13f)) {
                Icon(
                    if (run.conclusion == "failure") Icons.Default.ErrorOutline else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    run.displayTitle.ifBlank { run.name ?: "Workflow" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    run.headBranch.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(state.name, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun workflowColor(run: WorkflowRun) = when (run.displayState()) {
    WorkflowDisplayState.Success -> MaterialTheme.colorScheme.tertiary
    WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error
    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun RepositoryCard(repo: GitHubRepositoryModel, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(repo.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                repo.description ?: "No description",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(repo.language ?: "—", style = MaterialTheme.typography.bodyMedium)
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Stars",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(repo.stars.toString(), style = MaterialTheme.typography.bodyMedium)
                Text("Forks ${repo.forks}", style = MaterialTheme.typography.bodyMedium)
                if (repo.private) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)) {
                        Text(
                            "Private",
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

