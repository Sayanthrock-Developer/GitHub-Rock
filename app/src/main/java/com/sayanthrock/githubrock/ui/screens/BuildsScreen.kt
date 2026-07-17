package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build as AndroidBuild
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowArtifact
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.WorkflowPreviewHealth
import com.sayanthrock.githubrock.core.util.WorkflowPreviewInspector
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader

private val WorkflowHealthyGreen = Color(0xFF2DA44E)

@Composable
fun BuildsScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onSelectRepository: (GitHubRepositoryModel) -> Unit,
    initialRepository: GitHubRepositoryModel? = null,
    initialRunId: Long? = null,
    viewModel: BuildsViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val actionState by viewModel.state.collectAsStateWithLifecycle()
    val preferences by appearanceViewModel.state.collectAsStateWithLifecycle()
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    var selectedRepository by remember(repositories, initialRepository?.id) {
        val requested = initialRepository?.let { initial ->
            repositories.firstOrNull { it.id == initial.id } ?: initial
        }
        mutableStateOf(requested ?: repositories.firstOrNull())
    }
    val requestedRunId = initialRunId.takeIf { selectedRepository?.id == initialRepository?.id }

    LaunchedEffect(mode, selectedRepository?.id, requestedRunId) {
        if (mode == AppMode.Connected) {
            selectedRepository?.let { viewModel.loadAndroidBuild(it, requestedRunId) }
        } else {
            viewModel.resetBuild()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(if (preferences.compactCards) 10.dp else 14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Builds",
                subtitle = "Read real workflow code and understand every run without leaving the app"
            )
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("GitHub Actions control centre", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "The template generator has been removed. This page now shows only the active repository workflow, native controls, job results, failed steps, and artifacts.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mode == AppMode.Guest) {
                        Text("Connect GitHub to inspect private workflow code and dispatch runs.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { StandardSectionHeader("Choose a repository") }
        if (repositories.isEmpty()) {
            item {
                GlassCard {
                    Text("No repositories are available in this workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repositories.take(8).forEach { repo ->
                        FilterChip(
                            selected = selectedRepository?.id == repo.id,
                            onClick = { selectedRepository = repo },
                            label = { Text(repo.name) },
                            leadingIcon = if (selectedRepository?.id == repo.id) {
                                { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
        selectedRepository?.let { repo ->
            item {
                OutlinedButton(
                    onClick = { onSelectRepository(repo) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preferences.repositoryManager
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (preferences.repositoryManager) "Open native repository manager" else "Repository manager disabled")
                }
            }
        }
        actionState.message?.let { message ->
            item { StatusMessageCard(message, problem = false, preferences = preferences) }
        }
        actionState.error?.let { error ->
            item { StatusMessageCard(error, problem = true, preferences = preferences) }
        }
        if (preferences.workflowPreview) {
            item {
                WorkflowPreviewCard(
                    actionState = actionState,
                    preferences = preferences
                )
            }
        } else {
            item {
                GlassCard {
                    Text(
                        "Workflow code preview is turned off in Customization settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            BuildExecutionCard(
                mode = mode,
                repository = selectedRepository,
                actionState = actionState,
                preferences = preferences,
                onRefresh = { selectedRepository?.let { viewModel.loadAndroidBuild(it) } },
                onDispatch = { ref -> selectedRepository?.let { viewModel.dispatchAndroidBuild(it, ref) } },
                onDownload = { artifact ->
                    selectedRepository?.let { repo ->
                        downloadsViewModel.enqueue(
                            artifact.archiveDownloadUrl,
                            "${repo.name}-${artifact.name}-${artifact.id}.zip"
                        )
                    }
                }
            )
        }
        item { StandardSectionHeader("Recent runs") }
        if (runs.isEmpty()) {
            item { GlassCard { Text("No workflow runs loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        items(runs, key = { it.id }) { run ->
            RecentRunCard(run, preferences)
        }
    }
}

@Composable
private fun StatusMessageCard(
    message: String,
    problem: Boolean,
    preferences: AppearancePreferences
) {
    val accent = statusColor(
        problem = problem,
        healthy = !problem,
        preferences = preferences
    )
    GlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (problem) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accent
            )
            Text(message, color = accent)
        }
    }
}

@Composable
private fun WorkflowPreviewCard(
    actionState: BuildsActionState,
    preferences: AppearancePreferences
) {
    val clipboard = LocalClipboardManager.current
    val source = actionState.workflowSource.orEmpty()
    val report = remember(source, actionState.run, actionState.jobs, actionState.workflowSourceError) {
        WorkflowPreviewInspector.inspect(
            source = source,
            run = actionState.run,
            jobs = actionState.jobs,
            sourceError = actionState.workflowSourceError
        )
    }
    val panelPadding = if (preferences.compactCards) 12.dp else 16.dp

    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Column(
                modifier = Modifier.padding(panelPadding),
                verticalArrangement = Arrangement.spacedBy(if (preferences.compactCards) 8.dp else 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Active workflow code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            actionState.workflowSourcePath ?: "No active workflow path",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                WorkflowHealthFrame(report.health, report.title, report.detail, preferences)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactMetric("Steps", "${report.completedSteps}/${report.totalSteps}", report.failedSteps == 0, preferences)
                    CompactMetric("Failed", report.failedSteps.toString(), report.failedSteps == 0, preferences)
                    CompactMetric(
                        "YAML checks",
                        if (report.sourceProblems.isEmpty()) "Passed" else "${report.sourceProblems.size} problem",
                        report.sourceProblems.isEmpty(),
                        preferences
                    )
                }
                report.sourceProblems.forEach { problem ->
                    CompactProblemRow(problem, preferences)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                actionState.workflowSourceLoading -> {
                    Row(
                        modifier = Modifier.padding(panelPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Loading active workflow code…")
                    }
                }
                source.isBlank() -> {
                    Text(
                        actionState.workflowSourceError ?: "No active workflow code is available.",
                        modifier = Modifier.padding(panelPadding),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> WorkflowCodeViewer(source, preferences.compactCards)
            }

            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(source)) },
                modifier = Modifier.fillMaxWidth().padding(panelPadding),
                enabled = source.isNotBlank()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy workflow code")
            }
        }
    }
}

@Composable
private fun WorkflowHealthFrame(
    health: WorkflowPreviewHealth,
    title: String,
    detail: String,
    preferences: AppearancePreferences
) {
    val accent = when (health) {
        WorkflowPreviewHealth.Healthy -> statusColor(false, true, preferences)
        WorkflowPreviewHealth.Problem -> statusColor(true, false, preferences)
        WorkflowPreviewHealth.Running -> MaterialTheme.colorScheme.primary
        WorkflowPreviewHealth.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f))
    ) {
        Row(
            modifier = Modifier.padding(if (preferences.compactCards) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (health) {
                    WorkflowPreviewHealth.Healthy -> Icons.Default.CheckCircle
                    WorkflowPreviewHealth.Problem -> Icons.Default.ErrorOutline
                    else -> Icons.Default.HourglassTop
                },
                contentDescription = null,
                tint = accent
            )
            Column(Modifier.weight(1f)) {
                Text(title, color = accent, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    healthy: Boolean,
    preferences: AppearancePreferences
) {
    val accent = statusColor(problem = !healthy, healthy = healthy, preferences = preferences)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        border = BorderStroke(1.dp, accent.copy(alpha = .35f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = if (preferences.compactCards) 7.dp else 9.dp)) {
            Text(value, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactProblemRow(problem: String, preferences: AppearancePreferences) {
    val accent = statusColor(problem = true, healthy = false, preferences = preferences)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = .10f)
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(problem, color = accent, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkflowCodeViewer(source: String, compact: Boolean) {
    val numberedSource = remember(source) {
        source.lineSequence().mapIndexed { index, line ->
            "${(index + 1).toString().padStart(3, ' ')}  $line"
        }.joinToString("\n")
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = .88f)
    ) {
        SelectionContainer {
            Text(
                text = numberedSource,
                modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (compact) 26 else 36,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BuildExecutionCard(
    mode: AppMode,
    repository: GitHubRepositoryModel?,
    actionState: BuildsActionState,
    preferences: AppearancePreferences,
    onRefresh: () -> Unit,
    onDispatch: (String) -> Unit,
    onDownload: (WorkflowArtifact) -> Unit
) {
    var ref by remember(repository?.id) { mutableStateOf(repository?.defaultBranch ?: "main") }
    val refIsSafe = remember(ref) { BuildRunTracker.isSafeRef(ref) }
    val context = LocalContext.current
    val notificationManager = remember(context) { NotificationManagerCompat.from(context) }
    val hasNotificationPermission = AndroidBuild.VERSION.SDK_INT < AndroidBuild.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var notificationsAllowed by remember(context) {
        mutableStateOf(hasNotificationPermission && notificationManager.areNotificationsEnabled())
    }
    var pendingDispatchRef by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && notificationManager.areNotificationsEnabled()
        pendingDispatchRef?.let(onDispatch)
        pendingDispatchRef = null
    }
    val dispatchWithNotificationPermission: (String) -> Unit = { selectedRef ->
        if (AndroidBuild.VERSION.SDK_INT >= AndroidBuild.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDispatchRef = selectedRef
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsAllowed = notificationManager.areNotificationsEnabled()
            onDispatch(selectedRef)
        }
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(if (preferences.compactCards) 8.dp else 10.dp)) {
            Text("Run and diagnose", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Dispatch the merged workflow and inspect job and step status in native compact frames.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                mode != AppMode.Connected -> Text("Connect GitHub to start and track a build.", color = MaterialTheme.colorScheme.primary)
                repository == null -> Text("Select a repository first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                actionState.loading && actionState.workflow == null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Detecting Android build workflow…")
                    }
                }
                actionState.workflow == null -> {
                    Text("No merged Android build workflow is active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (preferences.actionsControls) {
                        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh detection") }
                    }
                }
                else -> {
                    val workflow = requireNotNull(actionState.workflow)
                    Text("${workflow.name} • ${workflow.state}", fontWeight = FontWeight.SemiBold)
                    Text(workflow.path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (preferences.actionsControls) {
                        OutlinedTextField(
                            value = ref,
                            onValueChange = { ref = it },
                            label = { Text("Branch or tag") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { dispatchWithNotificationPermission(ref) },
                                enabled = refIsSafe && !actionState.loading && !actionState.tracking,
                                modifier = Modifier.weight(1f)
                            ) { Text("Run workflow") }
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !actionState.loading && !actionState.tracking,
                                modifier = Modifier.weight(1f)
                            ) { Text("Refresh") }
                        }
                        if (!refIsSafe) Text("Use a valid branch or tag.", color = statusColor(true, false, preferences))
                        if (!notificationsAllowed) {
                            Text(
                                "Background tracking continues, but Android notifications are disabled.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text("Actions controls are turned off in Customization settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (actionState.tracking && !preferences.reduceMotion) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (actionState.tracking) {
                Text("Workflow is running", color = MaterialTheme.colorScheme.primary)
            }

            actionState.run?.let { run ->
                val displayState = run.displayState()
                val color = runColor(displayState, preferences)
                HorizontalDivider()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = color.copy(alpha = .10f),
                    border = BorderStroke(1.dp, color.copy(alpha = .38f))
                ) {
                    Column(Modifier.padding(if (preferences.compactCards) 10.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(run.displayTitle.ifBlank { run.name ?: "Android build" }, fontWeight = FontWeight.SemiBold)
                        Text("${displayState.name} • ${run.headBranch.orEmpty()} • run ${run.id}", color = color)
                        if (run.htmlUrl.isNotBlank() && preferences.actionsControls) {
                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl))) }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open run details")
                            }
                        }
                    }
                }
            }

            actionState.jobs.forEach { job ->
                val jobProblem = job.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure")
                val jobHealthy = job.conclusion == "success"
                val accent = statusColor(jobProblem, jobHealthy, preferences)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .35f))
                ) {
                    Column(Modifier.padding(if (preferences.compactCards) 9.dp else 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (jobProblem) Icons.Default.ErrorOutline else if (jobHealthy) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(19.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(job.name, fontWeight = FontWeight.Medium)
                                Text("${job.status} • ${job.conclusion ?: "pending"}", color = accent, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (preferences.workflowStepDetails) {
                            job.steps.forEach { step ->
                                val stepFailed = step.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure")
                                val stepPassed = step.conclusion == "success"
                                val stepAccent = statusColor(stepFailed, stepPassed, preferences)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (stepFailed) Icons.Default.ErrorOutline else if (stepPassed) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = stepAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(step.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(step.conclusion ?: step.status, color = stepAccent, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Text(
                                "${job.steps.size} steps hidden by Customization settings",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            actionState.artifacts.forEach { artifact ->
                if (preferences.actionsControls) {
                    OutlinedButton(
                        onClick = { onDownload(artifact) },
                        enabled = !artifact.expired,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (artifact.expired) "${artifact.name} expired" else "Queue ${artifact.name} in Downloads")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRunCard(run: WorkflowRun, preferences: AppearancePreferences) {
    val state = run.displayState()
    val accent = runColor(state, preferences)
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (state) {
                    WorkflowDisplayState.Success -> Icons.Default.CheckCircle
                    WorkflowDisplayState.Failed -> Icons.Default.ErrorOutline
                    else -> Icons.Default.HourglassTop
                },
                contentDescription = null,
                tint = accent
            )
            Column(Modifier.weight(1f)) {
                Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.SemiBold)
                Text("${run.event} • ${run.headBranch.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(state.name, color = accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun statusColor(
    problem: Boolean,
    healthy: Boolean,
    preferences: AppearancePreferences
): Color = when {
    !preferences.statusColors -> MaterialTheme.colorScheme.primary
    problem -> MaterialTheme.colorScheme.error
    healthy -> WorkflowHealthyGreen
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun runColor(state: WorkflowDisplayState, preferences: AppearancePreferences): Color = when (state) {
    WorkflowDisplayState.Success -> statusColor(false, true, preferences)
    WorkflowDisplayState.Failed -> statusColor(true, false, preferences)
    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}
