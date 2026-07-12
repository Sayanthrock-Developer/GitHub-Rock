package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.core.util.AndroidWorkflowGenerator
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BuildsScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onSelectRepository: (GitHubRepositoryModel) -> Unit,
    viewModel: BuildsViewModel = hiltViewModel()
) {
    val actionState by viewModel.state.collectAsStateWithLifecycle()
    var selectedRepository by remember(repositories) { mutableStateOf(repositories.firstOrNull()) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Android Builds", style = MaterialTheme.typography.headlineSmall) }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CloudQueue, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Cloud builds with GitHub Actions", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Select a repository to inspect its workflows and runs. GitHub Rock never compiles large projects on your phone or stores signing secrets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mode == AppMode.Guest) Text("Connect GitHub to dispatch workflows.", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text("Choose a repository", style = MaterialTheme.typography.titleMedium) }
        items(repositories.take(8), key = { it.id }) { repo ->
            OutlinedButton(
                onClick = { selectedRepository = repo },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selectedRepository?.id == repo.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text(if (selectedRepository?.id == repo.id) "Selected • ${repo.fullName}" else repo.fullName)
            }
        }
        selectedRepository?.let { repo ->
            item { TextButton(onClick = { onSelectRepository(repo) }) { Text("Open ${repo.fullName}") } }
        }
        item {
            WorkflowPreviewCard(
                mode = mode,
                repository = selectedRepository,
                actionState = actionState,
                onCreatePullRequest = viewModel::createWorkflowPullRequest
            )
        }
        item { Text("Recent runs", style = MaterialTheme.typography.titleMedium) }
        if (runs.isEmpty()) item {
            GlassCard { Text("No workflow runs loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(runs, key = { it.id }) { run ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.SemiBold)
                    Text("${run.event} • ${run.headBranch.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.displayState().name, color = when (run.displayState().name) {
                        "Success" -> MaterialTheme.colorScheme.tertiary
                        "Failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    })
                }
            }
        }
    }
}

@Composable
private fun WorkflowPreviewCard(
    mode: AppMode,
    repository: GitHubRepositoryModel?,
    actionState: BuildsActionState,
    onCreatePullRequest: (GitHubRepositoryModel, String, String, AndroidArtifactType) -> Unit
) {
    var artifact by remember { mutableStateOf(AndroidArtifactType.DebugApk) }
    var module by remember { mutableStateOf("app") }
    var featureBranch by remember(repository?.id) { mutableStateOf("github-rock/android-build-${System.currentTimeMillis() / 1000}") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val yamlResult = remember(module, artifact) { runCatching { AndroidWorkflowGenerator.generate(module, artifact) } }
    val yaml = yamlResult.getOrDefault("")
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Workflow preview", style = MaterialTheme.typography.titleLarge)
            Text("Review the exact YAML before it is committed to a new branch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Repository: ${repository?.fullName ?: "Select a repository"}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = module, onValueChange = { module = it }, label = { Text("Android application module") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = featureBranch, onValueChange = { featureBranch = it }, label = { Text("Review branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AndroidArtifactType.entries.forEach { option ->
                    FilterChip(selected = artifact == option, onClick = { artifact = option }, label = { Text(option.name) })
                }
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background.copy(alpha = .75f)) {
                Text(yamlResult.exceptionOrNull()?.message ?: yaml, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, maxLines = 12, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(yaml)) }, Modifier.fillMaxWidth(), enabled = yaml.isNotBlank()) { Text("Copy workflow YAML") }
            Button(
                onClick = { repository?.let { onCreatePullRequest(it, featureBranch, yaml, artifact) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = mode == AppMode.Connected && repository != null && yaml.isNotBlank() && !actionState.loading
            ) { Text(if (actionState.loading) "Creating pull request…" else "Create branch and pull request") }
            if (mode != AppMode.Connected) Text("Connect GitHub to commit this workflow.", color = MaterialTheme.colorScheme.primary)
            actionState.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
            actionState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            actionState.pullRequestUrl?.let { url ->
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Text("Open pull request") }
            }
        }
    }
}
