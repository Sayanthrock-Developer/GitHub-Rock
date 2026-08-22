package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** Native repository workspace. GitHub browsing stays inside GitHub Rock; only the explicit external action opens GitHub. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryHubScreen(repository: GitHubRepositoryModel?, onBack: () -> Unit, initialTag: String? = null, viewModel: RepositoryHubViewModel = hiltViewModel(), downloadsViewModel: DownloadsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloads by downloadsViewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var workspacePage by rememberSaveable { mutableStateOf("overview") }
    var nativeSection by rememberSaveable { mutableStateOf<RepoSection?>(null) }
    var confirmUninstall by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(repository?.id) { viewModel.start(repository) }
    val displayedRepository = state.repository ?: repository
    val appState = rememberRepositoryAppPackageState(downloads, state.releases)
    val obtainiumInstalled = isObtainiumInstalled(context)

    when (workspacePage) {
        "manager" -> {
            BackHandler { workspacePage = "overview"; nativeSection = null }
            RepositoryDetailSectionScreen(
                repository = displayedRepository,
                section = nativeSection ?: RepoSection.Overview,
                onBack = { workspacePage = "overview"; nativeSection = null }
            )
            return
        }
        "files" -> {
            BackHandler { workspacePage = "overview" }
            RepositoryFileManagerScreen(repository = displayedRepository, onBack = { workspacePage = "overview" })
            return
        }
    }

    // Normal repository URLs are translated to native GitHub Rock sections.
    // Only the explicit Open on GitHub action uses the external browser.
    val openUrl: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            nativeSection = when {
                url.contains("/issues", ignoreCase = true) -> RepoSection.Issues
                url.contains("/pulls", ignoreCase = true) -> RepoSection.Pulls
                url.contains("/actions", ignoreCase = true) -> RepoSection.Actions
                url.contains("/releases", ignoreCase = true) -> RepoSection.Releases
                url.contains("/tree/", ignoreCase = true) || url.contains("/blob/", ignoreCase = true) -> null
                else -> RepoSection.Code
            }
            workspacePage = if (url.contains("/tree/", ignoreCase = true) || url.contains("/blob/", ignoreCase = true)) "files" else "manager"
        }
    }

    val openGitHub: () -> Unit = {
        displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbar.showSnackbar("Unable to open GitHub") }
            } catch (_: IllegalArgumentException) {
                scope.launch { snackbar.showSnackbar("This GitHub link is not valid") }
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
                repositoryLoading = state.loading,
                repositoryHasError = state.error != null,
                onBack = onBack,
                onOpenManager = { nativeSection = RepoSection.Overview; workspacePage = "manager" },
                onOpenFiles = { workspacePage = "files" },
                onOpenGitHub = openGitHub,
                applicationStatus = appState?.statusLabel
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                    if (asset.downloadUrl.startsWith("https://example.com")) scope.launch { snackbar.showSnackbar("Demo assets are preview-only") }
                    else { downloadsViewModel.enqueue(asset.downloadUrl, asset.name); scope.launch { snackbar.showSnackbar("${asset.name} added to Downloads") } }
                },
                modifier = Modifier.weight(1f)
            )
            appState?.let { installedApp ->
                RepositoryAppInstallPanel(
                    state = installedApp,
                    onInstall = { installRepositoryApk(context, installedApp).onFailure { problem -> scope.launch { snackbar.showSnackbar(problem.message ?: "Android could not open the package installer.") } } },
                    onOpen = { openRepositoryApp(context, installedApp).onFailure { problem -> scope.launch { snackbar.showSnackbar(problem.message ?: "Android could not open this application.") } } },
                    onUninstall = { confirmUninstall = true }
                )
            }
            if (repositoryReady) {
                displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { repositoryUrl ->
                    ObtainiumUpdateCard(
                        obtainiumInstalled = obtainiumInstalled,
                        onOpen = {
                            openInObtainium(context, repositoryUrl)
                                .onSuccess { openedInObtainium ->
                                    if (!openedInObtainium) {
                                        scope.launch { snackbar.showSnackbar("Obtainium is not installed; opened GitHub Store instead") }
                                    }
                                }
                                .onFailure { problem ->
                                    scope.launch { snackbar.showSnackbar(problem.message ?: "Unable to open Obtainium") }
                                }
                        }
                    )
                }
            }
        }
    }

    val uninstallTarget = appState
    if (confirmUninstall && uninstallTarget != null) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            title = { Text("Uninstall ${uninstallTarget.appName}?") },
            text = { Text("Android will open its uninstall confirmation for ${uninstallTarget.packageName}. GitHub Rock cannot remove an app silently.") },
            confirmButton = { TextButton(onClick = { confirmUninstall = false; requestRepositoryAppUninstall(context, uninstallTarget).onFailure { problem -> scope.launch { snackbar.showSnackbar(problem.message ?: "Android could not open the uninstall screen.") } } }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { confirmUninstall = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RepositoryWorkspaceTopBar(
    repository: GitHubRepositoryModel?,
    repositoryReady: Boolean,
    repositoryLoading: Boolean,
    repositoryHasError: Boolean,
    onBack: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenGitHub: () -> Unit = {},
    applicationStatus: String? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(repository?.fullName ?: "Repository", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(
                    text = repository?.let { listOfNotNull(if (it.private) "Private" else "Public", it.defaultBranch, applicationStatus).joinToString(" · ") } ?: when {
                        repositoryHasError -> "Repository unavailable"
                        repositoryLoading -> "Loading repository"
                        repositoryReady -> "Repository workspace"
                        else -> "Repository unavailable"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
        actions = {
            repository?.let {
                Icon(if (it.private) Icons.Default.Lock else Icons.Default.Public, contentDescription = if (it.private) "Private repository" else "Public repository", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onOpenManager, enabled = repositoryReady) { Icon(Icons.Default.Code, contentDescription = "Manage repository") }
                IconButton(onClick = onOpenFiles, enabled = repositoryReady) { Icon(Icons.Default.FolderOpen, contentDescription = "Browse repository files") }
                IconButton(onClick = onOpenGitHub, enabled = repositoryReady) { Icon(Icons.Default.OpenInNew, contentDescription = "Open on GitHub") }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}
