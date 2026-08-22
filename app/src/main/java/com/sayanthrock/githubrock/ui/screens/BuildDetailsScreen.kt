package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding

@Composable
fun BuildDetailsScreen(
    mode: AppMode,
    repository: GitHubRepositoryModel,
    runId: Long,
    onBack: () -> Unit,
    viewModel: BuildsViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by appearanceViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(repository.id, runId, mode) {
        if (mode == AppMode.Connected) viewModel.loadAndroidBuild(repository, runId)
        else viewModel.resetBuild()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Build details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        state.error?.let { error -> item { StatusCard(error, true, preferences) } }
        state.message?.let { message -> item { StatusCard(message, false, preferences) } }

        state.run?.let { run ->
            item { BuildRunHeader(run, state.workflow?.name, preferences) }
            item { BuildMetadata(run) }
        } ?: item {
            GlassCard { Text(if (state.loading) "Loading build details…" else "Build run details are unavailable.") }
        }

        if (state.tracking) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        item {
            Text("Jobs & steps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (state.jobs.isEmpty()) {
            item { GlassCard { Text("No job details returned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(state.jobs, key = { it.id }) { job -> JobDetailsCard(job, preferences) }
        }

        item {
            Text("Artifacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (state.artifacts.isEmpty()) {
            item { GlassCard { Text("No downloadable artifacts were published for this run.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(state.artifacts, key = { it.id }) { artifact ->
                OutlinedButton(onClick = { /* Downloads are handled by the Builds screen action flow. */ }, modifier = Modifier.fillMaxWidth(), enabled = !artifact.expired) {
                    Icon(Icons.Default.Archive, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (artifact.expired) "${artifact.name} expired" else artifact.name)
                }
            }
        }
    }
}

@Composable
private fun BuildRunHeader(run: WorkflowRun, workflowName: String?, preferences: AppearancePreferences) {
    val state = run.displayState()
    val accent = buildRunColor(state, preferences)
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BuildStatusIcon(state, accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                workflowName?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("#${run.id}", color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BuildMetadata(run: WorkflowRun) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Metadata("Status", run.displayState().name)
            Metadata("Branch", run.headBranch)
            Metadata("Event", run.event)
            Metadata("Run", "#${run.id}")
        }
    }
}

@Composable
private fun Metadata(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.ifBlank { "—" } ?: "—")
    }
}

@Composable
private fun JobDetailsCard(job: WorkflowJob, preferences: AppearancePreferences) {
    var expanded by remember(job.id) { mutableStateOf(true) }
    val failed = job.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure")
    val passed = job.conclusion == "success"
    val accent = if (failed) MaterialTheme.colorScheme.error else if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = BorderStroke(1.dp, accent.copy(alpha = .35f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BuildStatusIcon(if (failed) WorkflowDisplayState.Failed else if (passed) WorkflowDisplayState.Success else WorkflowDisplayState.Running, accent)
                Column(Modifier.weight(1f)) {
                    Text(job.name, fontWeight = FontWeight.SemiBold)
                    Text(job.conclusion ?: job.status, color = accent, style = MaterialTheme.typography.labelMedium)
                }
                if (job.steps.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle steps")
                    }
                }
            }
            if (expanded) {
                job.steps.forEachIndexed { index, step ->
                    val stepFailed = step.conclusion in setOf("failure", "timed_out", "action_required", "startup_failure")
                    val stepPassed = step.conclusion == "success"
                    val stepAccent = if (stepFailed) MaterialTheme.colorScheme.error else if (stepPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}", Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(step.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(step.conclusion ?: step.status, color = stepAccent, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(message: String, error: Boolean, preferences: AppearancePreferences) {
    val accent = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .10f), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null, tint = accent)
            Text(message, color = accent)
        }
    }
}

@Composable
private fun BuildStatusIcon(state: WorkflowDisplayState, accent: Color) {
    Icon(
        when (state) {
            WorkflowDisplayState.Success -> Icons.Default.CheckCircle
            WorkflowDisplayState.Failed -> Icons.Default.ErrorOutline
            else -> Icons.Default.Sync
        },
        contentDescription = state.name,
        tint = accent
    )
}

@Composable
private fun buildRunColor(state: WorkflowDisplayState, preferences: AppearancePreferences): Color = when (state) {
    WorkflowDisplayState.Success -> MaterialTheme.colorScheme.primary
    WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error
    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.secondary
}
