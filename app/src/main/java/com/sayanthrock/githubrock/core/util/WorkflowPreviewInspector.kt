package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState

enum class WorkflowPreviewHealth {
    Healthy,
    Running,
    Problem,
    Unknown
}

data class WorkflowPreviewReport(
    val health: WorkflowPreviewHealth,
    val title: String,
    val detail: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val failedSteps: Int,
    val sourceProblems: List<String>
)

object WorkflowPreviewInspector {
    fun inspect(
        source: String?,
        run: WorkflowRun?,
        jobs: List<WorkflowJob>,
        sourceError: String? = null
    ): WorkflowPreviewReport {
        val problems = buildList {
            if (!sourceError.isNullOrBlank()) add(sourceError)
            source?.takeIf(String::isNotBlank)?.let { yaml ->
                if (!hasRootKey(yaml, "name")) add("Workflow name is missing")
                if (!hasRootKey(yaml, "on")) add("Workflow trigger is missing")
                if (!hasRootKey(yaml, "jobs")) add("Jobs section is missing")
                if ('\t' in yaml) add("Tab indentation can break YAML parsing")
            }
        }
        val steps = jobs.flatMap(WorkflowJob::steps)
        val completed = steps.count { it.status == "completed" }
        val failed = steps.count { it.conclusion in FAILURE_CONCLUSIONS }
        val active = steps.any { it.status in ACTIVE_STATUSES }
        val runState = run?.displayState()
        val health = when {
            problems.isNotEmpty() || failed > 0 || runState == WorkflowDisplayState.Failed -> WorkflowPreviewHealth.Problem
            active || runState == WorkflowDisplayState.Running || runState == WorkflowDisplayState.Queued -> WorkflowPreviewHealth.Running
            runState == WorkflowDisplayState.Success ||
                (runState == null && !source.isNullOrBlank() && problems.isEmpty()) -> WorkflowPreviewHealth.Healthy
            else -> WorkflowPreviewHealth.Unknown
        }
        val title = when (health) {
            WorkflowPreviewHealth.Healthy -> "Workflow looks healthy"
            WorkflowPreviewHealth.Running -> "Workflow is running"
            WorkflowPreviewHealth.Problem -> "Workflow needs attention"
            WorkflowPreviewHealth.Unknown -> when (runState) {
                WorkflowDisplayState.Cancelled -> "Workflow was cancelled"
                else -> "No completed run yet"
            }
        }
        val detail = when {
            problems.isNotEmpty() -> problems.first()
            failed > 0 -> "$failed failed step${if (failed == 1) "" else "s"} detected"
            active -> "GitHub is still processing the current run"
            runState == WorkflowDisplayState.Success -> "The latest run completed successfully"
            runState == WorkflowDisplayState.Cancelled -> "The latest run did not complete"
            !source.isNullOrBlank() -> "Basic YAML structure checks passed"
            else -> "Select a repository with an active workflow"
        }
        return WorkflowPreviewReport(
            health = health,
            title = title,
            detail = detail,
            completedSteps = completed,
            totalSteps = steps.size,
            failedSteps = failed,
            sourceProblems = problems
        )
    }

    private fun hasRootKey(source: String, key: String): Boolean {
        val candidates = source.lineSequence()
            .map { raw ->
                val spaces = raw.takeWhile { it == ' ' }.length
                spaces to raw.trim()
            }
            .filter { (_, text) ->
                text.isNotEmpty() && !text.startsWith('#') && text != "---" && text != "..."
            }
            .toList()
        val rootIndent = candidates.minOfOrNull { it.first } ?: return false
        val escaped = Regex.escape(key)
        val keyPattern = Regex("^(?:$escaped|\"$escaped\"|'$escaped')\\s*:")
        return candidates.any { (indent, text) -> indent == rootIndent && keyPattern.containsMatchIn(text) }
    }

    private val FAILURE_CONCLUSIONS = setOf("failure", "timed_out", "action_required", "startup_failure")
    private val ACTIVE_STATUSES = setOf("queued", "in_progress", "waiting", "pending")
}
