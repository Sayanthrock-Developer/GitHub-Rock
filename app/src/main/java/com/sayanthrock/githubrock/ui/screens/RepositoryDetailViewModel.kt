package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepoSection(val title: String) { Overview("Overview"), Code("Code"), Issues("Issues"), Pulls("Pull Requests"), Actions("Actions"), Releases("Releases") }

data class RepositoryDetailState(
    val section: RepoSection = RepoSection.Overview,
    val loading: Boolean = false,
    val contents: List<ContentEntry> = emptyList(),
    val issues: List<GitHubIssue> = emptyList(),
    val pulls: List<PullRequestSummary> = emptyList(),
    val workflows: List<Workflow> = emptyList(),
    val runs: List<WorkflowRun> = emptyList(),
    val jobs: List<WorkflowJob> = emptyList(),
    val artifacts: List<WorkflowArtifact> = emptyList(),
    val releases: List<Release> = emptyList(),
    val issueComments: List<IssueComment> = emptyList(),
    val pullReviews: List<PullRequestReview> = emptyList(),
    val currentPath: String = "",
    val editor: FileEditorState? = null,
    val error: String? = null,
    val message: String? = null,
    val jobLog: String? = null
)

data class FileEditorState(
    val path: String,
    val sha: String?,
    val content: String,
    val originalContent: String,
    val branchProtected: Boolean,
    val pullRequestUrl: String? = null
)

@HiltViewModel
class RepositoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GitHubRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repo: String = checkNotNull(savedStateHandle["repo"])
    private val demo: Boolean = savedStateHandle["demo"] ?: false
    private var baseBranch: String = "main"
    private val _state = MutableStateFlow(RepositoryDetailState())
    val state: StateFlow<RepositoryDetailState> = _state.asStateFlow()

    fun setRepositoryDefaults(defaultBranch: String) {
        if (defaultBranch.isNotBlank()) baseBranch = defaultBranch
    }

    fun select(section: RepoSection) {
        _state.update { it.copy(section = section, error = null) }
        if (section != RepoSection.Overview) load(section)
    }

    fun openDirectory(path: String) {
        _state.update { it.copy(currentPath = path, section = RepoSection.Code) }
        load(RepoSection.Code)
    }

    fun openFile(path: String) = viewModelScope.launch {
        if (path.isBlank()) return@launch
        _state.update { it.copy(loading = true, error = null, message = null, editor = null) }
        try {
            val entry = if (demo) {
                DemoData.contents.firstOrNull { it.path == path }
                    ?: error("Demo file was not found")
            } else {
                repository.file(owner, repo, path, baseBranch)
            }
            if (entry.type != "file") error("Only text files can be opened")
            if (entry.size > MAX_EDITABLE_FILE_BYTES) error("This file is too large to edit in the app")
            val content = if (demo) {
                "# Demo file\n\nDemo mode does not load repository file contents.\n"
            } else {
                SourceFileDecoder.decode(entry)
            }
            val branchProtected = if (demo) false else runCatching {
                repository.branchProtected(owner, repo, baseBranch)
            }.getOrDefault(false)
            _state.update {
                it.copy(
                    loading = false,
                    editor = FileEditorState(
                        path = path,
                        sha = entry.sha.takeIf(String::isNotBlank),
                        content = content,
                        originalContent = content,
                        branchProtected = branchProtected
                    ),
                    message = if (branchProtected) {
                        "${baseBranch} is protected. Changes will be proposed through a new branch and pull request."
                    } else {
                        "Changes always use a new branch and pull request."
                    }
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, error = error.message ?: "Unable to open this file") }
        }
    }

    fun startNewFile(path: String) = viewModelScope.launch {
        if (!isSafeFilePath(path)) {
            _state.update { it.copy(error = "Use a valid relative text-file path") }
            return@launch
        }
        val protected = if (demo) false else runCatching {
            repository.branchProtected(owner, repo, baseBranch)
        }.getOrDefault(false)
        _state.update {
            it.copy(
                error = null,
                message = "New file will be created on a review branch.",
                editor = FileEditorState(path, null, "", "", protected)
            )
        }
    }

    fun updateEditorContent(content: String) {
        _state.update { current -> current.editor?.let { current.copy(editor = it.copy(content = content)) } ?: current }
    }

    fun closeEditor() {
        _state.update { it.copy(editor = null, error = null, message = null) }
    }

    fun saveEditor(featureBranch: String, commitMessage: String) = viewModelScope.launch {
        val editor = _state.value.editor ?: return@launch
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not commit files") }
            return@launch
        }
        if (!BuildRunTracker.isSafeRef(featureBranch)) {
            _state.update { it.copy(error = "Use a valid review branch name") }
            return@launch
        }
        if (commitMessage.isBlank()) {
            _state.update { it.copy(error = "A commit message is required") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching {
            repository.commitFileAndOpenPullRequest(
                owner = owner,
                repo = repo,
                path = editor.path,
                content = editor.content,
                currentSha = editor.sha,
                baseBranch = baseBranch,
                featureBranch = featureBranch,
                commitMessage = commitMessage.trim(),
                pullTitle = "Edit ${editor.path}",
                pullBody = "Prepared in GitHub Rock on a new review branch. The default branch was not overwritten."
            )
        }.onSuccess { pull ->
            _state.update {
                it.copy(
                    message = "Pull request #${pull.number} created",
                    editor = it.editor?.copy(
                        originalContent = it.editor.content,
                        pullRequestUrl = pull.htmlUrl
                    )
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(error = error.message ?: "Unable to commit this file") }
        }
        _state.update { it.copy(loading = false) }
    }

    fun renameOrMoveFile(sourcePath: String, destinationPath: String, featureBranch: String, commitMessage: String) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not rename or move files") }
            return@launch
        }
        if (!isSafeFilePath(sourcePath) || !isSafeFilePath(destinationPath) || sourcePath == destinationPath) {
            _state.update { it.copy(error = "Use a different valid relative destination path") }
            return@launch
        }
        if (!BuildRunTracker.isSafeRef(featureBranch) || commitMessage.isBlank()) {
            _state.update { it.copy(error = "Use a valid review branch and commit message") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching {
            val entry = repository.file(owner, repo, sourcePath, baseBranch)
            check(entry.type == "file") { "Only files can be renamed or moved" }
            check(entry.size <= MAX_EDITABLE_FILE_BYTES) { "This file is too large to move in the app" }
            repository.renameOrMoveFileAndOpenPullRequest(
                owner, repo, sourcePath, destinationPath, entry.sha, SourceFileDecoder.decode(entry),
                baseBranch, featureBranch, commitMessage.trim()
            )
        }.onSuccess { pull ->
            _state.update { it.copy(message = "Pull request #${pull.number} created for the file move") }
            load(RepoSection.Code)
        }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to rename or move the file") } }
        _state.update { it.copy(loading = false) }
    }

    fun deleteFile(path: String, featureBranch: String, commitMessage: String) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not delete files") }
            return@launch
        }
        if (!isSafeFilePath(path)) {
            _state.update { it.copy(error = "Use a valid relative file path") }
            return@launch
        }
        if (!BuildRunTracker.isSafeRef(featureBranch) || commitMessage.isBlank()) {
            _state.update { it.copy(error = "Use a valid review branch and commit message") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching {
            val entry = repository.file(owner, repo, path, baseBranch)
            check(entry.type == "file") { "Only files can be deleted" }
            repository.deleteFileAndOpenPullRequest(owner, repo, path, entry.sha, baseBranch, featureBranch, commitMessage.trim())
        }.onSuccess { pull ->
            _state.update { it.copy(message = "Pull request #${pull.number} created to delete the file") }
            load(RepoSection.Code)
        }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to delete the file") } }
        _state.update { it.copy(loading = false) }
    }

    fun mergePullRequest(number: Int, method: String = "merge") = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.mergePullRequest(owner, repo, number, method) }
            .onSuccess { result ->
                _state.update {
                    if (result.merged) it.copy(message = "Pull request #$number merged successfully")
                    else it.copy(error = "GitHub did not merge #$number: ${result.message}")
                }
                if (result.merged) load(RepoSection.Pulls)
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to merge pull request") } }
        _state.update { it.copy(loading = false) }
    }

    fun loadJobLog(jobId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, jobLog = null) }
        runCatching { repository.workflowJobLogs(owner, repo, jobId) }
            .onSuccess { log -> _state.update { it.copy(jobLog = log.ifBlank { "No log output was returned by GitHub." }) } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to load workflow logs") } }
        _state.update { it.copy(loading = false) }
    }

    fun cancelWorkflow(runId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.cancel(owner, repo, runId) }
            .onSuccess { ok -> _state.update { if (ok) it.copy(message = "Workflow cancellation requested") else it.copy(error = "GitHub rejected the cancellation request") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to cancel workflow") } }
        _state.update { it.copy(loading = false) }
    }

    fun rerunWorkflow(runId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.rerun(owner, repo, runId) }
            .onSuccess { ok -> _state.update { if (ok) it.copy(message = "Workflow rerun requested") else it.copy(error = "GitHub rejected the rerun request") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to rerun workflow") } }
        _state.update { it.copy(loading = false) }
    }

    fun dispatchWorkflow(workflowId: Long, ref: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.dispatch(owner, repo, workflowId, ref, emptyMap()) }
            .onSuccess { ok -> _state.update { if (ok) it.copy(message = "Workflow dispatch requested") else it.copy(error = "GitHub rejected the workflow dispatch") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to dispatch workflow") } }
        _state.update { it.copy(loading = false) }
    }

    fun loadIssueComments(issueNumber: Int) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, issueComments = emptyList()) }
        runCatching { if (demo) emptyList() else repository.issueComments(owner, repo, issueNumber) }
            .onSuccess { comments -> _state.update { it.copy(issueComments = comments) } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to load issue comments") } }
        _state.update { it.copy(loading = false) }
    }

    fun addIssueComment(issueNumber: Int, body: String) = viewModelScope.launch {
        if (body.isBlank()) {
            _state.update { it.copy(error = "A comment cannot be empty") }
            return@launch
        }
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not post to GitHub") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.addIssueComment(owner, repo, issueNumber, body.trim()) }
            .onSuccess { comment -> _state.update { it.copy(issueComments = it.issueComments + comment, message = "Comment added") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to add comment") } }
        _state.update { it.copy(loading = false) }
    }

    fun createIssue(title: String, body: String) = viewModelScope.launch {
        if (title.isBlank()) {
            _state.update { it.copy(error = "An issue title is required") }
            return@launch
        }
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not create issues") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.createIssue(owner, repo, title.trim(), body.trim()) }
            .onSuccess { issue -> _state.update { it.copy(issues = listOf(issue) + it.issues, message = "Issue #${issue.number} created") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to create issue") } }
        _state.update { it.copy(loading = false) }
    }

    fun createPullRequest(title: String, head: String, base: String, body: String) = viewModelScope.launch {
        if (title.isBlank() || head.isBlank()) {
            _state.update { it.copy(error = "A pull request title and source branch are required") }
            return@launch
        }
        if (!head.matches(Regex("^[A-Za-z0-9._/-]+$"))) {
            _state.update { it.copy(error = "Use a valid source branch name") }
            return@launch
        }
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not create pull requests") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.createPullRequest(owner, repo, title.trim(), head.trim(), base, body.trim()) }
            .onSuccess { pull ->
                _state.update { it.copy(message = "Pull request #${pull.number} created") }
                load(RepoSection.Pulls)
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to create pull request") } }
        _state.update { it.copy(loading = false) }
    }

    fun updateIssueState(issueNumber: Int, state: String) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not change issue state") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.updateIssueState(owner, repo, issueNumber, state) }
            .onSuccess { updated ->
                _state.update { current ->
                    current.copy(
                        issues = current.issues.map { if (it.id == updated.id) updated else it },
                        message = "Issue #$issueNumber ${if (state == "closed") "closed" else "reopened"}"
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to update issue") } }
        _state.update { it.copy(loading = false) }
    }

    fun updateIssueMetadata(issueNumber: Int, labelsInput: String, assigneesInput: String, milestoneInput: String) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not edit issue metadata") }
            return@launch
        }
        val labels = labelsInput.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        val assignees = assigneesInput.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        if (labels.any { it.length > 50 || !it.matches(Regex("^[A-Za-z0-9][A-Za-z0-9 _./-]*$")) }) {
            _state.update { it.copy(error = "Use comma-separated GitHub label names") }
            return@launch
        }
        if (assignees.any { !it.matches(Regex("^[A-Za-z0-9-]+$")) }) {
            _state.update { it.copy(error = "Use comma-separated GitHub usernames") }
            return@launch
        }
        val milestone = milestoneInput.trim().takeIf(String::isNotBlank)?.toIntOrNull()
        if (milestoneInput.isNotBlank() && (milestone == null || milestone <= 0)) {
            _state.update { it.copy(error = "Milestone must be a positive number") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.updateIssueMetadata(owner, repo, issueNumber, labels, assignees, milestone) }
            .onSuccess { updated ->
                _state.update { current ->
                    current.copy(
                        issues = current.issues.map { if (it.id == updated.id) updated else it },
                        message = "Issue #$issueNumber metadata updated"
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to update issue metadata") } }
        _state.update { it.copy(loading = false) }
    }

    fun addIssueReaction(issueNumber: Int, content: String) = viewModelScope.launch {
        val allowed = setOf("+1", "-1", "laugh", "hooray", "confused", "heart", "rocket", "eyes")
        if (content !in allowed) {
            _state.update { it.copy(error = "Unsupported reaction") }
            return@launch
        }
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not add reactions") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.addIssueReaction(owner, repo, issueNumber, content) }
            .onSuccess {
                _state.update { it.copy(message = "Reaction added to issue #$issueNumber") }
                load(RepoSection.Issues)
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to add reaction") } }
        _state.update { it.copy(loading = false) }
    }

    fun setRepositoryStarred(starred: Boolean) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not change repository stars") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.setRepositoryStarred(owner, repo, starred) }
            .onSuccess { ok -> _state.update { if (ok) it.copy(message = if (starred) "Repository starred" else "Repository unstarred") else it.copy(error = "GitHub rejected the star change") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to update repository star") } }
        _state.update { it.copy(loading = false) }
    }

    fun forkRepository() = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not create forks") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.forkRepository(owner, repo) }
            .onSuccess { fork -> _state.update { it.copy(message = "Fork created: ${fork.fullName}") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to fork repository") } }
        _state.update { it.copy(loading = false) }
    }

    fun createDraftRelease(tag: String, name: String, body: String, prerelease: Boolean) = viewModelScope.launch {
        if (tag.isBlank() || !tag.matches(Regex("^[A-Za-z0-9._/-]+$"))) {
            _state.update { it.copy(error = "Use a valid release tag") }
            return@launch
        }
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not create releases") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.createDraftRelease(owner, repo, tag.trim(), name.trim(), body.trim(), prerelease) }
            .onSuccess { release -> _state.update { it.copy(releases = listOf(release) + it.releases, message = "Draft release ${release.tagName} created") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to create draft release") } }
        _state.update { it.copy(loading = false) }
    }

    fun deleteRelease(releaseId: Long) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not delete releases") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.deleteRelease(owner, repo, releaseId) }
            .onSuccess { deleted ->
                _state.update { if (deleted) it.copy(releases = it.releases.filterNot { release -> release.id == releaseId }, message = "Release deleted") else it.copy(error = "GitHub rejected the release deletion") }
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to delete release") } }
        _state.update { it.copy(loading = false) }
    }

    fun updateRelease(releaseId: Long, name: String, body: String, draft: Boolean, prerelease: Boolean) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not edit releases") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.updateRelease(owner, repo, releaseId, name.trim(), body.trim(), draft, prerelease) }
            .onSuccess { updated -> _state.update { current -> current.copy(releases = current.releases.map { if (it.id == updated.id) updated else it }, message = "Release ${updated.tagName} updated") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to update release") } }
        _state.update { it.copy(loading = false) }
    }

    fun loadPullReviews(pullNumber: Int) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, pullReviews = emptyList()) }
        runCatching { if (demo) emptyList() else repository.pullReviews(owner, repo, pullNumber) }
            .onSuccess { reviews -> _state.update { it.copy(pullReviews = reviews) } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to load pull request reviews") } }
        _state.update { it.copy(loading = false) }
    }

    fun submitPullReview(pullNumber: Int, event: String, body: String) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not submit reviews") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { repository.submitPullReview(owner, repo, pullNumber, event, body.trim()) }
            .onSuccess { review -> _state.update { it.copy(pullReviews = it.pullReviews + review, message = "Review submitted") } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to submit review") } }
        _state.update { it.copy(loading = false) }
    }

    private fun load(section: RepoSection) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            if (demo) loadDemo(section) else when (section) {
                RepoSection.Overview -> Unit
                RepoSection.Code -> _state.update { it.copy(contents = repository.contents(owner, repo, it.currentPath, baseBranch)) }
                RepoSection.Issues -> _state.update { it.copy(issues = repository.issues(owner, repo)) }
                RepoSection.Pulls -> _state.update { it.copy(pulls = repository.pulls(owner, repo)) }
                RepoSection.Actions -> {
                    val runs = repository.runs(owner, repo)
                    val latest = runs.firstOrNull()
                    _state.update {
                        it.copy(
                            workflows = repository.workflows(owner, repo),
                            runs = runs,
                            jobs = latest?.let { run -> repository.workflowJobs(owner, repo, run.id) }.orEmpty(),
                            artifacts = latest?.let { run -> repository.workflowArtifacts(owner, repo, run.id) }.orEmpty()
                        )
                    }
                }
                RepoSection.Releases -> _state.update { it.copy(releases = repository.releases(owner, repo)) }
            }
        }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to load this section") } }
        _state.update { it.copy(loading = false) }
    }

    private fun loadDemo(section: RepoSection) {
        _state.update {
            when (section) {
                RepoSection.Code -> it.copy(contents = DemoData.contents)
                RepoSection.Issues -> it.copy(issues = DemoData.issues)
                RepoSection.Pulls -> it.copy(pulls = DemoData.pulls)
                RepoSection.Actions -> it.copy(runs = DemoData.workflows, workflows = listOf(Workflow(1, "Android Build", ".github/workflows/android-build.yml", "active")))
                RepoSection.Releases -> it.copy(releases = DemoData.releases)
                RepoSection.Overview -> it
            }
        }
    }

    private fun isSafeFilePath(path: String): Boolean =
        path.matches(Regex("^[A-Za-z0-9._/-]+$")) &&
            !path.startsWith('/') &&
            !path.endsWith('/') &&
            !path.contains("..") &&
            !path.contains("//")

    private companion object {
        const val MAX_EDITABLE_FILE_BYTES = 1_000_000L
    }
}
