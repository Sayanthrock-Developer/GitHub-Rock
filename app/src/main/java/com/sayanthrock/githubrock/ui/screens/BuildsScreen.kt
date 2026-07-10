package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun BuildsScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onSelectRepository: (GitHubRepositoryModel) -> Unit
) {
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
            OutlinedButton(onClick = { onSelectRepository(repo) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text(repo.fullName)
            }
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

