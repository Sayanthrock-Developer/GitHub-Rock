package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubIssue
import com.sayanthrock.githubrock.core.model.PullRequestSummary
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RepositoryDetailScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    viewModel: RepositoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    var mergePull by remember { mutableStateOf<PullRequestSummary?>(null) }
    var logJob by remember { mutableStateOf<WorkflowJob?>(null) }
    var runAction by remember { mutableStateOf<WorkflowRun?>(null) }
    var expandedRelease by remember { mutableStateOf<Long?>(null) }
    var dispatchWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var selectedIssue by remember { mutableStateOf<GitHubIssue?>(null) }
    var issueCommentDraft by remember { mutableStateOf("") }
    var selectedPull by remember { mutableStateOf<PullRequestSummary?>(null) }
    var reviewDraft by remember { mutableStateOf("") }
    var showCreateIssue by remember { mutableStateOf(false) }
    var newIssueTitle by remember { mutableStateOf("") }
    var newIssueBody by remember { mutableStateOf("") }
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
                RepoSection.Issues -> {
                    item { OutlinedButton(onClick = { showCreateIssue = true }, Modifier.fillMaxWidth()) { Text("New issue") } }
                    items(state.issues, key = { it.id }) { issue ->
                        SummaryCard("#${issue.number} ${issue.title}", "${issue.state} • ${issue.user.login} • ${issue.commentCount} comments")
                        TextButton(onClick = { selectedIssue = issue; issueCommentDraft = ""; viewModel.loadIssueComments(issue.number) }) { Text("Open issue") }
                    }
                }
                RepoSection.Pulls -> items(state.pulls, key = { it.id }) { pull ->
                    SummaryCard("#${pull.number} ${pull.title}", if (pull.draft) "Draft • ${pull.user.login}" else "${pull.state} • ${pull.user.login}")
                    TextButton(onClick = { selectedPull = pull; reviewDraft = ""; viewModel.loadPullReviews(pull.number) }) { Text("Open reviews") }
                    if (pull.state == "open" && pull.draft != true) {
                        TextButton(onClick = { mergePull = pull }) { Text("Merge…") }
                    }
                }
                RepoSection.Actions -> {
                    items(state.workflows, key = { it.id }) { workflow ->
                        SummaryCard(workflow.name, workflow.path)
                        TextButton(onClick = { dispatchWorkflow = workflow }) { Text("Run workflow") }
                    }
                    items(state.runs, key = { it.id }) { run ->
                        SummaryCard(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, "${run.status} • ${run.conclusion ?: "pending"}")
                        if (run.htmlUrl.isNotBlank()) TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl))) }) { Text("Open on GitHub") }
                        if (run.status == "in_progress" || run.status == "queued") TextButton(onClick = { runAction = run }) { Text("Cancel") }
                        if (run.conclusion == "failure" || run.conclusion == "cancelled") TextButton(onClick = { runAction = run }) { Text("Rerun") }
                    }
                    items(state.jobs, key = { it.id }) { job ->
                        SummaryCard(job.name, "${job.status} • ${job.conclusion ?: "running"} • ${job.steps.size} steps")
                        TextButton(onClick = { logJob = job; viewModel.loadJobLog(job.id) }) { Text("View logs") }
                    }
                    items(state.artifacts, key = { it.id }) { artifact ->
                        SummaryCard(artifact.name, "${artifact.sizeBytes / 1_048_576} MB • ${if (artifact.expired) "expired" else "available"}")
                        if (!artifact.expired) {
                            TextButton(onClick = {
                                downloadsViewModel.enqueue(artifact.archiveDownloadUrl, "${artifact.name}-${artifact.id}.zip")
                            }) { Text("Download artifact") }
                        }
                    }
                }
                RepoSection.Releases -> items(state.releases, key = { it.id }) { release ->
                    SummaryCard(release.name ?: release.tagName, "${release.tagName} • ${release.assets.size} assets")
                    TextButton(onClick = { expandedRelease = if (expandedRelease == release.id) null else release.id }) {
                        Text(if (expandedRelease == release.id) "Hide release notes" else "View release notes")
                    }
                    if (expandedRelease == release.id) {
                        Text(release.body?.ifBlank { "No release notes." } ?: "No release notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${if (release.draft) "Draft" else "Published"} • ${if (release.prerelease) "Prerelease" else "Stable"} • ${release.publishedAt ?: "Not published"}", style = MaterialTheme.typography.bodySmall)
                    }
                    release.assets.forEach { asset ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(asset.name, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                downloadsViewModel.enqueue(asset.downloadUrl, asset.name)
                            }) { Text("Download") }
                        }
                    }
                }
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
    logJob?.let { job ->
        AlertDialog(
            onDismissRequest = { logJob = null },
            title = { Text("Logs • ${job.name}") },
            text = { Text(state.jobLog ?: "Loading workflow logs…", style = MaterialTheme.typography.bodySmall, maxLines = 18, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            confirmButton = { TextButton(onClick = { logJob = null }) { Text("Close") } }
        )
    }
    runAction?.let { run ->
        val cancel = run.status == "in_progress" || run.status == "queued"
        AlertDialog(
            onDismissRequest = { runAction = null },
            title = { Text(if (cancel) "Cancel workflow?" else "Rerun workflow?") },
            text = { Text(if (cancel) "GitHub will stop this workflow run if cancellation is permitted." else "GitHub will start another run using the same workflow revision.") },
            confirmButton = { TextButton(onClick = { runAction = null; if (cancel) viewModel.cancelWorkflow(run.id) else viewModel.rerunWorkflow(run.id) }) { Text(if (cancel) "Cancel workflow" else "Rerun") } },
            dismissButton = { TextButton(onClick = { runAction = null }) { Text("Keep") } }
        )
    }
    dispatchWorkflow?.let { workflow ->
        AlertDialog(
            onDismissRequest = { dispatchWorkflow = null },
            title = { Text("Run ${workflow.name}?") },
            text = { Text("GitHub will dispatch this workflow on the repository default branch. Any required workflow inputs must be configured in GitHub.") },
            confirmButton = { TextButton(onClick = { dispatchWorkflow = null; viewModel.dispatchWorkflow(workflow.id, repository?.defaultBranch ?: "main") }) { Text("Run") } },
            dismissButton = { TextButton(onClick = { dispatchWorkflow = null }) { Text("Cancel") } }
        )
    }
    selectedIssue?.let { issue ->
        AlertDialog(
            onDismissRequest = { selectedIssue = null },
            title = { Text("#${issue.number} ${issue.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(issue.body?.ifBlank { "No issue description." } ?: "No issue description.")
                    if (issue.labels.isNotEmpty()) Text("Labels: ${issue.labels.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                    Text("Comments", fontWeight = FontWeight.SemiBold)
                    if (state.issueComments.isEmpty()) Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.issueComments.takeLast(4).forEach { comment ->
                        Text("${comment.user.login}: ${comment.body}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = issueCommentDraft,
                        onValueChange = { issueCommentDraft = it },
                        label = { Text("Add a comment") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addIssueComment(issue.number, issueCommentDraft); issueCommentDraft = "" }) { Text("Comment") } },
            dismissButton = { TextButton(onClick = { selectedIssue = null }) { Text("Close") } }
        )
    }
    selectedPull?.let { pull ->
        AlertDialog(
            onDismissRequest = { selectedPull = null },
            title = { Text("#${pull.number} ${pull.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reviews", fontWeight = FontWeight.SemiBold)
                    if (state.pullReviews.isEmpty()) Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.pullReviews.takeLast(4).forEach { review ->
                        Text("${review.user.login}: ${review.state}${review.body?.let { " • $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = reviewDraft,
                        onValueChange = { reviewDraft = it },
                        label = { Text("Review comment (optional)") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.submitPullReview(pull.number, "APPROVE", reviewDraft); reviewDraft = "" }) { Text("Approve") }
                    TextButton(onClick = { viewModel.submitPullReview(pull.number, "REQUEST_CHANGES", reviewDraft); reviewDraft = "" }) { Text("Request changes") }
                }
            },
            dismissButton = { TextButton(onClick = { selectedPull = null }) { Text("Close") } }
        )
    }
    if (showCreateIssue) {
        AlertDialog(
            onDismissRequest = { showCreateIssue = false },
            title = { Text("New issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newIssueTitle, onValueChange = { newIssueTitle = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = newIssueBody, onValueChange = { newIssueBody = it }, label = { Text("Description (optional)") }, minLines = 3, maxLines = 6)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.createIssue(newIssueTitle, newIssueBody); newIssueTitle = ""; newIssueBody = ""; showCreateIssue = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreateIssue = false }) { Text("Cancel") } }
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
