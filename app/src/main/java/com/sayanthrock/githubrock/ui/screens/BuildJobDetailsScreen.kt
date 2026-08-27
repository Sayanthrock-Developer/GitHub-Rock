package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuildJobDetailsState(val loading: Boolean = false, val job: WorkflowJob? = null, val logs: String? = null, val error: String? = null)

@HiltViewModel
class BuildJobDetailsViewModel @Inject constructor(private val repository: GitHubRepository) : ViewModel() {
    private val _state = MutableStateFlow(BuildJobDetailsState())
    val state = _state.asStateFlow()

    fun load(repo: GitHubRepositoryModel, runId: Long, jobId: Long) = viewModelScope.launch {
        _state.value = BuildJobDetailsState(loading = true)
        runCatching {
            val job = repository.workflowJobs(repo.owner.login, repo.name, runId).firstOrNull { it.id == jobId }
            val logs = if (job != null) runCatching { repository.workflowJobLogs(repo.owner.login, repo.name, jobId) }.getOrNull() else null
            job to logs
        }.onSuccess { (job, logs) ->
            _state.value = BuildJobDetailsState(job = job, logs = logs, error = if (job == null) "Job details are unavailable." else null)
        }.onFailure { _state.value = BuildJobDetailsState(error = it.message ?: "Unable to load job details") }
    }
}

@Composable
fun BuildJobDetailsScreen(mode: AppMode, repository: GitHubRepositoryModel, runId: Long, jobId: Long, onBack: () -> Unit, viewModel: BuildJobDetailsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(repository.id, runId, jobId, mode) { if (mode == AppMode.Connected) viewModel.load(repository, runId, jobId) }
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = StandardScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column { Text("Job details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.job?.let { job ->
            item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(job.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${job.status} · ${job.conclusion ?: "in progress"}", color = MaterialTheme.colorScheme.primary); Text("Job ID: ${job.id}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            item { Text("Steps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(job.steps.size) { index -> val step = job.steps[index]; GlassCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${index + 1}. ${step.name}", Modifier.weight(1f)); Text(step.conclusion ?: step.status, color = if (step.conclusion == "failure") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } } }
        }
        state.logs?.let { logs ->
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(onClick = { clipboard.setText(AnnotatedString(logs)) }) { Icon(Icons.Default.ContentCopy, "Copy logs") } } }
            item { GlassCard { Text(logs, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } }
        }
    }
}
