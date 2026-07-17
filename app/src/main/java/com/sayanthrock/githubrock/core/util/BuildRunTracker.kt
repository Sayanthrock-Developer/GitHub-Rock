package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.model.WorkflowRun

object BuildRunTracker {
    private const val GENERATED_WORKFLOW_PATH = ".github/workflows/android-build.yml"
    private val forbiddenRefCharacters = setOf('~', '^', ':', '?', '*', '[', '\\')

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

    /**
     * Determines whether a workflow run is still active.
     *
     * @param run The workflow run to evaluate.
     * @return `true` when the run has no conclusion and has not completed.
     */
    fun isActive(run: WorkflowRun): Boolean =
        run.conclusion == null && run.status != "completed"

    /**
     * Checks a branch or tag against Git ref naming constraints used by build operations.
     *
     * @param ref The branch or tag to validate.
     * @return `true` when the ref can be safely passed to GitHub APIs.
     */
    fun isSafeRef(ref: String): Boolean {
        if (
            ref.isBlank() ||
            ref == "@" ||
            ref.startsWith('/') ||
            ref.endsWith('/') ||
            ref.contains("..") ||
            ref.contains("//") ||
            ref.contains("@{") ||
            ref.any { character ->
                character.code <= 0x20 ||
                    character.code == 0x7f ||
                    character in forbiddenRefCharacters
            }
        ) {
            return false
        }

        return ref.split('/').all { component ->
            component.isNotBlank() &&
                !component.startsWith('.') &&
                !component.endsWith('.') &&
                !component.endsWith(".lock", ignoreCase = true)
        }
    }
}
