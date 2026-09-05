package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.ui.AppMode

/**
 * Opens the existing Builds screen with a pre-filtered run set.
 * The original Builds UI, repository controls, workflow inspection, execution panel,
 * filter chips and build details behavior remain unchanged.
 */
@Composable
fun BuildStatusPage(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    filterName: String,
    onSelectRepository: (GitHubRepositoryModel) -> Unit,
    onOpenRun: (GitHubRepositoryModel, WorkflowRun) -> Unit,
    ownerLogin: String? = null
) {
    val filteredRuns = when (filterName.lowercase()) {
        "running" -> runs.filter {
            it.displayState() !in setOf(
                WorkflowDisplayState.Success,
                WorkflowDisplayState.Failed,
                WorkflowDisplayState.Cancelled
            )
        }
        "failed" -> runs.filter { it.displayState() == WorkflowDisplayState.Failed }
        "success" -> runs.filter { it.displayState() == WorkflowDisplayState.Success }
        else -> runs
    }

    val ownedRepositories = ownerLogin?.takeIf { it.isNotBlank() }?.let { login ->
        repositories.filter { it.owner.login.equals(login, ignoreCase = true) }
    } ?: repositories

    BuildsScreen(
        mode = mode,
        repositories = ownedRepositories,
        runs = filteredRuns,
        onSelectRepository = onSelectRepository,
        onOpenRun = onOpenRun
    )
}
