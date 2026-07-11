package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.PullRequestSummary
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RepositoryDetailScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    viewModel: RepositoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mergePull by remember { mutableStateOf<PullRequestSummary?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(repository?.fullName ?: "Repository") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = .92f))
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RepoSection.entries.forEach { section ->
                FilterChip(selected = state.section == section, onClick = { viewModel.select(section) }, label = { Text(section.title) })
            }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state.section) {
                RepoSection.Overview -> item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(repository?.name ?: "Repository", style = MaterialTheme.typography.titleLarge)
                            Text(repository?.description ?: "No repository description.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Default branch: ${repository?.defaultBranch ?: "main"}")
                            Text("${repository?.stars ?: 0} stars • ${repository?.forks ?: 0} forks • ${repository?.openIssues ?: 0} open issues")
                            if (repository?.private == true) Text("Private repository", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                RepoSection.Code -> items(state.contents, key = { it.sha }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.name) },
                        supportingContent = { Text(entry.path) },
                        leadingContent = { Icon(if (entry.type == "dir") Icons.Default.Folder else Icons.Default.Description, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (entry.type == "dir") TextButton(onClick = { viewModel.openDirectory(entry.path) }) { Text("Open folder") }
                }
                RepoSection.Issues -> items(state.issues, key = { it.id }) { issue -> SummaryCard("#${issue.number} ${issue.title}", "${issue.state} • ${issue.user.login}") }
                RepoSection.Pulls -> items(state.pulls, key = { it.id }) { pull ->
                    SummaryCard("#${pull.number} ${pull.title}", if (pull.draft) "Draft • ${pull.user.login}" else "${pull.state} • ${pull.user.login}")
                    if (pull.state == "open" && pull.draft != true) {
                        TextButton(onClick = { mergePull = pull }) { Text("Merge…") }
                    }
                }
                RepoSection.Actions -> {
                    items(state.workflows, key = { it.id }) { workflow -> SummaryCard(workflow.name, workflow.path) }
                    items(state.runs, key = { it.id }) { run -> SummaryCard(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, "${run.status} • ${run.conclusion ?: "pending"}") }
                    items(state.jobs, key = { it.id }) { job ->
                        SummaryCard(job.name, "${job.status} • ${job.conclusion ?: "running"} • ${job.steps.size} steps")
                    }
                    items(state.artifacts, key = { it.id }) { artifact ->
                        SummaryCard(artifact.name, "${artifact.sizeBytes / 1_048_576} MB • ${if (artifact.expired) "expired" else "available"}")
                    }
                }
                RepoSection.Releases -> items(state.releases, key = { it.id }) { release -> SummaryCard(release.name ?: release.tagName, "${release.tagName} • ${release.assets.size} assets") }
            }
        }
    }
    mergePull?.let { pull ->
        AlertDialog(
            onDismissRequest = { mergePull = null },
            title = { Text("Merge pull request #${pull.number}?") },
            text = { Text("GitHub will apply the default merge method. This action cannot be silently undone.") },
            confirmButton = {
                TextButton(onClick = { mergePull = null; viewModel.mergePullRequest(pull.number) }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { mergePull = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryCard(title: String, subtitle: String) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
