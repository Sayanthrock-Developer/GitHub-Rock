package com.sayanthrock.githubrock.build

import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.util.BuildRunTracker

data class WorkflowCompletion(
    val title: String,
    val state: WorkflowDisplayState
)

object WorkflowMonitorPolicy {
    fun persistedRunId(persistedRunId: Long?, knownRunIds: Set<Long>): Long? =
        persistedRunId?.takeUnless { it in knownRunIds }

    fun completion(run: WorkflowRun): WorkflowCompletion? {
        if (BuildRunTracker.isActive(run)) return null
        val state = run.displayState()
        val title = when (state) {
            WorkflowDisplayState.Success -> "Android build succeeded"
            WorkflowDisplayState.Failed -> "Android build failed"
            WorkflowDisplayState.Cancelled -> "Android build cancelled"
            else -> "Android build finished"
        }
        return WorkflowCompletion(title, state)
    }
}
