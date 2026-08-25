package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** Single icon-first download workspace for applications, files, and build artifacts. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(8))
    ) {
        DownloadsApplicationsScreen(viewModel = viewModel)
    }
}
