package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            runCatchingPreservingCancellation { artworkResolver.attach(api.starredRepositories()) }
                .onSuccess { starred ->
                    _state.update { it.copy(starred = starred, loadingStars = false) }
                    rebuildFavourites()
                }
                .onFailure { error ->
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
            current.copy(
                favourites = current.favouriteNames.mapNotNull { candidates[it.lowercase()] }
            )
        }
    }
}

private enum class LibraryDestination(val label: String, val icon: ImageVector) {
    Search("Search", Icons.Default.Search),
    Library("Library", Icons.Default.GridView),
    Favourites("Favourites", Icons.Default.Favorite),
    Recent("Recently Viewed", Icons.Default.History)
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
    var destination by rememberSaveable { mutableStateOf(LibraryDestination.Library) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(mode, repositories.map { it.id }) {
        viewModel.start(mode, repositories)
    }

    val allRepositories = remember(state.repositories, state.starred, state.recent) {
        (state.repositories + state.starred + state.recent)
            .distinctBy { it.fullName.lowercase() }
    }
    val visibleRepositories = remember(destination, query, allRepositories, state.favourites, state.recent) {
        when (destination) {
            LibraryDestination.Search -> {
                val term = query.trim()
                if (term.isBlank()) allRepositories else allRepositories.filter { repository ->
                    repository.fullName.contains(term, ignoreCase = true) ||
                        repository.description.orEmpty().contains(term, ignoreCase = true) ||
                        repository.language.orEmpty().contains(term, ignoreCase = true)
                }
            }
            LibraryDestination.Library -> allRepositories
            LibraryDestination.Favourites -> state.favourites
            LibraryDestination.Recent -> state.recent
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = destination.label,
                subtitle = "Search, save and reopen GitHub repositories"
            )
        }

        item {
            LibraryNavigationHub(
                selected = destination,
                onSelect = { destination = it }
            )
        }

        if (destination == LibraryDestination.Search) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search repositories") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    supportingText = { Text("Search by repository, description or language") }
                )
            }
        }

        if (mode != AppMode.Connected && destination == LibraryDestination.Library) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Sign in for your complete library", fontWeight = FontWeight.Bold)
                                Text(
                                    "Load private repositories and GitHub Stars.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
                    }
                }
            }
        }

        if (destination == LibraryDestination.Library) {
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Mobile developer tools", fontWeight = FontWeight.Bold)
                            Text(
                                "GitHub CLI commands, PR checkout and API templates.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = onOpenDeveloperTools) { Text("Open") }
                    }
                }
            }
        }

        if (destination == LibraryDestination.Library && state.loadingStars) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        state.starError?.takeIf { destination == LibraryDestination.Library }?.let { error ->
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = viewModel::refreshStars) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }

        item {
            Text(
                when (destination) {
                    LibraryDestination.Search -> if (query.isBlank()) "${visibleRepositories.size} repositories" else "${visibleRepositories.size} results"
                    LibraryDestination.Library -> "${visibleRepositories.size} repositories"
                    LibraryDestination.Favourites -> "${visibleRepositories.size} saved on this device"
                    LibraryDestination.Recent -> "${visibleRepositories.size} recently opened"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (visibleRepositories.isEmpty()) {
            item {
                EmptyLibraryCard(
                    when (destination) {
                        LibraryDestination.Search -> "No repositories match your search."
                        LibraryDestination.Library -> "No repositories are available yet."
                        LibraryDestination.Favourites -> "Tap the heart beside a repository to save it here."
                        LibraryDestination.Recent -> "Repositories you open will appear here automatically."
                    }
                )
            }
        } else {
            items(visibleRepositories, key = { "${destination.name}-${it.id}-${it.fullName}" }) { repository ->
                LibraryRepositoryRow(
                    repository = repository,
                    favourite = repository.fullName.inNames(state.favouriteNames),
                    leading = when (destination) {
                        LibraryDestination.Favourites -> Icons.Default.Favorite
                        LibraryDestination.Recent -> Icons.Default.History
                        LibraryDestination.Library -> if (repository in state.starred) Icons.Default.Star else null
                        LibraryDestination.Search -> Icons.Default.Search
                    },
                    onOpen = { onOpenRepository(repository) },
                    onToggleFavourite = { viewModel.toggleFavourite(repository) }
                )
            }
        }
    }
}

@Composable
private fun LibraryNavigationHub(
    selected: LibraryDestination,
    onSelect: (LibraryDestination) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            LibraryDestination.entries.forEachIndexed { index, destination ->
                val active = selected == destination
                Surface(
                    onClick = { onSelect(destination) },
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = MaterialTheme.shapes.large,
                            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            destination.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                if (index < LibraryDestination.entries.lastIndex) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 70.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun LibraryRepositoryRow(
    repository: GitHubRepositoryModel,
    favourite: Boolean,
    leading: ImageVector?,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit
) {
    GlassCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading?.let { Icon(it, null, tint = MaterialTheme.colorScheme.primary) }
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
