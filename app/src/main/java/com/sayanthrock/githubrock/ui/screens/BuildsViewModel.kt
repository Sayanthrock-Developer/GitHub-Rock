package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.build.WorkflowMonitorScheduler
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.model.WorkflowArtifact
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuildsActionState(
    val loading: Boolean = false,
    val runsLoading: Boolean = false,
    val creatingPullRequest: Boolean = false,
    val tracking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val pullRequestUrl: String? = null,
    val selectedRepositoryId: Long? = null,
    val recentRuns: List<WorkflowRun> = emptyList(),
    val workflow: Workflow? = null,
    val workflowSource: String? = null,
    val workflowSourcePath: String? = null,
    val workflowSourceLoading: Boolean = false,
    val workflowSourceError: String? = null,
    val run: WorkflowRun? = null,
    val jobs: List<WorkflowJob> = emptyList(),
    val artifacts: List<WorkflowArtifact> = emptyList()
)

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val repository: GitHubRepository,
    private val monitorScheduler: WorkflowMonitorScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(BuildsActionState())
    val state: StateFlow<BuildsActionState> = _state.asStateFlow()
    private var trackingJob: Job? = null

    fun loadAndroidBuild(selected: GitHubRepositoryModel, requestedRunId: Long? = null) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            val previous = _state.value
            _state.update { it.copy(loading = true, runsLoading = true, creatingPullRequest = false, tracking = false, message = null, error = null, pullRequestUrl = null, selectedRepositoryId = selected.id, recentRuns = if (previous.selectedRepositoryId == selected.id) previous.recentRuns else emptyList(), workflow = null, workflowSource = null, workflowSourcePath = null, workflowSourceLoading = true, workflowSourceError = null, run = previous.run?.takeIf { previous.selectedRepositoryId == selected.id && (requestedRunId == null || it.id == requestedRunId) }, jobs = previous.jobs.takeIf { previous.selectedRepositoryId == selected.id && requestedRunId != null } ?: emptyList(), artifacts = previous.artifacts.takeIf { previous.selectedRepositoryId == selected.id && requestedRunId != null } ?: emptyList()) }
            try {
                val owner = selected.owner.login
                val repo = selected.name
                val runsJob = launch {
                    runCatching { repository.runs(owner, repo) }
                        .onSuccess { runs -> _state.update { if (it.selectedRepositoryId == selected.id) it.copy(runsLoading = false, recentRuns = runs) else it } }
                        .onFailure { error -> _state.update { if (it.selectedRepositoryId == selected.id) it.copy(runsLoading = false, error = error.message ?: "Unable to load workflow runs") else it } }
                }
                val workflow = BuildRunTracker.findAndroidWorkflow(repository.workflows(owner, repo))
                if (workflow == null) {
                    runsJob.join()
                    _state.update { it.copy(loading = false, workflowSourceLoading = false, message = "No merged Android build workflow was found. Merge its pull request, then refresh.") }
                    return@launch
                }
                val sourceResult = try { Result.success(SourceFileDecoder.decode(repository.file(owner, repo, workflow.path, selected.defaultBranch))) } catch (cancelled: CancellationException) { throw cancelled } catch (error: Throwable) { Result.failure(error) }
                val latest = requestedRunId?.let { repository.run(owner, repo, it) } ?: repository.runsForWorkflow(owner, repo, workflow.id).firstOrNull()
                val jobs = latest?.let { repository.workflowJobs(owner, repo, it.id) }.orEmpty()
                val artifacts = if (latest?.displayState() == WorkflowDisplayState.Success) repository.workflowArtifacts(owner, repo, latest.id) else emptyList()
                runsJob.join()
                val sourceFailure = sourceResult.exceptionOrNull()
                _state.update { it.copy(loading = false, runsLoading = false, workflow = workflow, workflowSource = sourceResult.getOrNull(), workflowSourcePath = workflow.path, workflowSourceLoading = false, workflowSourceError = sourceFailure?.let { failure -> "Unable to load workflow code: ${failure.message?.takeIf(String::isNotBlank) ?: "unknown source error"}" }, run = latest, jobs = jobs, artifacts = artifacts, message = if (latest == null) "Android build workflow is ready to run" else null) }
                if (latest != null && BuildRunTracker.isActive(latest)) monitorRun(selected, latest)
            } catch (cancelled: CancellationException) { throw cancelled } catch (error: Throwable) { _state.update { it.copy(loading = false, runsLoading = false, workflowSourceLoading = false, tracking = false, error = error.message ?: "Unable to inspect the Android build workflow") } }
        }
    }

    fun dispatchAndroidBuild(selected: GitHubRepositoryModel, ref: String) {
        if (!BuildRunTracker.isSafeRef(ref)) { _state.update { it.copy(error = "Use a valid branch or tag", message = null) }; return }
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, runsLoading = true, creatingPullRequest = false, tracking = false, error = null, message = null, selectedRepositoryId = selected.id, run = null, jobs = emptyList(), artifacts = emptyList()) }
            try {
                val owner = selected.owner.login; val repo = selected.name
                val workflow = BuildRunTracker.findAndroidWorkflow(repository.workflows(owner, repo)) ?: error("No merged Android build workflow was found")
                val knownIds = repository.runsForWorkflow(owner, repo, workflow.id).mapTo(mutableSetOf()) { it.id }
                check(repository.dispatch(owner, repo, workflow.id, ref, emptyMap())) { "GitHub rejected the workflow dispatch" }
                monitorScheduler.monitorDispatch(owner = owner, repo = repo, workflowId = workflow.id, ref = ref, knownRunIds = knownIds)
                _state.update { it.copy(loading = false, runsLoading = false, tracking = true, workflow = workflow, message = "GitHub accepted the dispatch. Waiting for the run…") }
                val dispatchedRun = awaitDispatchedRun(selected, workflow, ref, knownIds)
                if (dispatchedRun == null) { _state.update { it.copy(tracking = false, message = null, error = "GitHub accepted the dispatch, but the new run is not visible yet. Refresh to continue tracking.") }; return@launch }
                _state.update { it.copy(run = dispatchedRun, recentRuns = it.recentRuns.upsertRun(dispatchedRun), message = "Build queued on $ref") }
                monitorRun(selected, dispatchedRun)
            } catch (cancelled: CancellationException) { throw cancelled } catch (error: Throwable) { _state.update { it.copy(loading = false, runsLoading = false, tracking = false, message = null, error = error.message ?: "Unable to start the Android build") } }
        }
    }

    fun cancelRun(selected: GitHubRepositoryModel, runId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { check(repository.cancel(selected.owner.login, selected.name, runId)) { "GitHub rejected the cancellation" }; repository.run(selected.owner.login, selected.name, runId) }
            .onSuccess { run -> trackingJob?.cancel(); _state.update { it.copy(loading = false, tracking = false, run = run, recentRuns = it.recentRuns.upsertRun(run), message = "Build cancelled") }; loadRunDetails(selected, run.id) }
            .onFailure { error -> _state.update { it.copy(loading = false, error = error.message ?: "Unable to cancel the build") } }
    }

    fun rerunRun(selected: GitHubRepositoryModel, runId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        runCatching { check(repository.rerun(selected.owner.login, selected.name, runId)) { "GitHub rejected the re-run" }; repository.run(selected.owner.login, selected.name, runId) }
            .onSuccess { run -> _state.update { it.copy(loading = false, tracking = BuildRunTracker.isActive(run), run = run, recentRuns = it.recentRuns.upsertRun(run), message = "Build re-run requested") }; if (BuildRunTracker.isActive(run)) monitorRun(selected, run) else loadRunDetails(selected, run.id) }
            .onFailure { error -> _state.update { it.copy(loading = false, error = error.message ?: "Unable to re-run the build") } }
    }

    fun loadRunDetails(selected: GitHubRepositoryModel, runId: Long) = viewModelScope.launch {
        runCatching {
            val run = repository.run(selected.owner.login, selected.name, runId)
            val jobs = repository.workflowJobs(selected.owner.login, selected.name, runId)
            val artifacts = if (run.displayState() == WorkflowDisplayState.Success) repository.workflowArtifacts(selected.owner.login, selected.name, runId) else emptyList()
            Triple(run, jobs, artifacts)
        }.onSuccess { (run, jobs, artifacts) -> _state.update { if (it.selectedRepositoryId == null || it.selectedRepositoryId == selected.id) it.copy(run = run, recentRuns = it.recentRuns.upsertRun(run), jobs = jobs, artifacts = artifacts, loading = false, error = null) else it } }
            .onFailure { error -> if (error !is CancellationException) _state.update { it.copy(loading = false, error = error.message ?: "Unable to load build details") } }
    }

    fun resetBuild() {
        trackingJob?.cancel(); trackingJob = null
        _state.update { it.copy(loading = false, runsLoading = false, creatingPullRequest = false, tracking = false, message = null, error = null, pullRequestUrl = null, selectedRepositoryId = null, recentRuns = emptyList(), workflow = null, workflowSource = null, workflowSourcePath = null, workflowSourceLoading = false, workflowSourceError = null, run = null, jobs = emptyList(), artifacts = emptyList()) }
    }

    fun createWorkflowPullRequest(selected: GitHubRepositoryModel, featureBranch: String, yaml: String, artifact: AndroidArtifactType) = viewModelScope.launch {
        if (!BuildRunTracker.isSafeRef(featureBranch)) { _state.update { it.copy(error = "Use a valid review branch name", message = null) }; return@launch }
        _state.update { it.copy(loading = true, creatingPullRequest = true, error = null, message = null, pullRequestUrl = null) }
        runCatching { repository.commitFileAndOpenPullRequest(owner = selected.owner.login, repo = selected.name, path = ".github/workflows/android-build.yml", content = yaml, currentSha = null, baseBranch = selected.defaultBranch, featureBranch = featureBranch, commitMessage = "Add Android build workflow", pullTitle = "Add Android ${artifact.name} workflow", pullBody = "Generated and reviewed in GitHub Rock. Uses GitHub-hosted runners and does not contain signing secrets.") }
            .onSuccess { pull -> _state.update { it.copy(message = "Pull request #${pull.number} created", pullRequestUrl = pull.htmlUrl) } }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Unable to create workflow pull request") } }
        _state.update { it.copy(loading = false, creatingPullRequest = false) }
    }

    private suspend fun awaitDispatchedRun(selected: GitHubRepositoryModel, workflow: Workflow, ref: String, knownIds: Set<Long>): WorkflowRun? { repeat(RUN_DISCOVERY_ATTEMPTS) { delay(RUN_DISCOVERY_INTERVAL_MS); BuildRunTracker.findDispatchedRun(repository.runsForWorkflow(selected.owner.login, selected.name, workflow.id), knownIds, ref)?.let { return it } }; return null }

    private suspend fun monitorRun(selected: GitHubRepositoryModel, initial: WorkflowRun) {
        var current = initial; var consecutiveFailures = 0
        _state.update { it.copy(loading = false, tracking = true, run = current, recentRuns = it.recentRuns.upsertRun(current)) }
        while (currentCoroutineContext().isActive) {
            try {
                current = repository.run(selected.owner.login, selected.name, current.id)
                val jobs = repository.workflowJobs(selected.owner.login, selected.name, current.id); consecutiveFailures = 0
                _state.update { it.copy(loading = false, tracking = BuildRunTracker.isActive(current), run = current, recentRuns = it.recentRuns.upsertRun(current), jobs = jobs, message = if (BuildRunTracker.isActive(current)) "Build ${current.displayState().name.lowercase()}" else null) }
                if (!BuildRunTracker.isActive(current)) { finishRun(selected, current, jobs); return }
            } catch (cancelled: CancellationException) { throw cancelled } catch (error: Throwable) { consecutiveFailures++; if (consecutiveFailures >= MAX_CONSECUTIVE_POLL_FAILURES) throw error; _state.update { it.copy(message = "Run tracking was interrupted. Retrying…", error = null) } }
            delay(RUN_POLL_INTERVAL_MS)
        }
    }

    private suspend fun finishRun(selected: GitHubRepositoryModel, run: WorkflowRun, jobs: List<WorkflowJob>) {
        val artifacts = if (run.displayState() == WorkflowDisplayState.Success) awaitArtifacts(selected, run.id) else emptyList()
        val result = when (run.displayState()) {
            WorkflowDisplayState.Success -> if (artifacts.isNotEmpty()) {
                "Build succeeded with ${artifacts.size} downloadable artifact${if (artifacts.size == 1) "" else "s"}" to null
            } else null to "Build succeeded, but GitHub did not publish an artifact"
            WorkflowDisplayState.Failed -> null to "Build failed. Review the job and step statuses below."
            WorkflowDisplayState.Cancelled -> "Build was cancelled" to null
            else -> "Build finished with ${run.conclusion ?: run.status}" to null
        }
        _state.update { it.copy(loading = false, runsLoading = false, tracking = false, run = run, recentRuns = it.recentRuns.upsertRun(run), jobs = jobs, artifacts = artifacts, message = result.first, error = result.second) }
    }

    private suspend fun awaitArtifacts(selected: GitHubRepositoryModel, runId: Long): List<WorkflowArtifact> {
        repeat(ARTIFACT_DISCOVERY_ATTEMPTS) { attempt -> val artifacts = repository.workflowArtifacts(selected.owner.login, selected.name, runId); if (artifacts.isNotEmpty()) return artifacts; if (attempt < ARTIFACT_DISCOVERY_ATTEMPTS - 1) delay(ARTIFACT_DISCOVERY_INTERVAL_MS) }
        return emptyList()
    }

    private companion object { const val RUN_DISCOVERY_ATTEMPTS = 24; const val RUN_DISCOVERY_INTERVAL_MS = 2_500L; const val RUN_POLL_INTERVAL_MS = 8_000L; const val MAX_CONSECUTIVE_POLL_FAILURES = 3; const val ARTIFACT_DISCOVERY_ATTEMPTS = 4; const val ARTIFACT_DISCOVERY_INTERVAL_MS = 2_000L }
}

private fun List<WorkflowRun>.upsertRun(run: WorkflowRun): List<WorkflowRun> = (filterNot { it.id == run.id } + run).sortedByDescending { it.createdAt }
