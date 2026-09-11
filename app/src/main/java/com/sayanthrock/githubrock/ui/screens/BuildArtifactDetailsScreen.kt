package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
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
import com.sayanthrock.githubrock.ui.icons.RockIcon
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
fun BuildArtifactDetailsScreen(
    repository: GitHubRepositoryModel,
    runId: Long,
    artifactId: Long,
    onBack: () -> Unit,
    viewModel: BuildArtifactDetailsViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel()
) {
    val artifact by viewModel.artifact.collectAsState()
    LaunchedEffect(repository.id, runId, artifactId) { viewModel.load(repository, runId, artifactId) }

    Column(
        Modifier.fillMaxSize().padding(StandardScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), "Back") }
            Column {
                Text("Artifact", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        artifact?.let { item ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(RockIcon.Archive.vector(), null, tint = MaterialTheme.colorScheme.primary)
                    Text("Verify Android summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("GitHub Actions API data", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    ArtifactSummaryRow("Name", item.name)
                    ArtifactSummaryRow("Size", formatArtifactSize(item.sizeBytes))
                    ArtifactSummaryRow("Digest", item.digest ?: "Not provided by GitHub for this artifact")
                    ArtifactSummaryRow("Status", if (item.expired) "Expired" else "Available")
                    Button(
                        onClick = { downloadsViewModel.enqueue(item.archiveDownloadUrl, "${repository.name}-${item.name}-${item.id}.zip") },
                        enabled = !item.expired,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(RockIcon.Archive.vector(), null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (item.expired) "Expired" else "Download artifact")
                    }
                }
            }
        } ?: Text("Artifact details are unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArtifactSummaryRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatArtifactSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes bytes"
}
