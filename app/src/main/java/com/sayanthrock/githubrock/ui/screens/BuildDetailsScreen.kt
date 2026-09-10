package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.model.formatRunTime
import com.sayanthrock.githubrock.core.model.runTime
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.RockIcon.vector
import com.sayanthrock.githubrock.ui.icons.vector
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun BuildDetailsScreen(mode: AppMode, repository: GitHubRepositoryModel, runId: Long, onBack: () -> Unit, onOpenJob: (Long, Long) -> Unit = { _, _ -> }, onOpenArtifact: (Long, Long) -> Unit = { _, _ -> }, viewModel: BuildsViewModel = hiltViewModel(), appearanceViewModel: AppearanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val preferences by appearanceViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(repository.id, runId, mode) { if (mode == AppMode.Connected) viewModel.loadAndroidBuild(repository, runId) else viewModel.resetBuild() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = StandardScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), "Back") }; Column(Modifier.weight(1f)) { Text("Build details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        state.error?.let { item { StatusCard(it, true) } }; state.message?.let { item { StatusCard(it, false) } }
        state.run?.let { run ->
            item { BuildRunHeader(run, state.workflow?.name, preferences) }
            item { BuildActionRow(run, !state.loading, { viewModel.loadAndroidBuild(repository, run.id) }, { viewModel.cancelRun(repository, run.id) }, { viewModel.rerunRun(repository, run.id) }) }
            item { BuildMetadata(run) }
            item { RunUsageCard(run) }
        } ?: item { GlassCard { Text(if (state.loading) "Loading build details…" else "Build run details are unavailable.") } }
        if (state.tracking) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { Text("Jobs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.jobs.isEmpty()) item { GlassCard { Text("No job details returned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } else items(state.jobs, key = { it.id }) { job -> JobDetailsCard(job, preferences) { onOpenJob(runId, job.id) } }
        item { Text("Artifacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.artifacts.isEmpty()) item { GlassCard { Text("No downloadable artifacts were published for this run.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } else items(state.artifacts, key = { it.id }) { artifact -> OutlinedButton(onClick = { onOpenArtifact(runId, artifact.id) }, modifier = Modifier.fillMaxWidth()) { Icon(RockIcon.Archive.vector(), null); Spacer(Modifier.width(8.dp)); Text(if (artifact.expired) "${artifact.name} expired" else artifact.name) } }
    }
}

@Composable
private fun RunUsageCard(run: WorkflowRun) {
    val running = run.displayState() == WorkflowDisplayState.Running || run.displayState() == WorkflowDisplayState.Queued
    var now by remember(run.id) { mutableStateOf(Instant.now()) }
    LaunchedEffect(run.id, running) { if (running) { while (true) { now = Instant.now(); delay(1000) } } }
    val duration = run.runTime(now)
    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(RockIcon.Timer.vector(), null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text("Usage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Run details", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(RockIcon.Timer.vector(), null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text("Run time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(duration?.formatRunTime() ?: if (run.displayState() == WorkflowDisplayState.Queued) "Not started" else "Unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }; if (running && duration != null) Text("Live", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        }
    } }
}

@Composable private fun BuildActionRow(run: WorkflowRun, enabled: Boolean, onRefresh: () -> Unit, onCancel: () -> Unit, onRerun: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onRefresh, enabled = enabled, modifier = Modifier.weight(1f)) { Icon(RockIcon.Refresh.vector(), null); Spacer(Modifier.width(6.dp)); Text("Refresh") }; if (run.displayState() == WorkflowDisplayState.Queued || run.displayState() == WorkflowDisplayState.Running) Button(onClick = onCancel, enabled = enabled, modifier = Modifier.weight(1f)) { Icon(RockIcon.Stop.vector(), null); Spacer(Modifier.width(6.dp)); Text("Cancel") } else Button(onClick = onRerun, enabled = enabled, modifier = Modifier.weight(1f)) { Icon(RockIcon.Refresh.vector(), null); Spacer(Modifier.width(6.dp)); Text("Re-run") } } }
@Composable private fun BuildRunHeader(run: WorkflowRun, workflowName: String?, preferences: AppearancePreferences) { val state = run.displayState(); val accent = buildRunColor(state); GlassCard { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { BuildStatusIcon(state, accent); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); workflowName?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Text("#${run.id}", color = accent, fontWeight = FontWeight.Bold) } } }
@Composable private fun BuildMetadata(run: WorkflowRun) { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Metadata("Status", run.displayState().name); Metadata("Branch", run.headBranch); Metadata("Event", run.event); Metadata("Run", "#${run.id}") } } }
@Composable private fun Metadata(label: String, value: String?) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value?.ifBlank { "—" } ?: "—") } }
@Composable private fun JobDetailsCard(job: WorkflowJob, preferences: AppearancePreferences, onClick: () -> Unit) { var expanded by remember(job.id) { mutableStateOf(true) }; val failed = job.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure"); val passed = job.conclusion == "success"; val accent = if (failed) MaterialTheme.colorScheme.error else if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary; Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { BuildStatusIcon(if (failed) WorkflowDisplayState.Failed else if (passed) WorkflowDisplayState.Success else null, accent); Column(Modifier.weight(1f)) { Text(job.name, fontWeight = FontWeight.SemiBold); Text(job.conclusion ?: job.status, color = accent, style = MaterialTheme.typography.labelMedium) }; if (job.steps.isNotEmpty()) IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) RockIcon.ExpandLess.vector() else RockIcon.ExpandMore.vector(), "Toggle steps") } }; if (expanded) job.steps.forEachIndexed { index, step -> val stepFailed = step.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure"); val stepPassed = step.conclusion == "success"; val stepAccent = if (stepFailed) MaterialTheme.colorScheme.error else if (stepPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("${index + 1}", Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(step.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text(step.conclusion ?: step.status, color = stepAccent, style = MaterialTheme.typography.labelSmall) } } } } }
@Composable private fun StatusCard(message: String, error: Boolean) { val accent = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .10f), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (error) RockIcon.Error.vector() else RockIcon.Check.vector(), null, tint = accent); Text(message, color = accent) } } }
@Composable private fun BuildStatusIcon(state: WorkflowDisplayState?, accent: Color) { Icon(when (state) { WorkflowDisplayState.Success -> RockIcon.Check.vector(); WorkflowDisplayState.Failed -> RockIcon.Error.vector(); else -> RockIcon.Sync.vector() }, contentDescription = state?.name ?: "Running", tint = accent) }
@Composable private fun buildRunColor(state: WorkflowDisplayState): Color = when (state) { WorkflowDisplayState.Success -> MaterialTheme.colorScheme.primary; WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error; WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant; else -> MaterialTheme.colorScheme.secondary }