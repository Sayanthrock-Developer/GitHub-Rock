package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.ManagedAppEntity

@Composable
fun ManagedUpdatesScreen(
    viewModel: DownloadsViewModel,
    onOpenDownloads: () -> Unit
) {
    val managedApps by viewModel.managedApps.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Managed updates", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "GitHub Rock checks managed repositories every 6 hours and downloads new stable Android APK releases automatically. Android still requires user confirmation to install an APK.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { viewModel.checkUpdatesNow() }) {
                    Text("Check for updates now")
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }

        if (managedApps.isNotEmpty()) {
            items(managedApps, key = { it.packageName }) { app ->
                ManagedAppCard(
                    app = app,
                    hasDownloadedUpdate = downloads.any { it.autoUpdate && it.status == "completed" && it.repositoryOwner == app.repositoryOwner && it.repositoryName == app.repositoryName && it.releaseTag == app.trackedReleaseTag },
                    onToggle = { viewModel.setAutoUpdate(app.packageName, it) },
                    onStop = { viewModel.stopManaging(app.packageName) },
                    onOpenDownloads = onOpenDownloads
                )
            }
        } else {
            item {
                Text(
                    "No apps are managed yet. Install an APK downloaded from a GitHub release, then use Manage updates on that APK below.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val candidates = downloads.filter { it.status == "completed" && it.fileName.endsWith(".apk", ignoreCase = true) }
            .filter { download -> managedApps.none { it.repositoryOwner == download.repositoryOwner && it.repositoryName == download.repositoryName } }
        if (candidates.isNotEmpty()) {
            item { Text("Available APKs", style = MaterialTheme.typography.titleMedium) }
            items(candidates, key = { it.id }) { download ->
                CandidateApkCard(
                    download = download,
                    onManage = {
                        viewModel.manageUpdates(download) { result ->
                            message = result.fold(
                                onSuccess = { "${download.fileName} is now managed for automatic updates." },
                                onFailure = { it.message ?: "Could not enable automatic updates." }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ManagedAppCard(
    app: ManagedAppEntity,
    hasDownloadedUpdate: Boolean,
    onToggle: (Boolean) -> Unit,
    onStop: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium)
                    Text("${app.repositoryOwner}/${app.repositoryName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Installed v${app.installedVersionName ?: app.installedVersionCode}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = app.autoUpdate, onCheckedChange = onToggle)
            }
            app.trackedReleaseTag?.let { Text("Tracked release: $it", style = MaterialTheme.typography.bodySmall) }
            if (app.lastUpdateAvailableAt != null || hasDownloadedUpdate) {
                Text("Update ready in Downloads", color = MaterialTheme.colorScheme.primary)
                Button(onClick = onOpenDownloads) { Text("Open Downloads") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenDownloads) { Text("Downloads") }
                TextButton(onClick = onStop) { Text("Stop managing") }
            }
        }
    }
}

@Composable
private fun CandidateApkCard(
    download: DownloadEntity,
    onManage: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(download.fileName, style = MaterialTheme.typography.titleMedium)
            download.releaseTag?.let { Text("Release $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            download.repositoryOwner?.let { owner ->
                download.repositoryName?.let { repo ->
                    Text("$owner/$repo", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text("Manage updates")
            }
        }
    }
}
