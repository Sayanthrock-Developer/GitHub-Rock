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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class HomeFeed(val label: String, val subtitle: String) {
    Recent("Recent", "Latest repository updates"),
    Popular("Popular", "Most starred repositories"),
    Activity("Activity", "GitHub Actions and builds")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun HomeScreen(
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    onOpenBuilds: () -> Unit,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFeed by rememberSaveable { mutableStateOf(HomeFeed.Recent) }
    var showAllRuns by rememberSaveable { mutableStateOf(false) }

    val visibleRepositories = remember(repositories, selectedFeed) {
        when (selectedFeed) {
            HomeFeed.Recent -> repositories.sortedByDescending { it.updatedAt }
            HomeFeed.Popular -> repositories.sortedByDescending { it.stars }
            HomeFeed.Activity -> repositories.sortedByDescending { it.updatedAt }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = StandardScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLoading) {
                item { LoadingWorkspaceCard() }
            }

            item {
                FeedTabs(
                    selected = selectedFeed,
                    onSelected = { selectedFeed = it }
                )
            }

            when (selectedFeed) {
                HomeFeed.Activity -> {
                    item {
                        SectionHeading(
                            title = "Workflow activity",
                            subtitle = if (runs.isEmpty()) "No recent workflow runs" else "${runs.size} recent runs"
                        )
                    }

                    if (!isLoading && runs.isEmpty()) {
                        item {
                            EmptyFeedCard(
                                title = "No workflow activity yet",
                                message = "Run a workflow or open Builds to create and monitor an Android build.",
                                actionLabel = "Open Builds",
                                onAction = onOpenBuilds
                            )
                        }
                    }

                    items(
                        items = if (showAllRuns) runs else runs.take(4),
                        key = { it.id }
                    ) { run ->
                        WorkflowSummaryCard(run)
                    }

                    if (runs.size > 4) {
                        item {
                            TextButton(
                                onClick = { showAllRuns = !showAllRuns },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (showAllRuns) "Show fewer runs" else "Show all workflow runs")
                            }
                        }
                    }
                }

                HomeFeed.Recent,
                HomeFeed.Popular -> {
                    item {
                        SectionHeading(
                            title = selectedFeed.label,
                            subtitle = when {
                                isLoading -> "Loading repositories"
                                visibleRepositories.isEmpty() -> "Nothing to show"
                                else -> selectedFeed.subtitle
                            }
                        )
                    }

                    if (!isLoading && visibleRepositories.isEmpty()) {
                        item {
                            EmptyFeedCard(
                                title = "No repositories found",
                                message = "Pull down to refresh your GitHub workspace.",
                                actionLabel = "Refresh",
                                onAction = onRefresh
                            )
                        }
                    }

                    items(
                        items = visibleRepositories.take(12),
                        key = { it.id }
                    ) { repository ->
                        DiscoveryRepositoryCard(
                            repository = repository,
                            rank = if (selectedFeed == HomeFeed.Popular) {
                                visibleRepositories.indexOf(repository) + 1
                            } else {
                                null
                            },
                            onClick = { onOpenRepo(repository) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun FeedTabs(
    selected: HomeFeed,
    onSelected: (HomeFeed) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            HomeFeed.entries.forEach { feed ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = if (feed == selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (feed == selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = { onSelected(feed) }
                ) {
                    Text(
                        text = feed.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (feed == selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
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
                .semantics { contentDescription = "Loading GitHub workspace" },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Column {
                Text("Loading your workspace…", fontWeight = FontWeight.Bold)
                Text(
                    "Fetching repositories, account status and workflow activity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
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
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = .12f)
                ) {
                    Icon(
                        imageVector = workflowStateIcon(state),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                        tint = accent
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = run.displayTitle.ifBlank { run.name ?: "Workflow" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(run.event, run.headBranch.orEmpty())
                            .filter(String::isNotBlank)
                            .joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(state.name, accent)
            }
            HorizontalDivider(color = accent.copy(alpha = .24f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatGitHubTime(run.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Run #${run.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DiscoveryRepositoryCard(
    repository: GitHubRepositoryModel,
    rank: Int?,
    onClick: () -> Unit
) {
    val showImages = LocalRemoteImagesEnabled.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                    ) {
                        if (showImages && repository.owner.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = repository.owner.avatarUrl,
                                contentDescription = "${repository.owner.login} avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    repository.owner.login.take(2).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rank?.let {
                                Text(
                                    text = "#$it",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = repository.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = repository.owner.login,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open ${repository.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = repository.description ?: "No repository description provided.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepositoryMetaChip(Icons.Default.Star, "Stars", compactCount(repository.stars), true)
                    RepositoryMetaChip(Icons.Default.CallSplit, "Forks", compactCount(repository.forks))
                    RepositoryMetaChip(Icons.Default.ErrorOutline, "Issues", compactCount(repository.openIssues))
                    RepositoryMetaChip(Icons.Default.Code, "Language", repository.language ?: "Other")
                    repository.topics.firstOrNull()?.takeIf(String::isNotBlank)?.let { topic ->
                        RepositoryMetaChip(Icons.Default.Tag, "Topic", topic)
                    }
                }

                if (repository.updatedAt.isNotBlank()) {
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
                            text = "Updated ${formatGitHubTime(repository.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (accent) MaterialTheme.colorScheme.primary.copy(alpha = .25f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = foreground)
            Text(
                text = value,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
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
            text = label,
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
    WorkflowDisplayState.Failed,
    WorkflowDisplayState.Cancelled -> Icons.Default.ErrorOutline
    else -> Icons.Default.CloudQueue
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}

private val githubDateTimeFormatter = DateTimeFormatter
    .ofPattern("dd MMM yyyy • hh:mm a", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatGitHubTime(value: String): String {
    if (value.isBlank()) return "Time unavailable"
    return runCatching { githubDateTimeFormatter.format(Instant.parse(value)) }.getOrDefault(value)
}
