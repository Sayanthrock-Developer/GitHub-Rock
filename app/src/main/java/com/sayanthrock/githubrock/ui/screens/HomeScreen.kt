package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RateLimit
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.ui.AppMode
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
    mode: AppMode,
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
    val activeWorkflows = remember(runs) {
        runs.count { it.displayState() == WorkflowDisplayState.Running || it.displayState() == WorkflowDisplayState.Queued }
    }
    val successfulWorkflows = remember(runs) { runs.count { it.displayState() == WorkflowDisplayState.Success } }
    val failedWorkflows = remember(runs) { runs.count { it.displayState() == WorkflowDisplayState.Failed } }
    var showAllRuns by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = StandardScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Home",
                    subtitle = "A clean view of your GitHub workspace"
                )
            }

            item {
                GitHubRockHero(
                    mode = mode,
                    profile = profile,
                    rateLimit = rateLimit
                )
            }

            if (isLoading) {
                item { LoadingWorkspaceCard() }
            }

            item {
                WorkflowHealthCard(
                    total = runs.size,
                    running = activeWorkflows,
                    success = successfulWorkflows,
                    failed = failedWorkflows
                )
            }

            item {
                StandardSectionHeader("Workspace overview")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { DashboardMetric("Repositories", repositories.size.toString(), Icons.Default.Folder) }
                    item { DashboardMetric("Running", activeWorkflows.toString(), Icons.Default.CloudQueue) }
                    item { DashboardMetric("Success", successfulWorkflows.toString(), Icons.Default.CheckCircle, success = true) }
                    item { DashboardMetric("Failed", failedWorkflows.toString(), Icons.Default.ErrorOutline, warning = failedWorkflows > 0) }
                }
            }

            item {
                StandardSectionHeader("Quick actions")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenBuilds,
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Build APK", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { repositories.firstOrNull()?.let(onOpenRepo) },
                        enabled = repositories.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open repo")
                    }
                }
            }

            if (runs.isNotEmpty()) {
                item {
                    StandardSectionHeader(
                        title = "Workflow activity",
                        subtitle = "${runs.size} recent"
                    )
                }
                items(
                    items = if (showAllRuns) runs else runs.take(3),
                    key = { it.id }
                ) { run ->
                    WorkflowSummaryCard(run)
                }
                if (runs.size > 3) {
                    item {
                        TextButton(
                            onClick = { showAllRuns = !showAllRuns },
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                    title = "Recently updated repositories",
                    subtitle = when {
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

            items(repositories.take(6), key = { it.id }) { repo ->
                RepositoryCard(repo = repo, onClick = { onOpenRepo(repo) })
            }
        }
    }
}

@Composable
private fun GitHubRockHero(
    mode: AppMode,
    profile: GitHubUser?,
    rateLimit: RateLimit?
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(66.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .30f))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "GitHub Rock application logo",
                        modifier = Modifier.padding(5.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "GitHub Rock",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Visual developer control centre",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModeBadge(mode)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                        ) {
                            Text(
                                "v${BuildConfig.VERSION_NAME}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "Hide workspace details" else "Show workspace details")
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        profile?.name ?: profile?.login ?: "Developer workspace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: "Browse public repositories securely",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    profile?.bio?.takeIf(String::isNotBlank)?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    rateLimit?.let {
                        val progress = if (it.limit == 0) 0f else it.remaining.toFloat() / it.limit.toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GitHub API health", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${it.remaining} / ${it.limit}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
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
    }
}

@Composable
private fun WorkflowHealthCard(
    total: Int,
    running: Int,
    success: Int,
    failed: Int
) {
    val finished = (success + failed).coerceAtMost(total)
    val progress = if (total == 0) 0f else finished.toFloat() / total.toFloat()
    val percentage = (progress * 100).toInt().coerceIn(0, 100)

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Build health", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Live workflow status and completion",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$percentage%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("$finished/$total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    modifier: Modifier = Modifier
) {
    val accent = workflowStateColor(state)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(value.toString(), color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LoadingWorkspaceCard() {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Loading your GitHub workspace" },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Loading your GitHub workspace…", fontWeight = FontWeight.SemiBold)
                Text(
                    "Fetching account, repositories, workflow status, and activity time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModeBadge(mode: AppMode) {
    val label = when (mode) {
        AppMode.Connected -> "CONNECTED"
        AppMode.Guest -> "GUEST"
        AppMode.Demo -> "DEMO"
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkflowSummaryCard(run: WorkflowRun) {
    val state = run.displayState()
    val accent = workflowStateColor(state)
    val progress = when (state) {
        WorkflowDisplayState.Queued -> .08f
        WorkflowDisplayState.Running -> .55f
        WorkflowDisplayState.Success,
        WorkflowDisplayState.Failed,
        WorkflowDisplayState.Cancelled -> 1f
        WorkflowDisplayState.Unknown -> 0f
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = .12f)
                ) {
                    Icon(
                        workflowStateIcon(state),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(10.dp).size(22.dp)
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
                        listOf(run.event, run.headBranch.orEmpty()).filter(String::isNotBlank).joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(state)
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = accent
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
private fun StatusBadge(state: WorkflowDisplayState) {
    val accent = workflowStateColor(state)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .30f))
    ) {
        Text(
            state.name,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun workflowStateColor(state: WorkflowDisplayState): Color = when (state) {
    WorkflowDisplayState.Success -> MaterialTheme.colorScheme.tertiary
    WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error
    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    WorkflowDisplayState.Queued,
    WorkflowDisplayState.Running,
    WorkflowDisplayState.Unknown -> MaterialTheme.colorScheme.primary
}

private fun workflowStateIcon(state: WorkflowDisplayState): ImageVector = when (state) {
    WorkflowDisplayState.Success -> Icons.Default.CheckCircle
    WorkflowDisplayState.Failed -> Icons.Default.ErrorOutline
    WorkflowDisplayState.Cancelled -> Icons.Default.ErrorOutline
    WorkflowDisplayState.Queued,
    WorkflowDisplayState.Running,
    WorkflowDisplayState.Unknown -> Icons.Default.CloudQueue
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RepositoryCard(repo: GitHubRepositoryModel, onClick: () -> Unit) {
    val showImages = LocalRemoteImagesEnabled.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        repo.owner.login,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                repo.description ?: "No repository description.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepositoryMetaChip(Icons.Default.Star, "Stars", compactCount(repo.stars))
                RepositoryMetaChip(Icons.Default.CallSplit, "Forks", compactCount(repo.forks))
                RepositoryMetaChip(Icons.Default.ErrorOutline, "Open issues", compactCount(repo.openIssues))
                RepositoryMetaChip(Icons.Default.Code, "Language", repo.language ?: "Repository", accent = true)
                repo.topics.firstOrNull()?.takeIf(String::isNotBlank)?.let {
                    RepositoryMetaChip(Icons.Default.Tag, "Topic", it)
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
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(15.dp))
            Text(
                value,
                color = foreground,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> {
        val whole = value / 1_000_000
        val decimal = (value % 1_000_000) / 100_000
        if (decimal == 0) "${whole}M" else "$whole.${decimal}M"
    }
    value >= 1_000 -> {
        val whole = value / 1_000
        val decimal = (value % 1_000) / 100
        if (decimal == 0) "${whole}k" else "$whole.${decimal}k"
    }
    else -> value.toString()
}

private val githubDateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd MMM yyyy • hh:mm:ss a", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatGitHubTime(value: String): String {
    if (value.isBlank()) return "Time unavailable"
    return runCatching { githubDateTimeFormatter.format(Instant.parse(value)) }
        .getOrDefault(value)
}
