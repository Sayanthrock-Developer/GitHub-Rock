package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import kotlinx.coroutines.launch

/** Native repository workspace with overview, code, issues, pull requests, Actions and releases. */
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

    val repositoryReady = displayedRepository != null && !state.loading && state.error == null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            RepositoryWorkspaceTopBar(
                repository = displayedRepository,
                repositoryReady = repositoryReady,
                onBack = onBack,
                onOpenManager = { workspacePage = "manager" },
                onOpenFiles = { workspacePage = "files" }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RepositoryWorkspaceTopBar(
    repository: GitHubRepositoryModel?,
    repositoryReady: Boolean,
    onBack: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenFiles: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = repository?.fullName ?: "Repository",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = repository?.let {
                        val visibility = if (it.private) "Private" else "Public"
                        "$visibility · ${it.defaultBranch}"
                    } ?: if (repositoryReady) "Repository workspace" else "Loading repository",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            repository?.let {
                Icon(
                    imageVector = if (it.private) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = if (it.private) "Private repository" else "Public repository",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onOpenManager,
                    enabled = repositoryReady
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Manage repository")
                }
                IconButton(
                    onClick = onOpenFiles,
                    enabled = repositoryReady
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse repository files")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
