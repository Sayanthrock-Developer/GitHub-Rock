package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubIssue
import com.sayanthrock.githubrock.core.model.PullRequestSummary
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.util.DiffLineKind
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.core.util.SyntaxHighlighter
import com.sayanthrock.githubrock.core.util.SyntaxTokenKind
import com.sayanthrock.githubrock.core.util.TextDiff
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
    var showCreatePull by remember { mutableStateOf(false) }
    var newPullTitle by remember { mutableStateOf("") }
    var newPullHead by remember { mutableStateOf("") }
    var newPullBody by remember { mutableStateOf("") }
    var issueStateAction by remember { mutableStateOf<GitHubIssue?>(null) }
    var showIssueMetadata by remember { mutableStateOf(false) }
    var issueLabelsDraft by remember { mutableStateOf("") }
    var issueAssigneesDraft by remember { mutableStateOf("") }
    var issueMilestoneDraft by remember { mutableStateOf("") }
    var showForkConfirmation by remember { mutableStateOf(false) }
    var showCreateRelease by remember { mutableStateOf(false) }
    var newReleaseTag by remember { mutableStateOf("") }
    var newReleaseName by remember { mutableStateOf("") }
    var newReleaseNotes by remember { mutableStateOf("") }
    var newReleasePrerelease by remember { mutableStateOf(false) }
    var deleteReleaseTarget by remember { mutableStateOf<Release?>(null) }
    var editReleaseTarget by remember { mutableStateOf<Release?>(null) }
    var editReleaseName by remember { mutableStateOf("") }
    var editReleaseNotes by remember { mutableStateOf("") }
    var editReleasePrerelease by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var newFilePath by remember { mutableStateOf("") }
    var fileMoveTarget by remember { mutableStateOf<ContentEntry?>(null) }
    var fileDeleteTarget by remember { mutableStateOf<ContentEntry?>(null) }
    var destinationPath by remember { mutableStateOf("") }
    var fileOperationBranch by remember { mutableStateOf("") }
    var fileOperationMessage by remember { mutableStateOf("") }
    LaunchedEffect(repository?.id, repository?.defaultBranch) {
        repository?.let { viewModel.setRepositoryDefaults(it.defaultBranch) }
    }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.setRepositoryStarred(true) }) { Text("Star") }
                                OutlinedButton(onClick = { viewModel.setRepositoryStarred(false) }) { Text("Unstar") }
                                OutlinedButton(onClick = { showForkConfirmation = true }) { Text("Fork") }
                            }
                            if (repository?.private == true) Text("Private repository", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                RepoSection.Code -> {
                    if (state.editor == null) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Branch: ${repository?.defaultBranch ?: "main"}", style = MaterialTheme.typography.titleMedium)
                                OutlinedButton(onClick = { showNewFile = true }) { Text("New text file") }
                            }
                        }
                        items(state.contents, key = { it.path }) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.name) },
                                supportingContent = { Text(entry.path) },
                                leadingContent = { Icon(if (entry.type == "dir") Icons.Default.Folder else Icons.Default.Description, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (entry.type == "dir") {
                                TextButton(onClick = { viewModel.openDirectory(entry.path) }) { Text("Open folder") }
                            } else {
                                TextButton(onClick = { viewModel.openFile(entry.path) }) { Text("Open file") }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        fileMoveTarget = entry
                                        destinationPath = entry.path
                                        fileOperationBranch = "github-rock/move-${System.currentTimeMillis() / 1000}"
                                        fileOperationMessage = "Move ${entry.path}"
                                    }) { Text("Rename / move") }
                                    TextButton(onClick = {
                                        fileDeleteTarget = entry
                                        fileOperationBranch = "github-rock/delete-${System.currentTimeMillis() / 1000}"
                                        fileOperationMessage = "Delete ${entry.path}"
                                    }) { Text("Delete") }
                                }
                            }
                        }
                    } else {
                        item {
                            CodeEditorCard(
                                editor = requireNotNull(state.editor),
                                loading = state.loading,
                                demo = repository?.id?.let { it < 0 } == true,
                                onContentChange = viewModel::updateEditorContent,
                                onClose = viewModel::closeEditor,
                                onSave = viewModel::saveEditor
                            )
                        }
                    }
                }
                RepoSection.Issues -> {
                    item { OutlinedButton(onClick = { showCreateIssue = true }, Modifier.fillMaxWidth()) { Text("New issue") } }
                    items(state.issues, key = { it.id }) { issue ->
                        SummaryCard("#${issue.number} ${issue.title}", "${issue.state} • ${issue.user.login} • ${issue.commentCount} comments")
                        TextButton(onClick = {
                            selectedIssue = issue
                            issueCommentDraft = ""
                            issueLabelsDraft = issue.labels.joinToString(", ") { it.name }
                            issueAssigneesDraft = issue.assignees.joinToString(", ") { it.login }
                            issueMilestoneDraft = issue.milestone?.number?.toString().orEmpty()
                            viewModel.loadIssueComments(issue.number)
                        }) { Text("Open issue") }
                    }
                }
                RepoSection.Pulls -> {
                    item { OutlinedButton(onClick = { showCreatePull = true }, Modifier.fillMaxWidth()) { Text("New pull request") } }
                    items(state.pulls, key = { it.id }) { pull ->
                        SummaryCard("#${pull.number} ${pull.title}", if (pull.draft) "Draft • ${pull.user.login}" else "${pull.state} • ${pull.user.login}")
                        TextButton(onClick = { selectedPull = pull; reviewDraft = ""; viewModel.loadPullReviews(pull.number) }) { Text("Open reviews") }
                        if (pull.state == "open" && pull.draft != true) {
                            TextButton(onClick = { mergePull = pull }) { Text("Merge…") }
                        }
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
                RepoSection.Releases -> {
                    item { OutlinedButton(onClick = { showCreateRelease = true }, Modifier.fillMaxWidth()) { Text("New draft release") } }
                    items(state.releases, key = { it.id }) { release ->
                        SummaryCard(release.name ?: release.tagName, "${release.tagName} • ${release.assets.size} assets")
                        TextButton(onClick = { expandedRelease = if (expandedRelease == release.id) null else release.id }) {
                            Text(if (expandedRelease == release.id) "Hide release notes" else "View release notes")
                        }
                        if (expandedRelease == release.id) {
                            Text(release.body?.ifBlank { "No release notes." } ?: "No release notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${if (release.draft) "Draft" else "Published"} • ${if (release.prerelease) "Prerelease" else "Stable"} • ${release.publishedAt ?: "Not published"}", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { editReleaseTarget = release; editReleaseName = release.name.orEmpty(); editReleaseNotes = release.body.orEmpty(); editReleasePrerelease = release.prerelease }) { Text("Edit release") }
                            TextButton(onClick = { deleteReleaseTarget = release }) { Text("Delete release") }
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
    selectedIssue?.let { selected ->
        val issue = state.issues.firstOrNull { it.id == selected.id } ?: selected
        AlertDialog(
            onDismissRequest = { selectedIssue = null },
            title = { Text("#${issue.number} ${issue.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(issue.body?.ifBlank { "No issue description." } ?: "No issue description.")
                    if (issue.labels.isNotEmpty()) Text("Labels: ${issue.labels.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                    if (issue.assignees.isNotEmpty()) Text("Assignees: ${issue.assignees.joinToString { it.login }}", style = MaterialTheme.typography.bodySmall)
                    Text("Milestone: ${issue.milestone?.title ?: "None"}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Reactions: +1 ${issue.reactions.plusOne} • heart ${issue.reactions.heart} • rocket ${issue.reactions.rocket} • eyes ${issue.reactions.eyes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("+1" to "+1", "heart" to "♥", "rocket" to "🚀", "eyes" to "👀").forEach { (reaction, label) ->
                            AssistChip(onClick = { viewModel.addIssueReaction(issue.number, reaction) }, label = { Text(label) })
                        }
                    }
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
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        issueLabelsDraft = issue.labels.joinToString(", ") { it.name }
                        issueAssigneesDraft = issue.assignees.joinToString(", ") { it.login }
                        issueMilestoneDraft = issue.milestone?.number?.toString().orEmpty()
                        showIssueMetadata = true
                    }) { Text("Edit metadata") }
                    TextButton(onClick = { viewModel.addIssueComment(issue.number, issueCommentDraft); issueCommentDraft = "" }) { Text("Comment") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { issueStateAction = issue }) { Text(if (issue.state == "open") "Close issue" else "Reopen") }
                    TextButton(onClick = { selectedIssue = null }) { Text("Close") }
                }
            }
        )
    }
    if (showIssueMetadata) {
        AlertDialog(
            onDismissRequest = { showIssueMetadata = false },
            title = { Text("Edit issue metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leave labels or assignees empty to clear them. Milestone accepts a number; separate multiple values with commas.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = issueLabelsDraft, onValueChange = { issueLabelsDraft = it }, label = { Text("Labels") }, singleLine = true)
                    OutlinedTextField(value = issueAssigneesDraft, onValueChange = { issueAssigneesDraft = it }, label = { Text("Assignees (usernames)") }, singleLine = true)
                    OutlinedTextField(value = issueMilestoneDraft, onValueChange = { issueMilestoneDraft = it }, label = { Text("Milestone number") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedIssue?.let { issue -> viewModel.updateIssueMetadata(issue.number, issueLabelsDraft, issueAssigneesDraft, issueMilestoneDraft) }
                    showIssueMetadata = false
                }) { Text("Save metadata") }
            },
            dismissButton = { TextButton(onClick = { showIssueMetadata = false }) { Text("Cancel") } }
        )
    }
    issueStateAction?.let { issue ->
        val closing = issue.state == "open"
        AlertDialog(
            onDismissRequest = { issueStateAction = null },
            title = { Text(if (closing) "Close issue #${issue.number}?" else "Reopen issue #${issue.number}?") },
            text = { Text(if (closing) "This changes the issue state on GitHub." else "This restores the issue to open state on GitHub.") },
            confirmButton = { TextButton(onClick = { issueStateAction = null; viewModel.updateIssueState(issue.number, if (closing) "closed" else "open"); selectedIssue = null }) { Text(if (closing) "Close issue" else "Reopen") } },
            dismissButton = { TextButton(onClick = { issueStateAction = null }) { Text("Cancel") } }
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
    if (showNewFile) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title = { Text("New text file") },
            text = {
                OutlinedTextField(
                    value = newFilePath,
                    onValueChange = { newFilePath = it },
                    label = { Text("Relative path") },
                    placeholder = { Text("docs/notes.md") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.startNewFile(newFilePath.trim()); newFilePath = ""; showNewFile = false }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFile = false }) { Text("Cancel") } }
        )
    }
    if (showCreatePull) {
        AlertDialog(
            onDismissRequest = { showCreatePull = false },
            title = { Text("New pull request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newPullTitle, onValueChange = { newPullTitle = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = newPullHead, onValueChange = { newPullHead = it }, label = { Text("Source branch") }, singleLine = true)
                    Text("Base branch: ${repository?.defaultBranch ?: "main"}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = newPullBody, onValueChange = { newPullBody = it }, label = { Text("Description (optional)") }, minLines = 3, maxLines = 6)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.createPullRequest(newPullTitle, newPullHead, repository?.defaultBranch ?: "main", newPullBody); newPullTitle = ""; newPullHead = ""; newPullBody = ""; showCreatePull = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreatePull = false }) { Text("Cancel") } }
        )
    }
    if (showForkConfirmation) {
        AlertDialog(
            onDismissRequest = { showForkConfirmation = false },
            title = { Text("Fork ${repository?.name ?: "repository"}?") },
            text = { Text("GitHub will create a separate repository copy in your account. This does not change the original repository.") },
            confirmButton = { TextButton(onClick = { showForkConfirmation = false; viewModel.forkRepository() }) { Text("Fork") } },
            dismissButton = { TextButton(onClick = { showForkConfirmation = false }) { Text("Cancel") } }
        )
    }
    if (showCreateRelease) {
        AlertDialog(
            onDismissRequest = { showCreateRelease = false },
            title = { Text("New draft release") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newReleaseTag, onValueChange = { newReleaseTag = it }, label = { Text("Tag") }, singleLine = true)
                    OutlinedTextField(value = newReleaseName, onValueChange = { newReleaseName = it }, label = { Text("Release name (optional)") }, singleLine = true)
                    OutlinedTextField(value = newReleaseNotes, onValueChange = { newReleaseNotes = it }, label = { Text("Release notes (optional)") }, minLines = 3, maxLines = 6)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = newReleasePrerelease, onCheckedChange = { newReleasePrerelease = it })
                        Text("Mark as prerelease")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.createDraftRelease(newReleaseTag, newReleaseName, newReleaseNotes, newReleasePrerelease); newReleaseTag = ""; newReleaseName = ""; newReleaseNotes = ""; newReleasePrerelease = false; showCreateRelease = false }) { Text("Create draft") } },
            dismissButton = { TextButton(onClick = { showCreateRelease = false }) { Text("Cancel") } }
        )
    }
    deleteReleaseTarget?.let { release ->
        AlertDialog(
            onDismissRequest = { deleteReleaseTarget = null },
            title = { Text("Delete ${release.tagName}?") },
            text = { Text("This permanently deletes the GitHub release. Release assets may no longer be available to users.") },
            confirmButton = { TextButton(onClick = { deleteReleaseTarget = null; viewModel.deleteRelease(release.id) }) { Text("Delete release") } },
            dismissButton = { TextButton(onClick = { deleteReleaseTarget = null }) { Text("Cancel") } }
        )
    }
    editReleaseTarget?.let { release ->
        AlertDialog(
            onDismissRequest = { editReleaseTarget = null },
            title = { Text("Edit ${release.tagName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editReleaseName, onValueChange = { editReleaseName = it }, label = { Text("Release name") }, singleLine = true)
                    OutlinedTextField(value = editReleaseNotes, onValueChange = { editReleaseNotes = it }, label = { Text("Release notes") }, minLines = 3, maxLines = 6)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = editReleasePrerelease, onCheckedChange = { editReleasePrerelease = it })
                        Text("Mark as prerelease")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.updateRelease(release.id, editReleaseName, editReleaseNotes, release.draft, editReleasePrerelease); editReleaseTarget = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editReleaseTarget = null }) { Text("Cancel") } }
        )
    }
    fileMoveTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { fileMoveTarget = null },
            title = { Text("Rename or move file?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The file will be copied to the destination and the original removed on a new review branch.")
                    OutlinedTextField(value = destinationPath, onValueChange = { destinationPath = it }, label = { Text("Destination path") }, singleLine = true)
                    OutlinedTextField(value = fileOperationBranch, onValueChange = { fileOperationBranch = it }, label = { Text("Review branch") }, singleLine = true)
                    OutlinedTextField(value = fileOperationMessage, onValueChange = { fileOperationMessage = it }, label = { Text("Commit message") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameOrMoveFile(entry.path, destinationPath, fileOperationBranch, fileOperationMessage)
                    fileMoveTarget = null
                }) { Text("Open pull request") }
            },
            dismissButton = { TextButton(onClick = { fileMoveTarget = null }) { Text("Cancel") } }
        )
    }
    fileDeleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { fileDeleteTarget = null },
            title = { Text("Delete ${entry.path}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GitHub Rock will create a review branch and pull request. The default branch will not be changed automatically.")
                    OutlinedTextField(value = fileOperationBranch, onValueChange = { fileOperationBranch = it }, label = { Text("Review branch") }, singleLine = true)
                    OutlinedTextField(value = fileOperationMessage, onValueChange = { fileOperationMessage = it }, label = { Text("Commit message") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(entry.path, fileOperationBranch, fileOperationMessage)
                    fileDeleteTarget = null
                }) { Text("Open pull request") }
            },
            dismissButton = { TextButton(onClick = { fileDeleteTarget = null }) { Text("Cancel") } }
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

@Composable
private fun CodeEditorCard(
    editor: FileEditorState,
    loading: Boolean,
    demo: Boolean,
    onContentChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var featureBranch by remember(editor.path) { mutableStateOf("github-rock/edit-${System.currentTimeMillis() / 1000}") }
    var commitMessage by remember(editor.path) { mutableStateOf("Update ${editor.path}") }
    var showDiff by remember(editor.path) { mutableStateOf(true) }
    var showMarkdownPreview by remember(editor.path) { mutableStateOf(false) }
    var showSyntaxPreview by remember(editor.path) { mutableStateOf(true) }
    val changed = editor.content != editor.originalContent
    val isMarkdown = editor.path.endsWith(".md", ignoreCase = true) || editor.path.endsWith(".markdown", ignoreCase = true)
    val markdownBlocks = remember(editor.content) { MarkdownRenderer.render(editor.content) }
    val diff = remember(editor.originalContent, editor.content) {
        TextDiff.unified(editor.originalContent, editor.content)
    }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(editor.path, style = MaterialTheme.typography.titleLarge)
            Text(
                if (editor.branchProtected) "Protected default branch: changes require review."
                else "Changes are still isolated on a new branch before the pull request.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("${editor.content.length} characters • ${if (changed) "unsaved changes" else "unchanged"}", style = MaterialTheme.typography.bodySmall)
            if (isMarkdown) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showMarkdownPreview = !showMarkdownPreview }) {
                        Text(if (showMarkdownPreview) "Edit Markdown" else "Preview Markdown")
                    }
                    if (changed) TextButton(onClick = { showDiff = !showDiff }) { Text(if (showDiff) "Hide diff" else "Show diff") }
                }
            }
            if (showMarkdownPreview && isMarkdown) {
                MarkdownPreviewCard(markdownBlocks)
            } else {
                TextButton(onClick = { showSyntaxPreview = !showSyntaxPreview }) {
                    Text(if (showSyntaxPreview) "Hide syntax preview" else "Show syntax preview")
                }
                if (showSyntaxPreview) SyntaxPreviewCard(editor.path, editor.content)
                if (changed) {
                    if (!isMarkdown) {
                        TextButton(onClick = { showDiff = !showDiff }) {
                            Text(if (showDiff) "Hide diff" else "Show diff")
                        }
                    }
                    if (showDiff) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            diff.take(MAX_VISIBLE_DIFF_LINES).forEach { line ->
                                val background = when (line.kind) {
                                    DiffLineKind.Added -> MaterialTheme.colorScheme.tertiary.copy(alpha = .18f)
                                    DiffLineKind.Removed -> MaterialTheme.colorScheme.error.copy(alpha = .18f)
                                    DiffLineKind.Context -> androidx.compose.ui.graphics.Color.Transparent
                                }
                                Text(
                                    text = "${line.prefix()}${line.text}",
                                    modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                )
                            }
                            if (diff.size > MAX_VISIBLE_DIFF_LINES) {
                                Text("Diff truncated for readability.", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    }
                }
                OutlinedTextField(
                    value = editor.content,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 16,
                    maxLines = 28,
                    textStyle = LocalTextStyle.current.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    label = { Text("File contents") }
                )
            }
            OutlinedTextField(
                value = featureBranch,
                onValueChange = { featureBranch = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Review branch") }
            )
            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Commit message") }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Close") }
                Button(
                    onClick = { onSave(featureBranch, commitMessage) },
                    enabled = !demo && changed && editor.pullRequestUrl == null && !loading && featureBranch.isNotBlank() && commitMessage.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text(if (loading) "Saving…" else if (editor.pullRequestUrl != null) "Pull request created" else "Open pull request") }
            }
            if (demo) Text("Demo mode never commits to GitHub.", color = MaterialTheme.colorScheme.primary)
            editor.pullRequestUrl?.let { url ->
                val context = LocalContext.current
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Text("Open created pull request") }
            }
        }
    }
}

@Composable
private fun SyntaxPreviewCard(path: String, source: String) {
    val spans = remember(path, source) { SyntaxHighlighter.highlight(path, source) }
    val annotated = AnnotatedString.Builder(source).apply {
        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), 0, source.length)
        spans.forEach { span ->
            addStyle(SpanStyle(color = syntaxColor(span.kind)), span.start, span.end)
        }
    }.toAnnotatedString()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
    ) {
        SelectionContainer {
            Text(
                annotated,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 18,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun syntaxColor(kind: SyntaxTokenKind): Color = when (kind) {
    SyntaxTokenKind.Keyword -> MaterialTheme.colorScheme.primary
    SyntaxTokenKind.String -> MaterialTheme.colorScheme.tertiary
    SyntaxTokenKind.Comment -> MaterialTheme.colorScheme.onSurfaceVariant
    SyntaxTokenKind.Number -> MaterialTheme.colorScheme.secondary
    SyntaxTokenKind.Type -> MaterialTheme.colorScheme.primary.copy(alpha = .9f)
    SyntaxTokenKind.Tag -> MaterialTheme.colorScheme.primary
    SyntaxTokenKind.Attribute -> MaterialTheme.colorScheme.secondary
    SyntaxTokenKind.Property -> MaterialTheme.colorScheme.primary
    SyntaxTokenKind.Markdown -> MaterialTheme.colorScheme.primary
}

@Composable
private fun MarkdownPreviewCard(blocks: List<com.sayanthrock.githubrock.core.util.MarkdownBlock>) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block.kind) {
                    MarkdownBlockKind.Heading -> Text(
                        block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        }
                    )
                    MarkdownBlockKind.Bullet -> Text("• ${block.text}")
                    MarkdownBlockKind.Quote -> Text("> ${block.text}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MarkdownBlockKind.Code -> Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .7f)) {
                        Text(block.text, Modifier.fillMaxWidth().padding(10.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    MarkdownBlockKind.Divider -> HorizontalDivider()
                    MarkdownBlockKind.Paragraph -> Text(block.text)
                }
            }
        }
    }
}

private fun com.sayanthrock.githubrock.core.util.DiffLine.prefix(): String = when (kind) {
    DiffLineKind.Added -> "+ "
    DiffLineKind.Removed -> "- "
    DiffLineKind.Context -> "  "
}

private const val MAX_VISIBLE_DIFF_LINES = 240
