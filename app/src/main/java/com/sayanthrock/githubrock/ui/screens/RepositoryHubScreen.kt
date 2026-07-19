package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import kotlinx.coroutines.launch

/** Native repository workspace with overview, code, issues, pull requests, Actions and releases. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryHubScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    initialTag: String? = null,
    viewModel: RepositoryHubViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by appearanceViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var workspacePage by rememberSaveable { mutableStateOf("overview") }

    LaunchedEffect(repository?.id) {
        viewModel.start(repository)
    }
    LaunchedEffect(preferences.repositoryManager, preferences.fileTools, workspacePage) {
        if (workspacePage == "manager" && !preferences.repositoryManager) workspacePage = "overview"
        if (workspacePage == "files" && !preferences.fileTools) workspacePage = "overview"
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
    val managerReady = repositoryReady && preferences.repositoryManager
    val filesReady = repositoryReady && preferences.fileTools

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            displayedRepository?.fullName ?: "Repository",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (repositoryReady) "Native repository workspace" else "Loading repository",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            displayedRepository?.let { repo ->
                RepositoryIdentityPanel(
                    repository = repo,
                    repositoryReady = repositoryReady,
                    managerReady = managerReady,
                    filesReady = filesReady,
                    preferences = preferences,
                    onOpenManager = { workspacePage = "manager" },
                    onOpenFiles = { workspacePage = "files" }
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
private fun RepositoryIdentityPanel(
    repository: GitHubRepositoryModel,
    repositoryReady: Boolean,
    managerReady: Boolean,
    filesReady: Boolean,
    preferences: AppearancePreferences,
    onOpenManager: () -> Unit,
    onOpenFiles: () -> Unit
) {
    val spacing = if (preferences.compactCards) 10.dp else 14.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(if (preferences.compactCards) 16.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (repository.private) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            repository.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            repository.owner.login,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RepositoryVisibilityPill(repository.private)
                }

                Text(
                    repository.description?.takeIf { it.isNotBlank() }
                        ?: "No description has been added to this repository yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepositoryMetric(Icons.Default.Code, repository.defaultBranch, "Default branch")
                    RepositoryMetric(Icons.Default.Star, repository.stars.toString(), "Stars")
                    RepositoryMetric(Icons.Default.ForkRight, repository.forks.toString(), "Forks")
                    RepositoryMetric(Icons.Default.WarningAmber, repository.openIssues.toString(), "Open issues")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenManager,
                        enabled = managerReady,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (preferences.repositoryManager) "Manage" else "Manager off")
                    }
                    FilledTonalButton(
                        onClick = onOpenFiles,
                        enabled = filesReady,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (preferences.fileTools) "Files" else "Files off")
                    }
                }

                Text(
                    when {
                        !repositoryReady -> "Repository data is still loading or unavailable."
                        managerReady && filesReady -> "Repository tools are ready inside the app."
                        else -> "Enable repository manager and file tools in Appearance to use every native action."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RepositoryVisibilityPill(isPrivate: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            if (isPrivate) "Private" else "Public",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RepositoryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
