package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.model.WorkflowRun

object BuildRunTracker {
    private const val GENERATED_WORKFLOW_PATH = ".github/workflows/android-build.yml"
    private val safeRefCharacters = Regex("^[A-Za-z0-9._/-]+$")

    fun findAndroidWorkflow(workflows: List<Workflow>): Workflow? =
        workflows.firstOrNull {
            it.path.trimStart('/') == GENERATED_WORKFLOW_PATH && it.state == "active"
        } ?: workflows.firstOrNull {
            it.name.equals("Android Build", ignoreCase = true) && it.state == "active"
        }

    fun findDispatchedRun(
        runs: List<WorkflowRun>,
        knownRunIds: Set<Long>,
        ref: String
    ): WorkflowRun? = runs.firstOrNull {
        it.id !in knownRunIds && it.event == "workflow_dispatch" && it.headBranch == ref
    }

    fun isActive(run: WorkflowRun): Boolean =
        run.conclusion == null && run.status != "completed"

    fun isSafeRef(ref: String): Boolean =
        ref.isNotBlank() &&
            safeRefCharacters.matches(ref) &&
            !ref.startsWith('/') &&
            !ref.endsWith('/') &&
            !ref.contains("..") &&
            !ref.contains("//") &&
            !ref.endsWith(".lock", ignoreCase = true)
}
