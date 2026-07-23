package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** Single icon-first download workspace for applications, files, and build artifacts. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    DownloadsRedesignScreen(viewModel = viewModel)
}
