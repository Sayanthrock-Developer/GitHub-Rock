package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel

/**
 * Opens the native repository detail workspace on a specific section.
 * Issues use the dedicated native Open/Closed workspace so issue browsing
 * and issue details remain inside GitHub Rock.
 */
@Composable
fun RepositoryDetailSectionScreen(
    repository: GitHubRepositoryModel?,
    section: RepoSection,
    onBack: () -> Unit
) {
    if (section == RepoSection.Issues) {
        IssuesScreen(onBack = onBack)
        return
    }

    val viewModel: RepositoryDetailViewModel = hiltViewModel()

    LaunchedEffect(section) {
        viewModel.select(section)
    }

    RepositoryDetailScreen(
        repository = repository,
        onBack = onBack,
        viewModel = viewModel
    )
}
