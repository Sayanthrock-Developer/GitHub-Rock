package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.repository.RepositoryArtworkResolver
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepositoryLibraryState(
    val mode: AppMode = AppMode.Guest,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val starred: List<GitHubRepositoryModel> = emptyList(),
    val favourites: List<GitHubRepositoryModel> = emptyList(),
    val recent: List<GitHubRepositoryModel> = emptyList(),
    val favouriteNames: Set<String> = emptySet(),
    val loadingStars: Boolean = false,
    val starError: String? = null
)

@HiltViewModel
class RepositoryLibraryViewModel @Inject constructor(
    private val api: GitHubRestApi,
    private val recentDao: RepositoryDao,
    private val preferences: AppPreferences,
    private val artworkResolver: RepositoryArtworkResolver
) : ViewModel() {
    private val _state = MutableStateFlow(RepositoryLibraryState())
    val state = _state.asStateFlow()
    private var starJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.favoriteRepositories.collectLatest { names ->
                _state.update { current -> current.copy(favouriteNames = names) }
                rebuildFavourites()
            }
        }
        viewModelScope.launch {
            recentDao.observeRecent(20).collectLatest { entities ->
                val recent = entities.map { entity ->
                    GitHubRepositoryModel(
                        id = entity.id,
                        name = entity.name,
                        fullName = entity.fullName,
                        owner = Owner(entity.owner),
                        description = entity.description,
                        private = entity.isPrivate,
                        language = entity.language,
                        stars = entity.stars,
                        updatedAt = entity.updatedAt
                    )
                }
                _state.update { it.copy(recent = recent) }
                rebuildFavourites()
            }
        }
    }

    fun start(mode: AppMode, repositories: List<GitHubRepositoryModel>) {
        val modeChanged = mode != _state.value.mode
        _state.update { it.copy(mode = mode, repositories = repositories) }
        rebuildFavourites()
        when {
            mode == AppMode.Connected && (modeChanged || _state.value.starred.isEmpty()) -> refreshStars()
            mode != AppMode.Connected -> {
                starJob?.cancel()
                _state.update { it.copy(starred = emptyList(), loadingStars = false, starError = null) }
                rebuildFavourites()
            }
        }
    }

    fun refreshStars() {
        if (_state.value.mode != AppMode.Connected) return
        starJob?.cancel()
        starJob = viewModelScope.launch {
            _state.update { it.copy(loadingStars = true, starError = null) }
            runCatchingPreservingCancellation {
                artworkResolver.attach(api.starredRepositories())
            }.onSuccess { starred ->
                _state.update { it.copy(starred = starred, loadingStars = false) }
                rebuildFavourites()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loadingStars = false,
                        starError = error.message ?: "Unable to load starred repositories"
                    )
                }
            }
        }
    }

    fun toggleFavourite(repository: GitHubRepositoryModel) {
        viewModelScope.launch { preferences.toggleFavoriteRepository(repository.fullName) }
    }

    private fun rebuildFavourites() {
        _state.update { current ->
            val candidates = (current.repositories + current.starred + current.recent)
                .distinctBy { it.fullName.lowercase() }
                .associateBy { it.fullName.lowercase() }
            val favourites = current.favouriteNames.mapNotNull { candidates[it.lowercase()] }
            current.copy(favourites = favourites)
        }
    }
}

@Composable
fun RepositoryLibraryScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    onSignIn: () -> Unit,
    onOpenDeveloperTools: () -> Unit,
    viewModel: RepositoryLibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(mode, repositories.map { it.id }) {
        viewModel.start(mode, repositories)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Repository Library",
                subtitle = "Stars, favourites, recent projects and mobile tools"
            )
        }

        if (mode != AppMode.Connected) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Already have an account?", fontWeight = FontWeight.Bold)
                                Text(
                                    "Sign in to load your private repositories and GitHub Stars.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
                    }
                }
            }
        }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Mobile developer tools", fontWeight = FontWeight.Bold)
                        Text(
                            "GitHub CLI command builder, PR checkout helper and API templates.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onOpenDeveloperTools) { Text("Open") }
                }
            }
        }

        item {
            StandardSectionHeader("Stars", if (mode == AppMode.Connected) "${state.starred.size} repositories" else "Sign in required")
        }
        if (state.loadingStars) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        } else if (state.starError != null) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.starError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = viewModel::refreshStars) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            items(state.starred.take(8), key = { "star-${it.id}" }) { repository ->
                LibraryRepositoryRow(
                    repository = repository,
                    favourite = repository.fullName.inNames(state.favouriteNames),
                    leading = Icons.Default.Star,
                    onOpen = { onOpenRepository(repository) },
                    onToggleFavourite = { viewModel.toggleFavourite(repository) }
                )
            }
        }

        item { StandardSectionHeader("Favourites", "${state.favourites.size} saved on this device") }
        if (state.favourites.isEmpty()) {
            item { EmptyLibraryCard("Tap the heart beside a repository to keep it here.") }
        } else {
            items(state.favourites, key = { "fav-${it.id}" }) { repository ->
                LibraryRepositoryRow(
                    repository = repository,
                    favourite = true,
                    leading = Icons.Default.Favorite,
                    onOpen = { onOpenRepository(repository) },
                    onToggleFavourite = { viewModel.toggleFavourite(repository) }
                )
            }
        }

        item { StandardSectionHeader("Recently Viewed", "${state.recent.size} repositories") }
        if (state.recent.isEmpty()) {
            item { EmptyLibraryCard("Repositories you open will appear here automatically.") }
        } else {
            items(state.recent.take(10), key = { "recent-${it.id}" }) { repository ->
                LibraryRepositoryRow(
                    repository = repository,
                    favourite = repository.fullName.inNames(state.favouriteNames),
                    leading = Icons.Default.History,
                    onOpen = { onOpenRepository(repository) },
                    onToggleFavourite = { viewModel.toggleFavourite(repository) }
                )
            }
        }

        item { StandardSectionHeader("All repositories", "${repositories.size} available") }
        items(repositories, key = { "all-${it.id}" }) { repository ->
            LibraryRepositoryRow(
                repository = repository,
                favourite = repository.fullName.inNames(state.favouriteNames),
                leading = null,
                onOpen = { onOpenRepository(repository) },
                onToggleFavourite = { viewModel.toggleFavourite(repository) }
            )
        }

        item {
            GlassCard {
                Text("GitHub API access is free within GitHub's normal limits. Connected mode uses OAuth; GitHub Rock never displays or copies your access token.")
            }
        }
    }
}

@Composable
private fun LibraryRepositoryRow(
    repository: GitHubRepositoryModel,
    favourite: Boolean,
    leading: androidx.compose.ui.graphics.vector.ImageVector?,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit
) {
    GlassCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(repository.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    repository.description ?: repository.language ?: "GitHub repository",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favourite) "Remove from favourites" else "Add to favourites",
                    tint = if (favourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(message: String) {
    GlassCard { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun String.inNames(names: Set<String>): Boolean = names.any { equals(it, ignoreCase = true) }
