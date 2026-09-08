package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.local.RepositoryEntity
import com.sayanthrock.githubrock.data.repository.ProfileLibraryRepository
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

enum class ProfileLibrarySection(val route: String, val title: String, val subtitle: String, val icon: ImageVector) {
    Stars("stars", "Stars", "Repositories starred on GitHub", Icons.Default.Star),
    Favourites("favourites", "Favourites", "Repositories pinned inside GitHub Rock", Icons.Default.Favorite),
    RecentlyViewed("recent", "Recently viewed", "Repositories opened on this device", Icons.Default.History);

    companion object {
        fun fromRoute(value: String?) = entries.firstOrNull { it.route.equals(value, true) } ?: Stars
    }
}

data class ProfileLibraryUiState(
    val section: ProfileLibrarySection = ProfileLibrarySection.Stars,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val favouriteKeys: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileLibraryViewModel @Inject constructor(
    private val repository: ProfileLibraryRepository,
    private val repositoryDao: RepositoryDao,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(ProfileLibraryUiState(favouriteKeys = readFavouriteKeys()))
    val state: StateFlow<ProfileLibraryUiState> = _state.asStateFlow()
    private var loadedSection: ProfileLibrarySection? = null

    fun open(section: ProfileLibrarySection) {
        if (loadedSection == section) {
            _state.update { it.copy(section = section, favouriteKeys = readFavouriteKeys()) }
            return
        }
        _state.value = ProfileLibraryUiState(section = section, favouriteKeys = readFavouriteKeys(), loading = true)
        refresh()
    }

    fun refresh() {
        val section = _state.value.section
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, favouriteKeys = readFavouriteKeys()) }
            runCatchingPreservingCancellation { loadRepositories(section) }
                .onSuccess { repositories ->
                    loadedSection = section
                    _state.update { it.copy(section = section, repositories = repositories, favouriteKeys = readFavouriteKeys(), loading = false, error = null) }
                }
                .onFailure { problem ->
                    loadedSection = section
                    _state.update { it.copy(section = section, loading = false, error = problem.libraryMessage()) }
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
                repositories = if (state.section == ProfileLibrarySection.Favourites && wasFavourite)
                    state.repositories.filterNot { it.profileLibraryKey() == key } else state.repositories
            )
        }
    }

    private suspend fun loadRepositories(section: ProfileLibrarySection): List<GitHubRepositoryModel> = when (section) {
        ProfileLibrarySection.Stars -> repository.starredRepositories()
        ProfileLibrarySection.RecentlyViewed -> repositoryDao.recent().map(RepositoryEntity::toModel)
        ProfileLibrarySection.Favourites -> loadFavouriteRepositories()
    }

    private suspend fun loadFavouriteRepositories(): List<GitHubRepositoryModel> {
        val repositories = mutableListOf<GitHubRepositoryModel>()
        readFavouriteKeys().take(MAX_FAVOURITES).forEach { key ->
            val owner = key.substringBefore('/', "")
            val name = key.substringAfter('/', "")
            if (owner.isBlank() || name.isBlank()) return@forEach
            repository.repository(owner, name)?.let(repositories::add)
        }
        return repositories
    }

    private fun readFavouriteKeys(): Set<String> = preferences.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty()
        .mapTo(linkedSetOf()) { it.trim().lowercase(Locale.ROOT) }

    private companion object {
        const val PREFERENCES_NAME = "github_rock_profile_library"
        const val KEY_FAVOURITES = "favourite_repositories"
        const val MAX_FAVOURITES = 50
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLibraryScreen(
    section: ProfileLibrarySection,
    onBack: () -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    viewModel: ProfileLibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable(section.route) { mutableStateOf("") }
    LaunchedEffect(section) { viewModel.open(section) }
    val visibleRepositories = remember(state.repositories, query) {
        val normalized = query.trim()
        if (normalized.isBlank()) state.repositories else state.repositories.filter {
            it.name.contains(normalized, true) || it.fullName.contains(normalized, true) ||
                it.description.orEmpty().contains(normalized, true) || it.language.orEmpty().contains(normalized, true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(section.title, fontWeight = FontWeight.Black)
                        Text(section.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Refresh ${section.title}") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileLibrarySummary(section.icon, section.title, section.subtitle, state.repositories.size) }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search ${section.title.lowercase()}") })
            }
            when {
                state.loading -> item { Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                state.error != null -> item {
                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(state.error!!, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Retry") } } }
                }
                visibleRepositories.isEmpty() -> item { ProfileLibraryEmpty(section, query.isNotBlank()) }
                else -> items(visibleRepositories, key = GitHubRepositoryModel::id) { repo ->
                    ProfileLibraryRepositoryCard(repo, repo.profileLibraryKey() in state.favouriteKeys, { viewModel.toggleFavourite(repo) }) { onOpenRepository(repo) }
                }
            }
        }
    }
}

@Composable
private fun ProfileLibrarySummary(icon: ImageVector, title: String, subtitle: String, count: Int) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) } }
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileLibraryRepositoryCard(repository: GitHubRepositoryModel, isFavourite: Boolean, onToggleFavourite: () -> Unit, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(repository.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(repository.owner.login, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(repository.description ?: "No repository description.", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("★ ${repository.stars}", style = MaterialTheme.typography.labelMedium)
                    Text("Forks ${repository.forks}", style = MaterialTheme.typography.labelMedium)
                    Text(repository.language ?: "Repository", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onToggleFavourite) { Icon(if (isFavourite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, if (isFavourite) "Remove from favourites" else "Add to favourites", tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ProfileLibraryEmpty(section: ProfileLibrarySection, filtered: Boolean) {
    GlassCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(section.icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (filtered) "No matching repositories" else when (section) { ProfileLibrarySection.Stars -> "No starred repositories"; ProfileLibrarySection.Favourites -> "No favourites yet"; ProfileLibrarySection.RecentlyViewed -> "No recently viewed repositories" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (filtered) "Try another repository name, owner, language, or description." else when (section) { ProfileLibrarySection.Stars -> "Repositories starred on GitHub will appear here."; ProfileLibrarySection.Favourites -> "Use the heart button in Stars or Recently viewed to pin repositories here."; ProfileLibrarySection.RecentlyViewed -> "Repositories you open in GitHub Rock will appear here." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun GitHubRepositoryModel.profileLibraryKey(): String = fullName.trim().lowercase(Locale.ROOT)

private fun RepositoryEntity.toModel(): GitHubRepositoryModel = GitHubRepositoryModel(
    id = id, name = name, fullName = fullName, owner = Owner(login = owner), description = description,
    private = isPrivate, htmlUrl = "https://github.com/$fullName", language = language, stars = stars, updatedAt = updatedAt
)

private fun Throwable.libraryMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) { 401 -> "GitHub authentication is required."; 403 -> "GitHub rate limit or access restriction reached."; 404 -> "Repository data was not found."; else -> "GitHub request failed (${code()})." }
    else -> message ?: "Unable to load repository library."
}
