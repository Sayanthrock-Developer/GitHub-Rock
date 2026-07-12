package com.sayanthrock.githubrock.build

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.data.settings.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class WorkflowMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: GitHubRepository,
    private val preferences: AppPreferences,
    private val notifications: BuildNotificationManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val input = MonitorInput.from(inputData) ?: return Result.failure()
        return try {
            val persistedRunId = preferences.monitoredWorkflowRun(input.monitorKey)
            val savedRunId = WorkflowMonitorPolicy.persistedRunId(persistedRunId, input.knownRunIds)
            if (persistedRunId != null && savedRunId == null) {
                preferences.clearMonitoredWorkflowRun(input.monitorKey)
            }
            val run = if (savedRunId != null) {
                repository.run(input.owner, input.repo, savedRunId)
            } else {
                discoverRun(input)
            }
            if (run == null) return retryOrStop(input)
            if (savedRunId != run.id) preferences.setMonitoredWorkflowRun(input.monitorKey, run.id)

            val completion = WorkflowMonitorPolicy.completion(run)
            if (completion == null) return retryOrStop(input)

            preferences.clearMonitoredWorkflowRun(input.monitorKey)
            notifications.showCompletion(input.owner, input.repo, run, completion)
            Result.success(workDataOf(KEY_RUN_ID to run.id))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            retryOrStop(input)
        }
    }

    private suspend fun discoverRun(input: MonitorInput): WorkflowRun? = BuildRunTracker.findDispatchedRun(
        runs = repository.runsForWorkflow(input.owner, input.repo, input.workflowId),
        knownRunIds = input.knownRunIds,
        ref = input.ref
    )

    private suspend fun retryOrStop(input: MonitorInput): Result {
        if (runAttemptCount < MAX_ATTEMPTS) return Result.retry()
        preferences.clearMonitoredWorkflowRun(input.monitorKey)
        notifications.showMonitoringStopped(input.owner, input.repo)
        return Result.failure(workDataOf(KEY_ERROR to "monitor_timeout"))
    }

    private data class MonitorInput(
        val owner: String,
        val repo: String,
        val workflowId: Long,
        val ref: String,
        val monitorKey: String,
        val knownRunIds: Set<Long>
    ) {
        companion object {
            private val ownerPattern = Regex("^[A-Za-z0-9-]+$")
            private val repoPattern = Regex("^[A-Za-z0-9._-]+$")
            private val keyPattern = Regex("^[a-f0-9]{24}$")

            fun from(data: androidx.work.Data): MonitorInput? {
                val owner = data.getString(KEY_OWNER)?.takeIf(ownerPattern::matches) ?: return null
                val repo = data.getString(KEY_REPO)?.takeIf(repoPattern::matches) ?: return null
                val workflowId = data.getLong(KEY_WORKFLOW_ID, -1).takeIf { it > 0 } ?: return null
                val ref = data.getString(KEY_REF)?.takeIf(BuildRunTracker::isSafeRef) ?: return null
                val monitorKey = data.getString(KEY_MONITOR)?.takeIf(keyPattern::matches) ?: return null
                return MonitorInput(
                    owner = owner,
                    repo = repo,
                    workflowId = workflowId,
                    ref = ref,
                    monitorKey = monitorKey,
                    knownRunIds = (data.getLongArray(KEY_KNOWN_RUN_IDS) ?: longArrayOf()).toSet()
                )
            }
        }
    }

    companion object {
        const val KEY_OWNER = "owner"
        const val KEY_REPO = "repo"
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_REF = "ref"
        const val KEY_MONITOR = "monitor_key"
        const val KEY_KNOWN_RUN_IDS = "known_run_ids"
        const val KEY_RUN_ID = "run_id"
        const val KEY_ERROR = "error"
        private const val MAX_ATTEMPTS = 28
    }
}
