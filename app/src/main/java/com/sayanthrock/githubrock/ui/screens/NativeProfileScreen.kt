package com.sayanthrock.githubrock.ui.screens

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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
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

internal data class NativeProfileUiState(
    val profile: GitHubUser? = null,
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
internal class NativeProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NativeProfileRepository
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
        val isOwnProfile = normalizedOwnLogin.equals(login, ignoreCase = true)
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
                .onSuccess { success ->
                    if (!success) {
                        _state.update {
                            it.copy(
                                followUpdating = false,
                                followError = "GitHub did not accept the follow change. Sign out and sign in again if Follow permission was not granted."
                            )
                        }
                        return@onSuccess
                    }
                    _state.update { state ->
                        val followerDelta = if (desired) 1 else -1
                        state.copy(
                            profile = state.profile?.copy(
                                followers = (state.profile.followers + followerDelta).coerceAtLeast(0)
                            ),
                            isFollowing = desired,
                            followUpdating = false,
                            followError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            followUpdating = false,
                            followError = error.profileMessage(followAction = true)
                        )
                    }
                }
        }
    }

    private fun load(section: NativeProfileSection) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    section = section,
                    loading = true,
                    error = null,
                    repositories = if (section == NativeProfileSection.Repositories) it.repositories else emptyList(),
                    people = if (section == NativeProfileSection.Repositories) emptyList() else it.people
                )
            }

            val result = runCatchingPreservingCancellation {
                coroutineScope {
                    val profile = async { repository.profile(login) }
                    val content = async {
                        when (section) {
                            NativeProfileSection.Repositories -> repository.repositories(login)
                            NativeProfileSection.Followers -> repository.followers(login)
                            NativeProfileSection.Following -> repository.following(login)
                        }
                    }
                    profile.await() to content.await()
                }
            }

            result.onSuccess { (profile, content) ->
                _state.update {
                    when (section) {
                        NativeProfileSection.Repositories -> it.copy(
                            profile = profile,
                            repositories = content.filterIsInstance<GitHubRepositoryModel>(),
                            people = emptyList(),
                            loading = false,
                            error = null
                        )
                        NativeProfileSection.Followers,
                        NativeProfileSection.Following -> it.copy(
                            profile = profile,
                            repositories = emptyList(),
                            people = content.filterIsInstance<GitHubUser>(),
                            loading = false,
                            error = null
                        )
                    }
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.profileMessage()) }
            }
        }
    }

    private fun loadFollowStatus() {
        followJob?.cancel()
        followJob = viewModelScope.launch {
            _state.update { it.copy(followStateLoaded = false, followError = null) }
            runCatchingPreservingCancellation { repository.isFollowing(login) }
                .onSuccess { following ->
                    _state.update {
                        it.copy(
                            isFollowing = following,
                            followStateLoaded = true,
                            followError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            followStateLoaded = true,
                            followError = error.profileMessage(followAction = true)
                        )
                    }
                }
        }
    }
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

    LaunchedEffect(mode, ownLogin) {
        viewModel.configureFollow(
            connected = mode == AppMode.Connected,
            ownLogin = ownLogin
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.profile?.name ?: state.profile?.login ?: "GitHub profile",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        state.profile?.login?.let {
                            Text(
                                text = "@$it",
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NativeProfileHeader(
                    state = state,
                    onToggleFollow = viewModel::toggleFollow
                )
            }
            item {
                ProfileSectionTabs(
                    selected = state.section,
                    onSelected = viewModel::selectSection
                )
            }

            state.followError?.let { message ->
                item {
                    GlassCard {
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = viewModel::refresh) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                state.section == NativeProfileSection.Repositories && state.repositories.isEmpty() -> item {
                    NativeProfileEmpty("No public repositories to show.")
                }
                state.section != NativeProfileSection.Repositories && state.people.isEmpty() -> item {
                    NativeProfileEmpty("No ${state.section.title.lowercase()} to show.")
                }
                state.section == NativeProfileSection.Repositories -> items(
                    items = state.repositories,
                    key = { it.id }
                ) { repository ->
                    RepositoryCard(repository, onClick = { onOpenRepository(repository) })
                }
                else -> items(
                    items = state.people,
                    key = { it.id }
                ) { person ->
                    NativeProfilePersonCard(person) { onOpenProfile(person.login) }
                }
            }
        }
    }
}

@Composable
private fun NativeProfileHeader(
    state: NativeProfileUiState,
    onToggleFollow: () -> Unit
) {
    val profile = state.profile
    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "${profile?.login} avatar",
                        modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.extraLarge)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = profile?.login?.take(2)?.uppercase() ?: "GH",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile?.name ?: profile?.login ?: "Loading profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    profile?.login?.let {
                        Text(
                            text = "@$it",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    profile?.bio?.takeIf(String::isNotBlank)?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetric("Repositories", profile?.publicRepos ?: 0, Modifier.weight(1f))
                ProfileMetric("Followers", profile?.followers ?: 0, Modifier.weight(1f))
                ProfileMetric("Following", profile?.following ?: 0, Modifier.weight(1f))
            }

            if (state.canFollow) {
                Button(
                    onClick = onToggleFollow,
                    enabled = state.followStateLoaded && !state.followUpdating,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (!state.followStateLoaded || state.followUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isFollowing) "Unfollow" else "Follow", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (state.isOwnProfile) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .24f))
                ) {
                    Text(
                        text = "This is your connected GitHub account",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionTabs(
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
                    color = if (section == selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (section == selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Text(
                        text = section.title,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (section == selected) FontWeight.Black else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NativeProfilePersonCard(person: GitHubUser, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (person.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = person.avatarUrl,
                    contentDescription = "${person.login} avatar",
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.extraLarge)
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = person.login.take(2).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name ?: person.login,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${person.login}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                person.bio?.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeProfileEmpty(message: String) {
    GlassCard {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun Throwable.profileMessage(followAction: Boolean = false): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "Your GitHub session expired. Sign in again."
        403 -> if (followAction) {
            "GitHub denied Follow access. Sign out and sign in again to grant the user:follow permission."
        } else {
            "GitHub denied this profile request or the API limit was reached."
        }
        404 -> "This GitHub profile is unavailable."
        else -> "GitHub profile request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Unable to load this GitHub profile."
}
