package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowArtifact
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.screens.DownloadsViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BuildArtifactDetailsScreen(repository: GitHubRepositoryModel, artifact: WorkflowArtifact, onBack: () -> Unit, downloadsViewModel: DownloadsViewModel = hiltViewModel()) {
    Column(Modifier.fillMaxSize().padding(StandardScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Artifact", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                Text(artifact.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Size: ${artifact.sizeBytes} bytes")
                Text(if (artifact.expired) "Expired — this artifact can no longer be downloaded." else "Available for download")
                Button(onClick = { downloadsViewModel.enqueue(artifact.archiveDownloadUrl, "${repository.name}-${artifact.name}-${artifact.id}.zip") }, enabled = !artifact.expired, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Archive, null); Spacer(Modifier.width(8.dp)); Text(if (artifact.expired) "Expired" else "Download artifact")
                }
            }
        }
    }
}
