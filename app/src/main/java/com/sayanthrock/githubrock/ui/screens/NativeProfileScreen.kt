package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.repository.GitHubProfileDetailsResolver
import com.sayanthrock.githubrock.data.repository.NativeProfileRepository
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class NativeProfileUiState(
    val profile: GitHubUser? = null,
    val details: GitHubProfileDetails? = null,
    val section: NativeProfileSection = NativeProfileSection.Repositories,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val people: List<GitHubUser> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val canFollow: Boolean = false,
    val isOwnProfile: Boolean = false,
    val followStateLoaded: Boolean = false,
    val isFollowing: Boolean = false,
    val followUpdating: Boolean = false,
    val followError: String? = null
)

@HiltViewModel
class NativeProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NativeProfileRepository,
    private val detailsResolver: GitHubProfileDetailsResolver
) : ViewModel() {
    private val login: String = checkNotNull(savedStateHandle["login"])
    private val initialSection = NativeProfileSection.fromRoute(savedStateHandle["section"])
    private val _state = MutableStateFlow(NativeProfileUiState(section = initialSection))
    val state: StateFlow<NativeProfileUiState> = _state.asStateFlow()
    private var contentJob: Job? = null
    private var followJob: Job? = null
    private var followConfiguration: Pair<Boolean, String?>? = null

    init {
        load(initialSection)
    }

    fun configureFollow(connected: Boolean, ownLogin: String?) {
        val normalizedOwnLogin = normalizedGitHubLogin(ownLogin)
        val isOwnProfile = normalizedOwnLogin?.equals(login, ignoreCase = true) == true
        val configuration = connected to normalizedOwnLogin
        if (followConfiguration == configuration && _state.value.followStateLoaded) return
        followConfiguration = configuration
        val canFollow = connected && !isOwnProfile
        _state.update {
            it.copy(
                canFollow = canFollow,
                isOwnProfile = isOwnProfile,
                followStateLoaded = !canFollow,
                followError = null
            )
        }
        if (canFollow) loadFollowStatus()
    }

    fun selectSection(section: NativeProfileSection) {
        if (section == _state.value.section && !_state.value.loading) return
        load(section)
    }

    fun refresh() {
        load(_state.value.section)
        if (_state.value.canFollow) loadFollowStatus()
    }

    fun toggleFollow() {
        val current = _state.value
        if (!current.canFollow || !current.followStateLoaded || current.followUpdating) return
        val desired = !current.isFollowing
        followJob?.cancel()
        followJob = viewModelScope.launch {
            _state.update { it.copy(followUpdating = true, followError = null) }
            runCatchingPreservingCancellation { repository.setFollowing(login, desired) }
                .onSuccess { accepted ->
                    if (!accepted) {
                        _state.update {
                            it.copy(
                                followUpdating = false,
                                followError = "GitHub did not accept the follow change. Sign in again if Follow permission was not granted."
                            )
                        }
                    } else {
                        _state.update { state ->
                            val delta = if (desired) 1 else -1
                            state.copy(
                                profile = state.profile?.copy(
                                    followers = (state.profile.followers + delta).coerceAtLeast(0)
                                ),
                                isFollowing = desired,
                                followUpdating = false,
                                followError = null
                            )
                        }
                    }
                }
                .onFailure { problem ->
                    _state.update {
                        it.copy(followUpdating = false, followError = problem.profileMessage(followAction = true))
                    }
                }
        }
    }

    private fun load(section: NativeProfileSection) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            _state.update {
                it.copy(section = section, loading = true, error = null, people = emptyList())
            }
            runCatchingPreservingCancellation {
                coroutineScope {
                    val profileTask = async { repository.profile(login) }
                    val detailsTask = async { runCatchingPreservingCancellation { detailsResolver.resolve(login) }.getOrNull() }
                    val repositoriesTask = async {
                        if (_state.value.repositories.isNotEmpty()) _state.value.repositories else repository.repositories(login)
                    }
                    val peopleTask = async {
                        when (section) {
                            NativeProfileSection.Repositories -> emptyList()
                            NativeProfileSection.Followers -> repository.followers(login)
                            NativeProfileSection.Following -> repository.following(login)
                        }
                    }
                    LoadedProfile(
                        profile = profileTask.await(),
                        details = detailsTask.await(),
                        repositories = repositoriesTask.await(),
                        people = peopleTask.await()
                    )
                }
            }.onSuccess { loaded ->
                _state.update {
                    it.copy(
                        profile = loaded.profile,
                        details = loaded.details,
                        repositories = loaded.repositories,
                        people = loaded.people,
                        loading = false,
                        error = null
                    )
                }
            }.onFailure { problem ->
                _state.update { it.copy(loading = false, error = problem.profileMessage()) }
            }
        }
    }

    private fun loadFollowStatus() {
        followJob?.cancel()
        followJob = viewModelScope.launch {
            _state.update { it.copy(followStateLoaded = false, followError = null) }
            runCatchingPreservingCancellation { repository.isFollowing(login) }
                .onSuccess { following ->
                    _state.update { it.copy(isFollowing = following, followStateLoaded = true) }
                }
                .onFailure { problem ->
                    _state.update {
                        it.copy(
                            followStateLoaded = true,
                            followError = problem.profileMessage(followAction = true)
                        )
                    }
                }
        }
    }

    private data class LoadedProfile(
        val profile: GitHubUser,
        val details: GitHubProfileDetails?,
        val repositories: List<GitHubRepositoryModel>,
        val people: List<GitHubUser>
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeProfileScreen(
    mode: AppMode,
    ownLogin: String?,
    onBack: () -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: NativeProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.error
    val context = LocalContext.current
    var query by rememberSaveable(state.profile?.login) { mutableStateOf("") }
    var filter by rememberSaveable(state.profile?.login) { mutableStateOf(ProfileRepositoryFilter.All) }

    LaunchedEffect(mode, ownLogin) {
        viewModel.configureFollow(mode == AppMode.Connected, ownLogin)
    }

    val filteredRepositories = remember(state.repositories, query, filter) {
        filterProfileRepositories(state.repositories, query, filter)
    }

    val openExternal: (String) -> Unit = { rawUrl ->
        val url = if (rawUrl.startsWith("https://", ignoreCase = true)) rawUrl else "https://$rawUrl"
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.profile?.login ?: "GitHub profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.profile?.login?.let { login ->
                        IconButton(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://github.com/$login")
                                }
                                context.startActivity(Intent.createChooser(share, "Share profile"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share profile")
                        }
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileDashboardHeader(
                    profile = state.profile,
                    isOwnProfile = state.isOwnProfile,
                    canFollow = state.canFollow,
                    followStateLoaded = state.followStateLoaded,
                    isFollowing = state.isFollowing,
                    followUpdating = state.followUpdating,
                    onToggleFollow = viewModel::toggleFollow,
                    onRepositories = { viewModel.selectSection(NativeProfileSection.Repositories) },
                    onFollowers = { viewModel.selectSection(NativeProfileSection.Followers) },
                    onFollowing = { viewModel.selectSection(NativeProfileSection.Following) }
                )
            }

            item { ContributionActivityCard(state.details) }
            item { ProfileIdentitySummary(state.profile, state.details, openExternal) }
            item {
                ProfileSectionSelector(
                    selected = state.section,
                    onSelected = viewModel::selectSection
                )
            }

            state.followError?.let { message ->
                item { GlassCard { Text(message, color = MaterialTheme.colorScheme.error) } }
            }

            if (state.section == NativeProfileSection.Repositories) {
                item {
                    ProfileRepositoryToolbar(
                        query = query,
                        onQueryChange = { query = it },
                        selectedFilter = filter,
                        onFilterChange = { filter = it },
                        shown = filteredRepositories.size,
                        total = state.repositories.size
                    )
                }
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 54.dp),
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
                state.section == NativeProfileSection.Repositories && filteredRepositories.isEmpty() -> item {
                    ProfileEmpty(
                        if (query.isBlank() && filter == ProfileRepositoryFilter.All) {
                            "No public repositories to show."
                        } else {
                            "No repositories match this search and filter."
                        }
                    )
                }
                state.section != NativeProfileSection.Repositories && state.people.isEmpty() -> item {
                    ProfileEmpty("No ${state.section.title.lowercase()} to show.")
                }
                state.section == NativeProfileSection.Repositories -> items(
                    items = filteredRepositories,
                    key = GitHubRepositoryModel::id
                ) { repository ->
                    ProfileRepositoryCard(repository) { onOpenRepository(repository) }
                }
                else -> items(items = state.people, key = GitHubUser::id) { person ->
                    ProfilePersonCard(person) { onOpenProfile(person.login) }
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionSelector(
    selected: NativeProfileSection,
    onSelected: (NativeProfileSection) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            NativeProfileSection.entries.forEach { section ->
                Surface(
                    onClick = { onSelected(section) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = if (selected == section) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected == section) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        section.title,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected == section) FontWeight.Black else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEmpty(message: String) {
    GlassCard {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun Throwable.profileMessage(followAction: Boolean = false): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Your GitHub session expired. Sign in again."
        403 -> if (followAction) {
            "GitHub denied Follow access. Sign out and sign in again to grant user:follow permission."
        } else {
            "GitHub denied this profile request or the API limit was reached."
        }
        404 -> "This GitHub profile is unavailable."
        else -> "GitHub profile request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Unable to load this GitHub profile."
}
