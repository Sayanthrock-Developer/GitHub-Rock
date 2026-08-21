package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel

/**
 * Opens the native repository detail workspace on a specific section.
 * The detail screen remains the single implementation for Code, Issues,
 * Pull Requests, Actions, and Releases.
 */
@Composable
fun RepositoryDetailSectionScreen(
    repository: GitHubRepositoryModel?,
    section: RepoSection,
    onBack: () -> Unit
) {
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
