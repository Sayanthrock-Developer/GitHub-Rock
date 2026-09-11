package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.ui.components.LocalOpenGitHubProfile

/** Single download workspace for applications, files, and build artifacts. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val hostProfileOpener by rememberUpdatedState(LocalOpenGitHubProfile.current)
    DownloadsRedesignScreen(
        viewModel = viewModel,
        onOpenProfile = { login -> hostProfileOpener?.invoke(login) }
    )
}
