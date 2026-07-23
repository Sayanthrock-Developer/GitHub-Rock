package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.local.RepositoryEntity
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileLibrarySection(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    Stars(
        route = "stars",
        title = "Stars",
        subtitle = "Repositories starred on GitHub",
        icon = Icons.Default.Star
    ),
    Favourites(
        route = "favourites",
        title = "Favourites",
        subtitle = "Repositories pinned inside GitHub Rock",
        icon = Icons.Default.Favorite
    ),
    RecentlyViewed(
        route = "recent",
        title = "Recently viewed",
        subtitle = "Repositories opened on this device",
        icon = Icons.Default.History
    );

    companion object {
        fun fromRoute(value: String?): ProfileLibrarySection =
            entries.firstOrNull { it.route.equals(value, ignoreCase = true) } ?: Stars
    }
}

data class ProfileLibraryUiState(
    val section: ProfileLibrarySection,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val favouriteKeys: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProfileLibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: GitHubRestApi,
    private val repositoryDao: RepositoryDao,
    @ApplicationContext context: Context
) : ViewModel() {
    private val section = ProfileLibrarySection.fromRoute(savedStateHandle.get<String>("section"))
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        ProfileLibraryUiState(
            section = section,
            favouriteKeys = readFavouriteKeys()
        )
    )
    val state: StateFlow<ProfileLibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, favouriteKeys = readFavouriteKeys()) }
            runCatchingPreservingCancellation { loadRepositories(section) }
                .onSuccess { repositories ->
                    _state.update {
                        it.copy(
                            repositories = repositories,
                            favouriteKeys = readFavouriteKeys(),
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure { problem ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = problem.libraryMessage()
                        )
                    }
                }
        }
    }

    fun toggleFavourite(repository: GitHubRepositoryModel) {
        val key = repository.profileLibraryKey()
        val current = readFavouriteKeys().toMutableSet()
        val wasFavourite = !current.add(key)
        if (wasFavourite) current.remove(key)
        preferences.edit().putStringSet(KEY_FAVOURITES, current).apply()

        _state.update { state ->
            state.copy(
                favouriteKeys = current,
                repositories = if (state.section == ProfileLibrarySection.Favourites && wasFavourite) {
                    state.repositories.filterNot { it.profileLibraryKey() == key }
                } else {
                    state.repositories
                }
            )
        }
    }

    private suspend fun loadRepositories(section: ProfileLibrarySection): List<GitHubRepositoryModel> = when (section) {
        ProfileLibrarySection.Stars -> api.starredRepositories()
        ProfileLibrarySection.RecentlyViewed -> repositoryDao.recent().map(RepositoryEntity::toModel)
        ProfileLibrarySection.Favourites -> loadFavouriteRepositories()
    }

    private suspend fun loadFavouriteRepositories(): List<GitHubRepositoryModel> {
        val repositories = mutableListOf<GitHubRepositoryModel>()
        readFavouriteKeys().take(MAX_FAVOURITES).forEach { key ->
            val owner = key.substringBefore('/', missingDelimiterValue = "")
            val repository = key.substringAfter('/', missingDelimiterValue = "")
            if (owner.isBlank() || repository.isBlank()) return@forEach
            runCatchingPreservingCancellation { api.repository(owner, repository) }
                .getOrNull()
                ?.let(repositories::add)
        }
        return repositories
    }

    private fun readFavouriteKeys(): Set<String> =
        preferences.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty().mapTo(linkedSetOf()) {
            it.trim().lowercase(Locale.ROOT)
        }

    private companion object {
        const val PREFERENCES_NAME = "github_rock_profile_library"
        const val KEY_FAVOURITES = "favourite_repositories"
        const val MAX_FAVOURITES = 50
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLibraryScreen(
    onBack: () -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    viewModel: ProfileLibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable(state.section.route) { mutableStateOf("") }
    val visibleRepositories = remember(state.repositories, query) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            state.repositories
        } else {
            state.repositories.filter { repository ->
                repository.name.contains(normalized, ignoreCase = true) ||
                    repository.fullName.contains(normalized, ignoreCase = true) ||
                    repository.description.orEmpty().contains(normalized, ignoreCase = true) ||
                    repository.language.orEmpty().contains(normalized, ignoreCase = true)
            }
        }
    }
    val errorMessage = state.error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.section.title, fontWeight = FontWeight.Black)
                        Text(
                            state.section.subtitle,
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh ${state.section.title}")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileLibrarySummary(
                    icon = state.section.icon,
                    title = state.section.title,
                    subtitle = state.section.subtitle,
                    count = state.repositories.size
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search ${state.section.title.lowercase()}") }
                )
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = viewModel::refresh) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                visibleRepositories.isEmpty() -> item {
                    ProfileLibraryEmpty(state.section, query.isNotBlank())
                }

                else -> items(visibleRepositories, key = GitHubRepositoryModel::id) { repository ->
                    ProfileLibraryRepositoryCard(
                        repository = repository,
                        isFavourite = repository.profileLibraryKey() in state.favouriteKeys,
                        onToggleFavourite = { viewModel.toggleFavourite(repository) },
                        onClick = { onOpenRepository(repository) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileLibrarySummary(
    icon: ImageVector,
    title: String,
    subtitle: String,
    count: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileLibraryRepositoryCard(
    repository: GitHubRepositoryModel,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    repository.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    repository.owner.login,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    repository.description ?: "No repository description.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("★ ${repository.stars}", style = MaterialTheme.typography.labelMedium)
                    Text("Forks ${repository.forks}", style = MaterialTheme.typography.labelMedium)
                    Text(
                        repository.language ?: "Repository",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileLibraryEmpty(section: ProfileLibrarySection, filtered: Boolean) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                if (filtered) "No matching repositories" else when (section) {
                    ProfileLibrarySection.Stars -> "No starred repositories"
                    ProfileLibrarySection.Favourites -> "No favourites yet"
                    ProfileLibrarySection.RecentlyViewed -> "No recently viewed repositories"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (filtered) {
                    "Try another repository name, owner, language, or description."
                } else when (section) {
                    ProfileLibrarySection.Stars -> "Repositories starred on GitHub will appear here."
                    ProfileLibrarySection.Favourites -> "Use the heart button in Stars or Recently viewed to pin repositories here."
                    ProfileLibrarySection.RecentlyViewed -> "Repositories you open in GitHub Rock will appear here."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

internal fun GitHubRepositoryModel.profileLibraryKey(): String =
    fullName.trim().lowercase(Locale.ROOT)

private fun RepositoryEntity.toModel(): GitHubRepositoryModel = GitHubRepositoryModel(
    id = id,
    name = name,
    fullName = fullName,
    owner = Owner(login = owner),
    description = description,
    private = isPrivate,
    htmlUrl = "https://github.com/$fullName",
    language = language,
    stars = stars,
    updatedAt = updatedAt
)

private fun Throwable.libraryMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "Your GitHub session expired. Sign in again."
        403 -> "GitHub denied this request or the API limit was reached."
        else -> "GitHub library request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Unable to load this repository library."
}
