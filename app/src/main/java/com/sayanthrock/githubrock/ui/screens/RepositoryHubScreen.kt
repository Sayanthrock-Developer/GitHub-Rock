package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.RepositoryHealthState
import com.sayanthrock.githubrock.core.util.RepositoryWorkspacePolicy
import kotlinx.coroutines.launch

/** Single repository page containing identity, releases, tools, statistics and documentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryHubScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    initialTag: String? = null,
    viewModel: RepositoryHubViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var workspacePage by rememberSaveable { mutableStateOf("overview") }

    LaunchedEffect(repository?.id) {
        viewModel.start(repository)
    }

    val displayedRepository = state.repository ?: repository

    when (workspacePage) {
        "manager" -> {
            BackHandler { workspacePage = "overview" }
            RepositoryDetailScreen(
                repository = displayedRepository,
                onBack = { workspacePage = "overview" }
            )
            return
        }
        "files" -> {
            BackHandler { workspacePage = "overview" }
            RepositoryFileManagerScreen(
                repository = displayedRepository,
                onBack = { workspacePage = "overview" }
            )
            return
        }
    }

    val openUrl: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbar.showSnackbar("Unable to open this link") }
            } catch (_: IllegalArgumentException) {
                scope.launch { snackbar.showSnackbar("This link is not valid") }
            }
        }
    }

    val repositoryHealthy = displayedRepository != null && !state.loading && state.error == null
    val fileToolsHealthy = repositoryHealthy
    val loadProgress = RepositoryWorkspacePolicy.loadProgress(
        repositoryReady = repositoryHealthy,
        releasesLoading = state.releasesLoading,
        readmeLoading = state.readmeLoading
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            displayedRepository?.name ?: "Repository",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        displayedRepository?.owner?.login?.let {
                            Text(
                                "@$it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url ->
                        IconButton(onClick = { openUrl(url) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open repository on GitHub")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            displayedRepository?.let { repo ->
                RepositoryWorkspacePanel(
                    repository = repo,
                    progress = loadProgress,
                    repositoryHealthy = repositoryHealthy,
                    fileToolsHealthy = fileToolsHealthy,
                    onOpenManager = { workspacePage = "manager" },
                    onOpenFiles = { workspacePage = "files" },
                    onOpenUrl = openUrl
                )
            }
            RepositoryHubContent(
                repository = displayedRepository,
                releases = state.releases,
                readme = state.readme,
                loading = state.loading,
                releasesLoading = state.releasesLoading,
                readmeLoading = state.readmeLoading,
                error = state.error,
                releasesError = state.releasesError,
                readmeError = state.readmeError,
                initialTag = initialTag,
                onRetry = viewModel::retry,
                onOpenUrl = openUrl,
                onDownload = { asset ->
                    if (asset.downloadUrl.startsWith("https://example.com")) {
                        scope.launch { snackbar.showSnackbar("Demo assets are preview-only") }
                    } else {
                        downloadsViewModel.enqueue(asset.downloadUrl, asset.name)
                        scope.launch { snackbar.showSnackbar("${asset.name} added to Downloads") }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RepositoryWorkspacePanel(
    repository: GitHubRepositoryModel,
    progress: Int,
    repositoryHealthy: Boolean,
    fileToolsHealthy: Boolean,
    onOpenManager: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val base = repository.htmlUrl.trimEnd('/').ifBlank {
        "https://github.com/${repository.fullName}"
    }
    val issueHealth = RepositoryWorkspacePolicy.issueHealth(repository.openIssues)
    val issueHealthy = issueHealth == RepositoryHealthState.Healthy

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Loading repository workspace", fontWeight = FontWeight.Bold)
                    Text("${progress.coerceIn(0, 100)} / 100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkspaceHealthFrame(
                if (repositoryHealthy) "Repository ready" else "Repository loading or failed",
                healthy = repositoryHealthy
            )
            WorkspaceHealthFrame(
                if (issueHealthy) "No open issues" else "${repository.openIssues} open issues",
                healthy = issueHealthy
            )
            WorkspaceHealthFrame(
                if (fileToolsHealthy) "File tools ready" else "File tools unavailable",
                healthy = fileToolsHealthy
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenManager,
                enabled = repositoryHealthy,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Manage")
            }
            OutlinedButton(
                onClick = onOpenFiles,
                enabled = fileToolsHealthy,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Files")
            }
        }

        val options = listOf(
            "Issues" to "$base/issues",
            "Pull Requests" to "$base/pulls",
            "Discussions" to "$base/discussions",
            "Projects" to "$base/projects",
            "Top Repositories" to "https://github.com/${repository.owner.login}?tab=repositories",
            "Organization" to "https://github.com/${repository.owner.login}"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, url) ->
                WorkspaceOptionFrame(label = label, onClick = { onOpenUrl(url) })
            }
        }
    }
}

@Composable
private fun WorkspaceHealthFrame(label: String, healthy: Boolean) {
    val accent = if (healthy) HEALTHY_GREEN else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (healthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(label, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WorkspaceOptionFrame(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

private val HEALTHY_GREEN = Color(0xFF2DA44E)
