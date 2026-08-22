package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Single download workspace with a native automatic-update manager. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf("downloads") }

    androidx.compose.foundation.layout.Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == "downloads",
                onClick = { selectedTab = "downloads" },
                label = { Text("Downloads") }
            )
            FilterChip(
                selected = selectedTab == "updates",
                onClick = { selectedTab = "updates" },
                label = { Text("Managed updates") }
            )
        }
        when (selectedTab) {
            "updates" -> ManagedUpdatesScreen(
                viewModel = viewModel,
                onOpenDownloads = { selectedTab = "downloads" }
            )
            else -> DownloadsRedesignScreen(viewModel = viewModel)
        }
    }
}
