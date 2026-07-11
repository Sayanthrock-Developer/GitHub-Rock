package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.*
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
    val currentPath: String = "",
    val error: String? = null,
    val message: String? = null,
    val jobLog: String? = null
)

@HiltViewModel
class RepositoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GitHubRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repo: String = checkNotNull(savedStateHandle["repo"])
    private val demo: Boolean = savedStateHandle["demo"] ?: false
    private val _state = MutableStateFlow(RepositoryDetailState())
    val state: StateFlow<RepositoryDetailState> = _state.asStateFlow()

    fun select(section: RepoSection) {
        _state.update { it.copy(section = section, error = null) }
        if (section != RepoSection.Overview) load(section)
    }

    fun openDirectory(path: String) {
        _state.update { it.copy(currentPath = path, section = RepoSection.Code) }
        load(RepoSection.Code)
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

    private fun load(section: RepoSection) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            if (demo) loadDemo(section) else when (section) {
                RepoSection.Overview -> Unit
                RepoSection.Code -> _state.update { it.copy(contents = repository.contents(owner, repo, it.currentPath, null)) }
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
}
