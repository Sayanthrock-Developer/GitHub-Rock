package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build as AndroidBuild
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
        mutableStateOf(
            initialRepository?.let { initial -> repositories.firstOrNull { it.id == initial.id } ?: initial }
                ?: repositories.firstOrNull()
        )
    }
    var filter by remember { mutableStateOf(BuildFilter.All) }
    var selectedRun by remember { mutableStateOf<WorkflowRun?>(null) }
    val requestedRunId = initialRunId.takeIf { selectedRepository?.id == initialRepository?.id }

    LaunchedEffect(mode, selectedRepository?.id, requestedRunId) {
        if (mode == AppMode.Connected && selectedRepository != null) {
            viewModel.loadAndroidBuild(requireNotNull(selectedRepository), requestedRunId)
        } else {
            viewModel.resetBuild()
        }
    }

    val visibleRuns = remember(runs, filter) {
        runs.filter { run ->
            when (filter) {
                BuildFilter.All -> true
                BuildFilter.Running -> run.displayState() !in setOf(WorkflowDisplayState.Success, WorkflowDisplayState.Failed, WorkflowDisplayState.Cancelled)
                BuildFilter.Failed -> run.displayState() == WorkflowDisplayState.Failed
                BuildFilter.Success -> run.displayState() == WorkflowDisplayState.Success
            }
        }
    }
    val counts = remember(runs) {
        BuildCounts(
            running = runs.count { it.displayState() !in setOf(WorkflowDisplayState.Success, WorkflowDisplayState.Failed, WorkflowDisplayState.Cancelled) },
            failed = runs.count { it.displayState() == WorkflowDisplayState.Failed },
            success = runs.count { it.displayState() == WorkflowDisplayState.Success },
            artifacts = actionState.artifacts.size
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(if (preferences.compactCards) 10.dp else 14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Builds",
                subtitle = "Understand workflow runs, code, jobs, steps and artifacts in one native view"
            )
        }

        item { BuildSummary(counts, preferences) }

        item {
            BuildFilterRow(filter, onFilter = { filter = it })
        }

        item { RepositoryPicker(repositories, selectedRepository, preferences) { selectedRepository = it } }

        selectedRepository?.let { repo ->
            item {
                OutlinedButton(
                    onClick = { onSelectRepository(repo) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preferences.repositoryManager
                ) {
                    Icon(Icons.Default.Code, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (preferences.repositoryManager) "Open repository manager" else "Repository manager disabled")
                }
            }
        }

        actionState.message?.let { item { StatusMessageCard(it, false, preferences) } }
        actionState.error?.let { item { StatusMessageCard(it, true, preferences) } }

        item {
            WorkflowCodePanel(actionState, preferences)
        }

        item {
            BuildExecutionPanel(
                mode = mode,
                repository = selectedRepository,
                actionState = actionState,
                preferences = preferences,
                onRefresh = { selectedRepository?.let(viewModel::loadAndroidBuild) },
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

        item {
            Text("Recent runs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (visibleRuns.isEmpty()) {
            item { GlassCard { Text("No runs match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(visibleRuns, key = { it.id }) { run ->
                RecentRunCard(run, preferences) { selectedRun = run }
            }
        }
    }

    selectedRun?.let { run ->
        RunDetailsDialog(run = run, preferences = preferences, onDismiss = { selectedRun = null })
    }
}

private enum class BuildFilter(val label: String) { All("All"), Running("Running"), Failed("Failed"), Success("Success") }
private data class BuildCounts(val running: Int, val failed: Int, val success: Int, val artifacts: Int)

@Composable
private fun BuildSummary(counts: BuildCounts, preferences: AppearancePreferences) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Build health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric("Running", counts.running.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                SummaryMetric("Failed", counts.failed.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric("Successful", counts.success.toString(), statusColor(false, true, preferences), Modifier.weight(1f))
                SummaryMetric("Artifacts", counts.artifacts.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BuildFilterRow(selected: BuildFilter, onFilter: (BuildFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BuildFilter.values().forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onFilter(filter) }, label = { Text(filter.label) })
        }
    }
}

@Composable
private fun RepositoryPicker(
    repositories: List<GitHubRepositoryModel>,
    selected: GitHubRepositoryModel?,
    preferences: AppearancePreferences,
    onSelect: (GitHubRepositoryModel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (repositories.isEmpty()) {
            GlassCard { Text("No repositories are available in this workspace.") }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repositories.take(8).forEach { repo ->
                    FilterChip(
                        selected = selected?.id == repo.id,
                        onClick = { onSelect(repo) },
                        label = { Text(repo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = if (selected?.id == repo.id) { { Icon(Icons.Default.Build, null, Modifier.size(18.dp)) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMessageCard(message: String, problem: Boolean, preferences: AppearancePreferences) {
    val accent = statusColor(problem, !problem, preferences)
    GlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (problem) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null, tint = accent)
            Text(message, color = accent)
        }
    }
}

@Composable
private fun WorkflowCodePanel(actionState: BuildsActionState, preferences: AppearancePreferences) {
    val clipboard = LocalClipboardManager.current
    val source = actionState.workflowSource.orEmpty()
    val report = remember(source, actionState.run, actionState.jobs, actionState.workflowSourceError) {
        WorkflowPreviewInspector.inspect(source, actionState.run, actionState.jobs, actionState.workflowSourceError)
    }

    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)) {
                        Icon(Icons.Default.Code, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Workflow code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactMetric("Steps", "${report.completedSteps}/${report.totalSteps}", report.failedSteps == 0, preferences)
                    CompactMetric("Failed", report.failedSteps.toString(), report.failedSteps == 0, preferences)
                    CompactMetric("YAML", if (report.sourceProblems.isEmpty()) "Passed" else problemCountText(report.sourceProblems.size), report.sourceProblems.isEmpty(), preferences)
                }
                report.sourceProblems.forEach { CompactProblemRow(it, preferences) }
            }
            HorizontalDivider()
            when {
                actionState.workflowSourceLoading -> LoadingRow("Loading real workflow code…")
                source.isBlank() -> Text(actionState.workflowSourceError ?: "No active workflow code is available.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> WorkflowCodeViewer(source, preferences.compactCards)
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(source)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = source.isNotBlank()
            ) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("Copy workflow code")
            }
        }
    }
}

@Composable
private fun WorkflowHealthFrame(health: WorkflowPreviewHealth, title: String, detail: String, preferences: AppearancePreferences) {
    val accent = when (health) {
        WorkflowPreviewHealth.Healthy -> statusColor(false, true, preferences)
        WorkflowPreviewHealth.Problem -> statusColor(true, false, preferences)
        WorkflowPreviewHealth.Running -> MaterialTheme.colorScheme.primary
        WorkflowPreviewHealth.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f))
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (health) {
                    WorkflowPreviewHealth.Healthy -> Icons.Default.CheckCircle
                    WorkflowPreviewHealth.Problem -> Icons.Default.ErrorOutline
                    WorkflowPreviewHealth.Running -> Icons.Default.Sync
                    WorkflowPreviewHealth.Unknown -> Icons.Default.HelpOutline
                },
                contentDescription = title,
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
private fun CompactMetric(label: String, value: String, healthy: Boolean, preferences: AppearancePreferences) {
    val accent = statusColor(!healthy, healthy, preferences)
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(value, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactProblemRow(problem: String, preferences: AppearancePreferences) {
    val accent = statusColor(true, false, preferences)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .10f)) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.size(18.dp), tint = accent)
            Text(problem, color = accent, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkflowCodeViewer(source: String, compact: Boolean) {
    val numbered = remember(source) {
        source.lineSequence().mapIndexed { index, line -> "${(index + 1).toString().padStart(3, ' ')}  $line" }.joinToString("\n")
    }
    val verticalState = rememberScrollState()
    val horizontalState = rememberScrollState()
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background.copy(alpha = .88f)) {
        Box(
            Modifier.fillMaxWidth().heightIn(max = if (compact) 360.dp else 520.dp).verticalScroll(verticalState).horizontalScroll(horizontalState)
        ) {
            SelectionContainer {
                Text(numbered, Modifier.padding(if (compact) 12.dp else 16.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, softWrap = false)
            }
        }
    }
}

@Composable
private fun BuildExecutionPanel(
    mode: AppMode,
    repository: GitHubRepositoryModel?,
    actionState: BuildsActionState,
    preferences: AppearancePreferences,
    onRefresh: () -> Unit,
    onDispatch: (String) -> Unit,
    onDownload: (WorkflowArtifact) -> Unit
) {
    var ref by remember(repository?.id) { mutableStateOf(repository?.defaultBranch ?: "main") }
    var expandedJobs by remember(actionState.run?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val refIsSafe = remember(ref) { BuildRunTracker.isSafeRef(ref) }
    val context = LocalContext.current
    val notificationManager = remember(context) { NotificationManagerCompat.from(context) }
    var notificationsAllowed by remember(context) { mutableStateOf(notificationManager.areNotificationsEnabled()) }
    var pendingRef by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && notificationManager.areNotificationsEnabled()
        pendingRef?.let(onDispatch)
        pendingRef = null
    }
    val dispatch: (String) -> Unit = { selectedRef ->
        if (AndroidBuild.VERSION.SDK_INT >= AndroidBuild.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingRef = selectedRef
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsAllowed = notificationManager.areNotificationsEnabled()
            onDispatch(selectedRef)
        }
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Run diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Follow the real GitHub workflow from run → job → step.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                mode != AppMode.Connected -> Text("Connect GitHub to start and track a build.")
                repository == null -> Text("Select a repository first.")
                actionState.loading && actionState.workflow == null -> LoadingRow("Detecting Android build workflow…")
                actionState.workflow == null -> {
                    Text("No merged Android build workflow is active.")
                    if (preferences.actionsControls) OutlinedButton(onClick = onRefresh, Modifier.fillMaxWidth()) { Text("Refresh detection") }
                }
                else -> {
                    val workflow = requireNotNull(actionState.workflow)
                    Text(workflow.name, fontWeight = FontWeight.SemiBold)
                    Text(workflow.path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (preferences.actionsControls) {
                        OutlinedTextField(ref, { ref = it }, Modifier.fillMaxWidth(), label = { Text("Branch or tag") }, singleLine = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ dispatch(ref) }, Modifier.weight(1f), enabled = refIsSafe && !actionState.loading && !actionState.tracking) { Text("Run workflow") }
                            OutlinedButton(onRefresh, Modifier.weight(1f), enabled = !actionState.loading && !actionState.tracking) { Text("Refresh") }
                        }
                        if (!refIsSafe) Text("Use a valid branch or tag.", color = statusColor(true, false, preferences))
                        if (!notificationsAllowed) Text("Notifications are disabled. Build status remains available in-app.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (actionState.tracking && !preferences.reduceMotion) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if (actionState.tracking) Text("Workflow is running", color = MaterialTheme.colorScheme.primary)

            actionState.run?.let { RunFrame(it, preferences) }

            if (actionState.jobs.isEmpty() && actionState.run != null) {
                Text("No job details returned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actionState.jobs.forEachIndexed { index, job ->
                val failed = job.conclusion in failureConclusions
                val passed = job.conclusion == "success"
                val accent = statusColor(failed, passed, preferences)
                val expanded = job.id in expandedJobs
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
                    border = BorderStroke(1.dp, accent.copy(alpha = .35f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusRow(job.name, job.conclusion ?: job.status, failed, passed, accent)
                        }
                        if (preferences.workflowStepDetails && job.steps.isNotEmpty()) {
                            TextButton(
                                onClick = { expandedJobs = if (expanded) expandedJobs - job.id else expandedJobs + job.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (expanded) "Hide ${job.steps.size} steps" else "Show ${job.steps.size} steps")
                            }
                            if (expanded) {
                                job.steps.forEachIndexed { stepIndex, step ->
                                    val stepFailed = step.conclusion in failureConclusions
                                    val stepPassed = step.conclusion == "success"
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${stepIndex + 1}", Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        StatusRow(step.name, step.conclusion ?: step.status, stepFailed, stepPassed, statusColor(stepFailed, stepPassed, preferences), small = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (preferences.actionsControls && actionState.artifacts.isNotEmpty()) {
                Text("Artifacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                actionState.artifacts.forEach { artifact ->
                    OutlinedButton({ onDownload(artifact) }, Modifier.fillMaxWidth(), enabled = !artifact.expired) {
                        Icon(Icons.Default.Archive, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (artifact.expired) "${artifact.name} expired" else "Download ${artifact.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunFrame(run: WorkflowRun, preferences: AppearancePreferences) {
    val state = run.displayState()
    val accent = runColor(state, preferences)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .38f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusIcon(state, accent)
                Column(Modifier.weight(1f)) {
                    Text(run.displayTitle.ifBlank { run.name ?: "Android build" }, fontWeight = FontWeight.SemiBold)
                    Text("${state.name} • ${run.headBranch.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("#${run.id}", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

@Composable
private fun StatusIcon(state: WorkflowDisplayState, accent: Color) {
    Icon(
        when (state) {
            WorkflowDisplayState.Success -> Icons.Default.CheckCircle
            WorkflowDisplayState.Failed -> Icons.Default.ErrorOutline
            WorkflowDisplayState.Cancelled -> Icons.Default.Cancel
            else -> Icons.Default.Sync
        },
        contentDescription = state.name,
        tint = accent
    )
}

@Composable
private fun LoadingRow(text: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text)
    }
}

@Composable
private fun StatusRow(title: String, status: String, failed: Boolean, passed: Boolean, accent: Color, small: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (failed) Icons.Default.ErrorOutline else if (passed) Icons.Default.CheckCircle else Icons.Default.Sync,
            contentDescription = status,
            Modifier.size(if (small) 17.dp else 20.dp),
            tint = accent
        )
        Text(title, Modifier.weight(1f), style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
        Text(status, color = accent, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RecentRunCard(run: WorkflowRun, preferences: AppearancePreferences, onClick: () -> Unit) {
    val state = run.displayState()
    val accent = runColor(state, preferences)
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(state, accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${run.event} • ${run.headBranch.orEmpty()} • #${run.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(state.name, color = accent, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = "Open run details")
        }
    }
}

@Composable
private fun RunDetailsDialog(run: WorkflowRun, preferences: AppearancePreferences, onDismiss: () -> Unit) {
    val state = run.displayState()
    val accent = runColor(state, preferences)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                Modifier.fillMaxWidth().fillMaxHeight(.72f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Run details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Native GitHub build information", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = .12f)) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusIcon(state, accent)
                            Column(Modifier.weight(1f)) {
                                Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.Bold)
                                Text(state.name, color = accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    DetailRow("Repository", run.repositoryFullName)
                    DetailRow("Branch", run.headBranch.orEmpty())
                    DetailRow("Event", run.event)
                    DetailRow("Run", "#${run.id}")
                    Text("What happened", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (state) {
                            WorkflowDisplayState.Success -> "GitHub reports this workflow run completed successfully."
                            WorkflowDisplayState.Failed -> "GitHub reports a failure. Open the native job/step details in Builds to identify the failing step."
                            WorkflowDisplayState.Cancelled -> "This workflow run was cancelled."
                            else -> "This workflow run is still active. Its status can update while you stay in Builds."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun statusColor(problem: Boolean, healthy: Boolean, preferences: AppearancePreferences): Color = when {
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

private fun problemCountText(count: Int): String = "$count ${if (count == 1) "problem" else "problems"}"
private val failureConclusions = setOf("failure", "timed_out", "action_required", "startup_failure")
