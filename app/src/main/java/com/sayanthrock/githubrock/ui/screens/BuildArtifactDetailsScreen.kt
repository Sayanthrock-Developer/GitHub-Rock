package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowArtifact
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildArtifactDetailsViewModel @Inject constructor(private val repository: GitHubRepository) : ViewModel() {
    private val _artifact = MutableStateFlow<WorkflowArtifact?>(null)
    val artifact = _artifact.asStateFlow()
    fun load(repo: GitHubRepositoryModel, runId: Long, artifactId: Long) = viewModelScope.launch {
        _artifact.value = repository.workflowArtifacts(repo.owner.login, repo.name, runId).firstOrNull { it.id == artifactId }
    }
}

@Composable
fun BuildArtifactDetailsScreen(repository: GitHubRepositoryModel, runId: Long, artifactId: Long, onBack: () -> Unit, viewModel: BuildArtifactDetailsViewModel = hiltViewModel(), downloadsViewModel: DownloadsViewModel = hiltViewModel()) {
    val artifact by viewModel.artifact.collectAsState()
    LaunchedEffect(repository.id, runId, artifactId) { viewModel.load(repository, runId, artifactId) }
    Column(Modifier.fillMaxSize().padding(StandardScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Artifact", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        artifact?.let { item ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                    Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Size: ${item.sizeBytes} bytes")
                    Text(if (item.expired) "Expired — this artifact can no longer be downloaded." else "Available for download")
                    Button(onClick = { downloadsViewModel.enqueue(item.archiveDownloadUrl, "${repository.name}-${item.name}-${item.id}.zip") }, enabled = !item.expired, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Archive, null); Spacer(Modifier.width(8.dp)); Text(if (item.expired) "Expired" else "Download artifact")
                    }
                }
            }
        } ?: Text("Artifact details are unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
