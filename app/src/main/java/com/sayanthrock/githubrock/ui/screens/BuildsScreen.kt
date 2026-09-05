package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build as AndroidBuild
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import com.sayanthrock.githubrock.core.model.WorkflowJob
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
    onOpenRun: (GitHubRepositoryModel, WorkflowRun) -> Unit = { _, _ -> },
    initialRepository: GitHubRepositoryModel? = null,
    initialRunId: Long? = null,
    viewModel: BuildsViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val actionState by viewModel.state.collectAsStateWithLifecycle()
    val preferences by appearanceViewModel.state.collectAsStateWithLifecycle()
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    var selectedRepository by remember(repositories, initialRepository?.id) { mutableStateOf(initialRepository?.let { initial -> repositories.firstOrNull { it.id == initial.id } ?: initial } ?: repositories.firstOrNull()) }
    var filter by remember { mutableStateOf(BuildFilter.All) }
    var selectedRunId by remember(initialRunId) { mutableStateOf(initialRunId) }
    val requestedRunId = initialRunId.takeIf { selectedRepository?.id == initialRepository?.id }
    val repositoryRuns = actionState.recentRuns
    val selectedRun = selectedRunId?.let { id -> repositoryRuns.firstOrNull { it.id == id } ?: actionState.run?.takeIf { it.id == id } }

    LaunchedEffect(mode, selectedRepository?.id, requestedRunId) {
        if (mode == AppMode.Connected && selectedRepository != null) viewModel.loadAndroidBuild(requireNotNull(selectedRepository), requestedRunId) else viewModel.resetBuild()
    }

    val visibleRuns = remember(repositoryRuns, filter) { repositoryRuns.filter { run -> when (filter) {
        BuildFilter.All -> true
        BuildFilter.Running -> run.displayState() !in setOf(WorkflowDisplayState.Success, WorkflowDisplayState.Failed, WorkflowDisplayState.Cancelled)
        BuildFilter.Failed -> run.displayState() == WorkflowDisplayState.Failed
        BuildFilter.Success -> run.displayState() == WorkflowDisplayState.Success
    } } }
    val counts = remember(repositoryRuns, actionState.artifacts) { BuildCounts(
        repositoryRuns.count { it.displayState() !in setOf(WorkflowDisplayState.Success, WorkflowDisplayState.Failed, WorkflowDisplayState.Cancelled) },
        repositoryRuns.count { it.displayState() == WorkflowDisplayState.Failed },
        repositoryRuns.count { it.displayState() == WorkflowDisplayState.Success },
        actionState.artifacts.size
    ) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = StandardScreenPadding, verticalArrangement = Arrangement.spacedBy(if (preferences.compactCards) 10.dp else 14.dp)) {
            item { StandardScreenHeader(title = "Builds", subtitle = "Understand workflow runs, code, jobs, steps and artifacts in one native view") }
            item { BuildSummary(counts, preferences) }
            item { BuildFilterRow(filter) { filter = it } }
            item { RepositoryPicker(repositories, selectedRepository) { selectedRepository = it } }
            selectedRepository?.let { repo -> item {
                OutlinedButton(onClick = { onSelectRepository(repo) }, modifier = Modifier.fillMaxWidth(), enabled = preferences.repositoryManager) {
                    Icon(Icons.Default.Code, null); Spacer(Modifier.width(8.dp)); Text(if (preferences.repositoryManager) "Open repository manager" else "Repository manager disabled")
                }
            } }
            actionState.message?.let { item { StatusMessageCard(it, false, preferences) } }
            actionState.error?.let { item { StatusMessageCard(it, true, preferences) } }
            item { WorkflowCodePanel(actionState, preferences) }
            item { BuildExecutionPanel(mode, selectedRepository, actionState, preferences,
                { selectedRepository?.let(viewModel::loadAndroidBuild) },
                { ref -> selectedRepository?.let { viewModel.dispatchAndroidBuild(it, ref) } }) { artifact ->
                selectedRepository?.let { repo -> downloadsViewModel.enqueue(artifact.archiveDownloadUrl, "${repo.name}-${artifact.name}-${artifact.id}.zip") }
            } }
            item { Text("Recent runs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (visibleRuns.isEmpty()) item { GlassCard { Text("No runs match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            else items(visibleRuns, key = { it.id }) { run -> RecentRunCard(run, preferences) { selectedRunId = run.id } }
        }
        if (selectedRun != null && selectedRepository != null) RunDetailsDialog(
            selectedRun, selectedRepository, actionState.jobs, actionState.artifacts, preferences,
            { selectedRunId = null },
            { viewModel.loadAndroidBuild(requireNotNull(selectedRepository), selectedRun.id) },
            { viewModel.rerunRun(requireNotNull(selectedRepository), selectedRun.id) },
            { viewModel.cancelRun(requireNotNull(selectedRepository), selectedRun.id) }
        ) { artifact -> downloadsViewModel.enqueue(artifact.archiveDownloadUrl, "${selectedRepository.name}-${artifact.name}-${artifact.id}.zip") }
    }
}

private enum class BuildFilter(val label: String) { All("All"), Running("Running"), Failed("Failed"), Success("Success") }
private data class BuildCounts(val running: Int, val failed: Int, val success: Int, val artifacts: Int)

@Composable private fun BuildSummary(counts: BuildCounts, preferences: AppearancePreferences) { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Build health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SummaryMetric("Running", counts.running.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f)); SummaryMetric("Failed", counts.failed.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f)) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SummaryMetric("Successful", counts.success.toString(), statusColor(false, true, preferences), Modifier.weight(1f)); SummaryMetric("Artifacts", counts.artifacts.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) }
} } }
@Composable private fun SummaryMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) { Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) { Column(Modifier.padding(12.dp)) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun BuildFilterRow(selected: BuildFilter, onFilter: (BuildFilter) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { BuildFilter.values().forEach { FilterChip(selected == it, { onFilter(it) }, label = { Text(it.label) }) } } }
@Composable private fun RepositoryPicker(repositories: List<GitHubRepositoryModel>, selected: GitHubRepositoryModel?, onSelect: (GitHubRepositoryModel) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); if (repositories.isEmpty()) GlassCard { Text("No repositories are available in this workspace.") } else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repositories.forEach { repo -> FilterChip(selected?.id == repo.id, { onSelect(repo) }, label = { Text(repo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = if (selected?.id == repo.id) { { Icon(Icons.Default.Build, null, Modifier.size(18.dp)) } } else null) } } } }
@Composable private fun StatusMessageCard(message: String, problem: Boolean, preferences: AppearancePreferences) { val accent = statusColor(problem, !problem, preferences); GlassCard { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (problem) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null, tint = accent); Text(message, color = accent) } } }
@Composable private fun WorkflowCodePanel(actionState: BuildsActionState, preferences: AppearancePreferences) { val clipboard = LocalClipboardManager.current; val source = actionState.workflowSource.orEmpty(); val report = remember(source, actionState.run, actionState.jobs, actionState.workflowSourceError) { WorkflowPreviewInspector.inspect(source, actionState.run, actionState.jobs, actionState.workflowSourceError) }; GlassCard(contentPadding = PaddingValues(0.dp)) { Column { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Workflow code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(actionState.workflowSourcePath ?: "No active workflow path", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); WorkflowHealthFrame(report.health, report.title, report.detail, preferences) }; HorizontalDivider(); when { actionState.workflowSourceLoading -> LoadingRow("Loading real workflow code…"); source.isBlank() -> Text(actionState.workflowSourceError ?: "No active workflow code is available.", Modifier.padding(16.dp)); else -> WorkflowCodeViewer(source, preferences.compactCards) }; OutlinedButton({ clipboard.setText(AnnotatedString(source)) }, Modifier.fillMaxWidth().padding(16.dp), enabled = source.isNotBlank()) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy workflow code") } } } }
@Composable private fun WorkflowHealthFrame(health: WorkflowPreviewHealth, title: String, detail: String, preferences: AppearancePreferences) { val accent = when (health) { WorkflowPreviewHealth.Healthy -> statusColor(false, true, preferences); WorkflowPreviewHealth.Problem -> statusColor(true, false, preferences); WorkflowPreviewHealth.Running -> MaterialTheme.colorScheme.primary; WorkflowPreviewHealth.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant }; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, title, tint = accent); Column(Modifier.weight(1f)) { Text(title, color = accent, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } } }
@Composable private fun WorkflowCodeViewer(source: String, compact: Boolean) { val numbered = remember(source) { source.lineSequence().mapIndexed { index, line -> "${(index + 1).toString().padStart(3, ' ')}  $line" }.joinToString("\n") }; Box(Modifier.fillMaxWidth().heightIn(max = if (compact) 360.dp else 520.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) { SelectionContainer { Text(numbered, Modifier.padding(16.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, softWrap = false) } } }

@Composable private fun BuildExecutionPanel(mode: AppMode, repository: GitHubRepositoryModel?, actionState: BuildsActionState, preferences: AppearancePreferences, onRefresh: () -> Unit, onDispatch: (String) -> Unit, onDownload: (WorkflowArtifact) -> Unit) { var ref by remember(repository?.id) { mutableStateOf(repository?.defaultBranch ?: "main") }; val context = LocalContext.current; val notificationManager = remember(context) { NotificationManagerCompat.from(context) }; var notificationsAllowed by remember(context) { mutableStateOf(notificationManager.areNotificationsEnabled()) }; var pendingRef by remember { mutableStateOf<String?>(null) }; val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> notificationsAllowed = granted && notificationManager.areNotificationsEnabled(); pendingRef?.let(onDispatch); pendingRef = null }; val dispatch: (String) -> Unit = { selectedRef -> if (AndroidBuild.VERSION.SDK_INT >= AndroidBuild.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) { pendingRef = selectedRef; permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } else { notificationsAllowed = notificationManager.areNotificationsEnabled(); onDispatch(selectedRef) } }; GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Run diagnosis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); when { mode != AppMode.Connected -> Text("Connect GitHub to start and track a build."); repository == null -> Text("Select a repository first."); actionState.loading && actionState.workflow == null -> LoadingRow("Detecting Android build workflow…"); actionState.workflow == null -> { Text("No merged Android build workflow is active."); if (preferences.actionsControls) OutlinedButton(onClick = onRefresh, Modifier.fillMaxWidth()) { Text("Refresh detection") } }; else -> { val workflow = requireNotNull(actionState.workflow); Text(workflow.name, fontWeight = FontWeight.SemiBold); Text(workflow.path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); if (preferences.actionsControls) { OutlinedTextField(ref, { ref = it }, Modifier.fillMaxWidth(), label = { Text("Branch or tag") }, singleLine = true); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ dispatch(ref) }, Modifier.weight(1f), enabled = BuildRunTracker.isSafeRef(ref) && !actionState.loading && !actionState.tracking) { Text("Run workflow") }; OutlinedButton(onRefresh, Modifier.weight(1f), enabled = !actionState.loading && !actionState.tracking) { Text("Refresh") } }; if (!notificationsAllowed) Text("Notifications are disabled. Build status remains available in-app.", style = MaterialTheme.typography.bodySmall) } } }; if (actionState.tracking && !preferences.reduceMotion) LinearProgressIndicator(Modifier.fillMaxWidth()); else if (actionState.tracking) Text("Workflow is running", color = MaterialTheme.colorScheme.primary); actionState.run?.let { RunFrame(it, preferences) }; if (actionState.artifacts.isNotEmpty()) { Text("Artifacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); actionState.artifacts.forEach { artifact -> OutlinedButton({ onDownload(artifact) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(artifact.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis) } } } } } }
@Composable private fun RunFrame(run: WorkflowRun, preferences: AppearancePreferences) { val state = run.displayState(); val accent = runColor(state, preferences); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .10f), border = BorderStroke(1.dp, accent.copy(alpha = .38f))) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatusIcon(state, accent); Column(Modifier.weight(1f)) { Text(run.displayTitle.ifBlank { run.name ?: "Android build" }, fontWeight = FontWeight.SemiBold); Text("${state.name} • ${run.headBranch.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("#${run.id}", color = accent) } } }
@Composable private fun StatusIcon(state: WorkflowDisplayState, accent: Color) { Icon(when (state) { WorkflowDisplayState.Success -> Icons.Default.CheckCircle; WorkflowDisplayState.Failed -> Icons.Default.ErrorOutline; WorkflowDisplayState.Cancelled -> Icons.Default.Cancel; else -> Icons.Default.Sync }, state.name, tint = accent) }
@Composable private fun LoadingRow(text: String) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text(text) } }
@Composable private fun RecentRunCard(run: WorkflowRun, preferences: AppearancePreferences, onClick: () -> Unit) { val state = run.displayState(); val accent = runColor(state, preferences); OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { StatusIcon(state, accent); Column(Modifier.weight(1f)) { Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${run.event} • ${run.headBranch.orEmpty()} • #${run.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; Text(state.name, color = accent, fontWeight = FontWeight.SemiBold); Icon(Icons.Default.ChevronRight, "Open run details") } } }

private enum class DetailHeight(val fraction: Float) { Compact(.56f), Expanded(.78f), Full(1f) }

@Composable private fun RunDetailsDialog(run: WorkflowRun, repository: GitHubRepositoryModel, jobs: List<WorkflowJob>, artifacts: List<WorkflowArtifact>, preferences: AppearancePreferences, onDismiss: () -> Unit, onRefresh: () -> Unit, onRerun: () -> Unit, onCancel: () -> Unit, onDownload: (WorkflowArtifact) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state = run.displayState(); val accent = runColor(state, preferences)
    var height by remember { mutableStateOf(DetailHeight.Expanded) }
    var selectedJobId by remember(run.id) { mutableStateOf<Long?>(null) }
    val jobViewModel: BuildJobDetailsViewModel = hiltViewModel()
    val jobState by jobViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(repository.id, run.id, selectedJobId) { selectedJobId?.let { jobViewModel.load(repository, run.id, it) } }
    val animatedHeight by animateFloatAsState(height.fraction, label = "build-detail-height")
    val detailHeight = if (preferences.reduceMotion) height.fraction else animatedHeight
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(detailHeight), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), tonalElevation = 10.dp) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Run details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }; IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }; IconButton(onClick = { height = when (height) { DetailHeight.Compact -> DetailHeight.Expanded; DetailHeight.Expanded -> DetailHeight.Full; DetailHeight.Full -> DetailHeight.Compact } }) { Icon(if (height == DetailHeight.Full) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Resize") }; IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } } }
                    item { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatusIcon(state, accent); Column(Modifier.weight(1f)) { Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.Bold); Text(state.name, color = accent, fontWeight = FontWeight.SemiBold) } }; DetailRow("Branch", run.headBranch.orEmpty()); DetailRow("Event", run.event); DetailRow("Run", "#${run.id}") } } }
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ clipboard.setText(AnnotatedString(run.htmlUrl)) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = run.htmlUrl.isNotBlank()) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy URL") }; OutlinedButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl))) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled = run.htmlUrl.isNotBlank()) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(4.dp)); Text("Open GitHub") } } }
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (state == WorkflowDisplayState.Running || state == WorkflowDisplayState.Queued) OutlinedButton(onCancel, Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(4.dp)); Text("Cancel") } else OutlinedButton(onRerun, Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Re-run") }; OutlinedButton(onRefresh, Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Default.Sync, null); Spacer(Modifier.width(4.dp)); Text("Refresh") } } }
                    item { Text("Jobs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    if (jobs.isEmpty()) item { Text("No jobs are available for this run.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else items(jobs, key = { it.id }) { job ->
                        val selected = selectedJobId == job.id
                        OutlinedCard(onClick = { selectedJobId = if (selected) null else job.id }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val failed = job.conclusion in failureConclusions; val passed = job.conclusion == "success"
                                Icon(if (failed) Icons.Default.ErrorOutline else if (passed) Icons.Default.CheckCircle else Icons.Default.Sync, null, tint = if (failed) MaterialTheme.colorScheme.error else if (passed) WorkflowHealthyGreen else MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) { Text(job.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${job.status} · ${job.conclusion ?: "in progress"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                                Icon(if (selected) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle job")
                            }
                        }
                    }
                    if (selectedJobId != null) {
                        if (jobState.loading) item { LoadingRow("Loading job and real logs…") }
                        jobState.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                        jobState.job?.let { job -> item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(job.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); job.steps.forEachIndexed { index, step -> Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(step.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text(step.conclusion ?: step.status, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) } }; jobState.logs?.let { logs -> Text("Logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); SelectionContainer { Text(logs, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }; OutlinedButton({ clipboard.setText(AnnotatedString(logs)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy logs") } } } } } }
                    if (artifacts.isNotEmpty()) { item { Text("Artifacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; items(artifacts, key = { it.id }) { artifact -> OutlinedButton({ onDownload(artifact) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(artifact.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("ZIP", style = MaterialTheme.typography.labelSmall) } } }
                    item { Text("Live GitHub Actions data. Refresh keeps this native detail layer open and preserves the current run.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge) } }
@Composable private fun statusColor(problem: Boolean, healthy: Boolean, preferences: AppearancePreferences): Color = when { !preferences.statusColors -> MaterialTheme.colorScheme.primary; problem -> MaterialTheme.colorScheme.error; healthy -> WorkflowHealthyGreen; else -> MaterialTheme.colorScheme.primary }
@Composable private fun runColor(state: WorkflowDisplayState, preferences: AppearancePreferences): Color = when (state) { WorkflowDisplayState.Success -> statusColor(false, true, preferences); WorkflowDisplayState.Failed -> statusColor(true, false, preferences); WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant; else -> MaterialTheme.colorScheme.primary }
private val failureConclusions = setOf("failure", "timed_out", "action_required", "startup_failure")
