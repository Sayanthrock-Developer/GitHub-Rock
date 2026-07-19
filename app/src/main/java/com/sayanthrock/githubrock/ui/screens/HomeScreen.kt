package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun HomeScreen(
    profile: GitHubUser?,
    rateLimit: RateLimit?,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    onOpenBuilds: () -> Unit,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val running = remember(runs) {
        runs.count { it.displayState() in setOf(WorkflowDisplayState.Running, WorkflowDisplayState.Queued) }
    }
    val success = remember(runs) { runs.count { it.displayState() == WorkflowDisplayState.Success } }
    val failed = remember(runs) { runs.count { it.displayState() == WorkflowDisplayState.Failed } }
    var showAllRuns by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing, onRefresh, Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = StandardScreenPadding,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Home",
                    subtitle = "Your GitHub workspace, clearly organized"
                )
            }
            item { WorkspaceStatusCard(profile, rateLimit) }
            if (isLoading) item { LoadingWorkspaceCard() }
            item { WorkflowActivityCard(runs.size, running, success, failed) }

            item {
                StandardSectionHeader("Overview")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { DashboardMetric("Repositories", repositories.size.toString(), Icons.Default.Folder) }
                    item { DashboardMetric("Running", running.toString(), Icons.Default.CloudQueue) }
                    item { DashboardMetric("Successful", success.toString(), Icons.Default.CheckCircle, success = true) }
                    item { DashboardMetric("Failed", failed.toString(), Icons.Default.ErrorOutline, warning = failed > 0) }
                }
            }

            item {
                StandardSectionHeader("Quick actions")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onOpenBuilds, Modifier.weight(1f).height(54.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Build APK", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { repositories.firstOrNull()?.let(onOpenRepo) },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = repositories.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open repository")
                    }
                }
            }

            if (runs.isNotEmpty()) {
                item { StandardSectionHeader("Workflow activity", "${runs.size} recent") }
                items(if (showAllRuns) runs else runs.take(3), key = { it.id }) { run ->
                    WorkflowSummaryCard(run)
                }
                if (runs.size > 3) {
                    item {
                        TextButton({ showAllRuns = !showAllRuns }, Modifier.fillMaxWidth()) {
                            Icon(
                                if (showAllRuns) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (showAllRuns) "Show fewer workflows" else "See all workflows")
                        }
                    }
                }
            }

            item {
                StandardSectionHeader(
                    "Recently updated repositories",
                    when {
                        isLoading -> "Loading"
                        repositories.isEmpty() -> "No repositories"
                        else -> "${repositories.size} available"
                    }
                )
            }
            if (!isLoading && repositories.isEmpty()) {
                item {
                    GlassCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null)
                            Text("No repositories to show. Pull down to refresh.")
                        }
                    }
                }
            }
            items(repositories.take(6), key = { it.id }) { repository ->
                RepositoryCard(repository) { onOpenRepo(repository) }
            }
        }
    }
}

@Composable
private fun WorkspaceStatusCard(profile: GitHubUser?, rateLimit: RateLimit?) {
    val accountReady = profile != null
    val apiReady = rateLimit != null && rateLimit.limit > 0

    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Workspace status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Important connection details without technical scoring.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            WorkspaceStatusRow(
                icon = if (accountReady) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                title = "GitHub account",
                detail = profile?.login?.let { "Connected as @$it" } ?: "Sign in to load your account",
                status = if (accountReady) "Connected" else "Not connected",
                healthy = accountReady
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WorkspaceStatusRow(
                icon = if (apiReady) Icons.Default.CloudQueue else Icons.Default.ErrorOutline,
                title = "GitHub API",
                detail = rateLimit?.let { "${it.remaining} of ${it.limit} requests available" }
                    ?: "Availability has not been loaded",
                status = if (apiReady) "Available" else "Unavailable",
                healthy = apiReady
            )
        }
    }
}

@Composable
private fun WorkspaceStatusRow(
    icon: ImageVector,
    title: String,
    detail: String,
    status: String,
    healthy: Boolean
) {
    val accent = if (healthy) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = accent.copy(alpha = .12f)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(22.dp), tint = accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        StatusPill(status, accent)
    }
}

@Composable
private fun WorkflowActivityCard(total: Int, running: Int, success: Int, failed: Int) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Build activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Recent workflow results at a glance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Text(
                        if (total == 1) "1 run" else "$total runs",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkflowStateTile("Running", running, WorkflowDisplayState.Running, Modifier.weight(1f))
                WorkflowStateTile("Success", success, WorkflowDisplayState.Success, Modifier.weight(1f))
                WorkflowStateTile("Failed", failed, WorkflowDisplayState.Failed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorkflowStateTile(
    label: String,
    value: Int,
    state: WorkflowDisplayState,
    modifier: Modifier
) {
    val accent = workflowStateColor(state)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                color = accent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun LoadingWorkspaceCard() {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Loading your GitHub workspace" },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Column {
                Text("Loading your GitHub workspace…", fontWeight = FontWeight.SemiBold)
                Text(
                    "Fetching account, repositories, workflow status, and activity time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    icon: ImageVector,
    warning: Boolean = false,
    success: Boolean = false
) {
    val accent = when {
        warning -> MaterialTheme.colorScheme.error
        success -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.width(132.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .50f),
        border = BorderStroke(1.dp, accent.copy(alpha = .20f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = accent)
            Text(
                value,
                color = accent,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkflowSummaryCard(run: WorkflowRun) {
    val state = run.displayState()
    val accent = workflowStateColor(state)
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = accent.copy(alpha = .12f)) {
                    Icon(
                        workflowStateIcon(state),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                        tint = accent
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        run.displayTitle.ifBlank { run.name ?: "Workflow" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOf(run.event, run.headBranch.orEmpty())
                            .filter(String::isNotBlank)
                            .joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(state.name, accent)
            }
            HorizontalDivider(color = accent.copy(alpha = .28f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatGitHubTime(run.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Run #${run.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, accent: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .30f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun workflowStateColor(state: WorkflowDisplayState): Color = when (state) {
    WorkflowDisplayState.Success -> MaterialTheme.colorScheme.tertiary
    WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error
    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

private fun workflowStateIcon(state: WorkflowDisplayState): ImageVector = when (state) {
    WorkflowDisplayState.Success -> Icons.Default.CheckCircle
    WorkflowDisplayState.Failed, WorkflowDisplayState.Cancelled -> Icons.Default.ErrorOutline
    else -> Icons.Default.CloudQueue
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RepositoryCard(repo: GitHubRepositoryModel, onClick: () -> Unit) {
    val showImages = LocalRemoteImagesEnabled.current
    GlassCard(Modifier.fillMaxWidth(), PaddingValues(16.dp), onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (showImages && repo.owner.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = repo.owner.avatarUrl,
                            contentDescription = "${repo.owner.login} avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                repo.owner.login.take(2).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(repo.owner.login, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open ${repo.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                repo.description ?: "No repository description.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepositoryMetaChip(Icons.Default.Star, "Stars", compactCount(repo.stars))
                RepositoryMetaChip(Icons.Default.CallSplit, "Forks", compactCount(repo.forks))
                RepositoryMetaChip(Icons.Default.ErrorOutline, "Open issues", compactCount(repo.openIssues))
                RepositoryMetaChip(Icons.Default.Code, "Language", repo.language ?: "Repository", true)
                repo.topics.firstOrNull()?.takeIf(String::isNotBlank)?.let { topic ->
                    RepositoryMetaChip(Icons.Default.Tag, "Topic", topic)
                }
            }
            if (repo.updatedAt.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Updated ${formatGitHubTime(repo.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryMetaChip(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Boolean = false
) {
    val foreground = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.semantics { contentDescription = "$label: $value" },
        shape = MaterialTheme.shapes.large,
        color = if (accent) {
            MaterialTheme.colorScheme.primary.copy(alpha = .10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)
        },
        border = BorderStroke(
            1.dp,
            if (accent) MaterialTheme.colorScheme.primary.copy(alpha = .28f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = foreground)
            Text(value, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}

private val githubDateTimeFormatter = DateTimeFormatter
    .ofPattern("dd MMM yyyy • hh:mm:ss a", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatGitHubTime(value: String): String {
    if (value.isBlank()) return "Time unavailable"
    return runCatching { githubDateTimeFormatter.format(Instant.parse(value)) }.getOrDefault(value)
}
