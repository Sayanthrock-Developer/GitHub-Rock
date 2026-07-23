package com.sayanthrock.githubrock.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.repository.NativeProfileRepository
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Repository content loaded for the connected-account profile dashboard. */
data class ConnectedProfileDashboardUiState(
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConnectedProfileDashboardViewModel @Inject constructor(
    private val repository: NativeProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectedProfileDashboardUiState())
    val state: StateFlow<ConnectedProfileDashboardUiState> = _state.asStateFlow()
    private var loadedLogin: String? = null

    fun load(login: String) {
        if (loadedLogin.equals(login, ignoreCase = true) && _state.value.repositories.isNotEmpty()) return
        loadedLogin = login
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatchingPreservingCancellation { repository.repositories(login) }
                .onSuccess { repositories ->
                    _state.value = ConnectedProfileDashboardUiState(repositories = repositories)
                }
                .onFailure { problem ->
                    _state.value = ConnectedProfileDashboardUiState(
                        loading = false,
                        error = problem.message ?: "Unable to load repositories."
                    )
                }
        }
    }

    fun refresh(login: String) {
        loadedLogin = null
        load(login)
    }
}

@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState = ProfileExplorerState(),
    onInspectProfile: (String) -> Unit = {},
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit,
    dashboardStateOverride: ConnectedProfileDashboardUiState? = null
) {
    if (dashboardStateOverride != null) {
        ProfileDashboardRuntime(
            mode = mode,
            profile = profile,
            explorerState = explorerState,
            repositoryState = dashboardStateOverride,
            onInspectProfile = onInspectProfile,
            onRefreshRepositories = {},
            onOpenDownloads = onOpenDownloads,
            onOpenFeatures = onOpenFeatures,
            onOpenSettings = onOpenSettings,
            onOpenAppInfo = onOpenAppInfo,
            onOpenGitHubUrl = onOpenGitHubUrl,
            onLogout = onLogout
        )
    } else {
        ConnectedProfileDashboardRoute(
            mode = mode,
            profile = profile,
            explorerState = explorerState,
            onInspectProfile = onInspectProfile,
            onOpenDownloads = onOpenDownloads,
            onOpenFeatures = onOpenFeatures,
            onOpenSettings = onOpenSettings,
            onOpenAppInfo = onOpenAppInfo,
            onOpenGitHubUrl = onOpenGitHubUrl,
            onLogout = onLogout
        )
    }
}

@Composable
private fun ConnectedProfileDashboardRoute(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState,
    onInspectProfile: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: ConnectedProfileDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val login = normalizedGitHubLogin(displayedProfile?.login)

    LaunchedEffect(login) {
        login?.let(viewModel::load)
    }

    ProfileDashboardRuntime(
        mode = mode,
        profile = profile,
        explorerState = explorerState,
        repositoryState = state,
        onInspectProfile = onInspectProfile,
        onRefreshRepositories = { login?.let(viewModel::refresh) },
        onOpenDownloads = onOpenDownloads,
        onOpenFeatures = onOpenFeatures,
        onOpenSettings = onOpenSettings,
        onOpenAppInfo = onOpenAppInfo,
        onOpenGitHubUrl = onOpenGitHubUrl,
        onLogout = onLogout
    )
}

@Composable
private fun ProfileDashboardRuntime(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState,
    repositoryState: ConnectedProfileDashboardUiState,
    onInspectProfile: (String) -> Unit,
    onRefreshRepositories: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val details = explorerState.snapshot?.details
    val login = normalizedGitHubLogin(displayedProfile?.login)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable(login) { mutableStateOf("") }
    var filter by rememberSaveable(login) { mutableStateOf(ProfileRepositoryFilter.All) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportProfile by remember { mutableStateOf<GitHubUser?>(null) }

    LaunchedEffect(mode, login) {
        login?.let(onInspectProfile)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val target = pendingExportProfile
        pendingExportProfile = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(ProfileExportFormatter.toJson(target))
                    } ?: error("Unable to create the profile file")
                }
            }.onSuccess {
                exportMessage = "Profile downloaded successfully"
            }.onFailure { problem ->
                exportMessage = problem.message ?: "Unable to download this profile"
            }
        }
    }

    val openUrl: (String) -> Unit = { rawUrl ->
        val value = rawUrl.trim()
        if (value.startsWith("https://github.com/", ignoreCase = true) ||
            value.startsWith("https://gist.github.com/", ignoreCase = true)
        ) {
            onOpenGitHubUrl(value)
        } else if (value.startsWith("https://", ignoreCase = true)) {
            runCatching { uriHandler.openUri(value) }
        }
    }

    val repositories = remember(repositoryState.repositories, query, filter) {
        filterProfileRepositories(repositoryState.repositories, query, filter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileDashboardHeader(
                profile = displayedProfile,
                isOwnProfile = mode == AppMode.Connected,
                canFollow = false,
                followStateLoaded = true,
                isFollowing = false,
                followUpdating = false,
                onToggleFollow = {},
                onRepositories = login?.let { { onOpenGitHubUrl("https://github.com/$it?tab=repositories") } },
                onFollowers = login?.let { { onOpenGitHubUrl("https://github.com/$it?tab=followers") } },
                onFollowing = login?.let { { onOpenGitHubUrl("https://github.com/$it?tab=following") } }
            )
        }

        item { ContributionActivityCard(details) }
        item { ProfileIdentitySummary(displayedProfile, details, openUrl) }

        if (explorerState.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        explorerState.error?.let { message ->
            item { GlassCard { Text(message, color = MaterialTheme.colorScheme.error) } }
        }

        item {
            ProfileRepositoryToolbar(
                query = query,
                onQueryChange = { query = it },
                selectedFilter = filter,
                onFilterChange = { filter = it },
                shown = repositories.size,
                total = repositoryState.repositories.size
            )
        }

        when {
            repositoryState.loading -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 42.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            repositoryState.error != null -> item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(repositoryState.error, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRefreshRepositories) { Text("Retry") }
                    }
                }
            }
            repositories.isEmpty() -> item {
                GlassCard {
                    Text(
                        if (query.isBlank() && filter == ProfileRepositoryFilter.All) {
                            "No public repositories to show."
                        } else {
                            "No repositories match this search and filter."
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> items(repositories, key = GitHubRepositoryModel::id) { repository ->
                ProfileRepositoryCard(repository) {
                    onOpenGitHubUrl(repository.htmlUrl.ifBlank { "https://github.com/${repository.fullName}" })
                }
            }
        }

        item { StandardSectionHeader("Profile controls") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardControlButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = onOpenSettings
                )
                DashboardControlButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountCircle,
                    label = "Accounts",
                    onClick = onOpenFeatures
                )
                DashboardControlButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Download,
                    label = "Downloads",
                    onClick = onOpenDownloads
                )
            }
        }

        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("App information")
                    }
                    displayedProfile?.let { target ->
                        OutlinedButton(
                            onClick = {
                                exportMessage = null
                                pendingExportProfile = target
                                exportLauncher.launch(ProfileExportFormatter.fileName(target))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download profile")
                        }
                    }
                    login?.let {
                        OutlinedButton(
                            onClick = { onOpenGitHubUrl("https://github.com/$it") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open GitHub profile")
                        }
                    }
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (mode == AppMode.Connected) "Log out" else "Exit ${mode.name.lowercase()} mode")
                    }
                }
            }
        }

        exportMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        message,
                        color = if (message.contains("success", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardControlButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
