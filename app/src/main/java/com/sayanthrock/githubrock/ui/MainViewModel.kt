package com.sayanthrock.githubrock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.build.WorkflowMonitorScheduler
import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.data.auth.DeviceFlowAuthRepository
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppMode { Connected, Guest, Demo }

data class DeviceAuthState(
    val code: DeviceCodeResponse? = null,
    val status: String? = null,
    val error: String? = null
)

data class ProfileExplorerState(
    val snapshot: GitHubProfileSnapshot? = null,
    val loading: Boolean = false,
    val followUpdating: Boolean = false,
    val error: String? = null
)

data class MainUiState(
    val mode: AppMode? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: GitHubUser? = null,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val rateLimit: RateLimit? = null,
    val profileExplorer: ProfileExplorerState = ProfileExplorerState(),
    val auth: DeviceAuthState = DeviceAuthState(),
    val message: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: DeviceFlowAuthRepository,
    private val githubRepository: GitHubRepository,
    private val monitorScheduler: WorkflowMonitorScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    val loginConfigured: Boolean get() = authRepository.isConfigured

    private var authJob: Job? = null
    private var searchJob: Job? = null
    private var refreshJob: Job? = null
    private var sessionJob: Job? = null
    private var rememberJob: Job? = null
    private var profileJob: Job? = null

    init {
        if (authRepository.hasSession) connectExistingSession()
    }

    fun startLogin() {
        cancelDataJobs()
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = false,
                    message = null,
                    auth = DeviceAuthState(status = "Requesting a device code…")
                )
            }
            try {
                val code = authRepository.begin()
                _state.update {
                    it.copy(
                        isLoading = false,
                        auth = DeviceAuthState(code, "Waiting for approval on GitHub…")
                    )
                }
                completeLogin(code)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportAuthFailure(error)
            }
        }
    }

    fun checkLoginStatus() {
        val code = _state.value.auth.code ?: return
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    auth = it.auth.copy(
                        status = "Checking GitHub authorization…",
                        error = null
                    )
                )
            }
            try {
                completeLogin(code)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportAuthFailure(error)
            }
        }
    }

    fun continueAsGuest() {
        cancelAllJobs()
        _state.value = MainUiState(mode = AppMode.Guest, isLoading = true)
        searchJob = viewModelScope.launch {
            runCatchingPreservingCancellation { githubRepository.publicRepositories("") }
                .onSuccess { repos ->
                    _state.update { it.copy(isLoading = false, repositories = repos) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, message = error.userMessage()) }
                }
        }
    }

    fun enterDemo() {
        cancelAllJobs()
        _state.value = MainUiState(
            mode = AppMode.Demo,
            profile = DemoData.profile,
            repositories = DemoData.repositories,
            workflowRuns = DemoData.workflows,
            rateLimit = RateLimit(5_000, 4_862, 0),
            profileExplorer = ProfileExplorerState(
                snapshot = GitHubProfileSnapshot(DemoData.profile, DemoData.profileDetails, false)
            ),
            message = "Demo mode uses isolated sample data."
        )
    }

    fun searchRepositories(options: RepositorySearchOptions) {
        val mode = _state.value.mode ?: return
        searchJob?.cancel()
        refreshJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }
            val result = when (mode) {
                AppMode.Demo -> Result.success(
                    DemoData.repositories.filter {
                        it.fullName.contains(options.query, true) || it.description.orEmpty().contains(options.query, true)
                    }.let(options::applyLocally)
                )
                AppMode.Guest, AppMode.Connected -> runCatchingPreservingCancellation {
                    githubRepository.publicRepositories(options)
                }
            }
            result.onSuccess { repos ->
                _state.update { it.copy(isLoading = false, repositories = repos) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, message = error.userMessage()) }
            }
        }
    }

    fun refresh() {
        val mode = _state.value.mode ?: return
        if (_state.value.isLoading || _state.value.isRefreshing) return

        searchJob?.cancel()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = false, isRefreshing = true, message = null) }
            when (mode) {
                AppMode.Connected -> loadConnectedDashboard()
                AppMode.Guest -> runCatchingPreservingCancellation {
                    githubRepository.publicRepositories("")
                }.onSuccess { repositories ->
                    _state.update {
                        it.copy(
                            repositories = repositories,
                            isRefreshing = false
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            message = error.userMessage()
                        )
                    }
                }
                AppMode.Demo -> _state.update {
                    it.copy(
                        isRefreshing = false,
                        message = "Demo data is already up to date."
                    )
                }
            }
        }
    }

    fun rememberRepository(repository: GitHubRepositoryModel) {
        if (_state.value.mode == AppMode.Demo) return
        rememberJob?.cancel()
        rememberJob = viewModelScope.launch {
            runCatchingPreservingCancellation { githubRepository.remember(repository) }
                .onFailure { error ->
                    _state.update { it.copy(message = "Unable to save recent repository: ${error.userMessage()}") }
                }
        }
    }

    fun inspectProfile(login: String) {
        val normalized = normalizedGitHubLogin(login)
        if (normalized == null) {
            _state.update {
                it.copy(profileExplorer = it.profileExplorer.copy(loading = false, error = "Enter a valid GitHub username."))
            }
            return
        }
        val mode = _state.value.mode ?: return
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _state.update {
                it.copy(profileExplorer = it.profileExplorer.copy(loading = true, followUpdating = false, error = null))
            }
            val result = when (mode) {
                AppMode.Demo -> if (normalized.equals(DemoData.profile.login, ignoreCase = true)) {
                    Result.success(GitHubProfileSnapshot(DemoData.profile, DemoData.profileDetails, false))
                } else {
                    Result.failure(IllegalArgumentException("Demo mode only contains @${DemoData.profile.login}."))
                }
                AppMode.Guest, AppMode.Connected -> runCatchingPreservingCancellation {
                    val ownLogin = _state.value.profile?.login
                    githubRepository.profile(
                        login = normalized,
                        checkFollowing = mode == AppMode.Connected && !normalized.equals(ownLogin, ignoreCase = true)
                    )
                }
            }
            result.onSuccess { snapshot ->
                _state.update {
                    it.copy(profileExplorer = ProfileExplorerState(snapshot = snapshot))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        profileExplorer = it.profileExplorer.copy(
                            loading = false,
                            error = error.userMessage()
                        )
                    )
                }
            }
        }
    }

    fun setProfileFollowing(following: Boolean) {
        val current = _state.value
        val snapshot = current.profileExplorer.snapshot ?: return
        if (current.mode != AppMode.Connected || snapshot.profile.login.equals(current.profile?.login, ignoreCase = true)) return
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _state.update {
                it.copy(profileExplorer = it.profileExplorer.copy(followUpdating = true, error = null))
            }
            runCatchingPreservingCancellation {
                githubRepository.setProfileFollowing(snapshot.profile.login, following)
            }.onSuccess {
                _state.update { state ->
                    val old = state.profileExplorer.snapshot ?: return@update state
                    val oldFollowers = old.profile.followers
                    val newFollowers = when {
                        following && old.isFollowing != true -> oldFollowers + 1
                        !following && old.isFollowing == true -> (oldFollowers - 1).coerceAtLeast(0)
                        else -> oldFollowers
                    }
                    state.copy(
                        profileExplorer = state.profileExplorer.copy(
                            snapshot = old.copy(
                                profile = old.profile.copy(followers = newFollowers),
                                details = old.details?.copy(viewerIsFollowing = following),
                                isFollowing = following
                            ),
                            followUpdating = false,
                            error = null
                        )
                    )
                }
            }.onFailure { error ->
                val message = if (error is retrofit2.HttpException && error.code() == 403) {
                    "Follow access needs the user:follow permission. Sign out and authorize GitHub Rock again."
                } else {
                    error.userMessage()
                }
                _state.update {
                    it.copy(profileExplorer = it.profileExplorer.copy(followUpdating = false, error = message))
                }
            }
        }
    }

    fun logout() {
        cancelAllJobs()
        monitorScheduler.cancelAll()
        authRepository.logout()
        _state.value = MainUiState()
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private suspend fun completeLogin(code: DeviceCodeResponse) {
        authRepository.poll(code) { status ->
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    auth = current.auth.copy(status = status, error = null)
                )
            }
        }
        _state.update {
            it.copy(
                mode = AppMode.Connected,
                auth = DeviceAuthState(),
                isLoading = true,
                isRefreshing = false,
                message = null
            )
        }
        loadConnectedDashboard()
    }

    private fun reportAuthFailure(error: Exception) {
        _state.update {
            it.copy(
                isLoading = false,
                auth = it.auth.copy(error = error.userMessage())
            )
        }
    }

    private fun connectExistingSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            _state.update { it.copy(mode = AppMode.Connected, isLoading = true, message = null) }
            try {
                if (!authRepository.refreshIfNeeded()) {
                    expireSession("Your GitHub session expired. Please sign in again.")
                } else {
                    loadConnectedDashboard()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (error is retrofit2.HttpException && error.code() == 401) {
                    expireSession("Your GitHub session expired. Please sign in again.")
                } else {
                    _state.update {
                        it.copy(
                            mode = AppMode.Connected,
                            isLoading = false,
                            isRefreshing = false,
                            message = error.userMessage()
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadConnectedDashboard() {
        runCatchingPreservingCancellation { githubRepository.dashboard() }
            .onSuccess { payload ->
                _state.update {
                    it.copy(
                        mode = AppMode.Connected,
                        isLoading = false,
                        profile = payload.profile,
                        rateLimit = payload.rateLimit,
                        repositories = payload.repositories,
                        profileExplorer = if (it.profileExplorer.snapshot == null ||
                            it.profileExplorer.snapshot.profile.login.equals(payload.profile.login, ignoreCase = true)
                        ) {
                            ProfileExplorerState(snapshot = GitHubProfileSnapshot(payload.profile))
                        } else {
                            it.profileExplorer
                        }
                    )
                }

                val runsResult = payload.repositories.firstOrNull()?.let { repo ->
                    runCatchingPreservingCancellation {
                        githubRepository.runs(repo.owner.login, repo.name)
                    }
                } ?: Result.success(emptyList())

                _state.update { current ->
                    current.copy(
                        workflowRuns = runsResult.getOrDefault(emptyList()),
                        isRefreshing = false,
                        message = runsResult.exceptionOrNull()?.let {
                            "Repository data refreshed, but workflow activity is temporarily unavailable."
                        }
                    )
                }
            }
            .onFailure { error ->
                if (error is retrofit2.HttpException && error.code() == 401) {
                    expireSession("Your GitHub session expired. Please sign in again.")
                } else {
                    _state.update {
                        it.copy(
                            mode = AppMode.Connected,
                            isLoading = false,
                            isRefreshing = false,
                            message = error.userMessage()
                        )
                    }
                }
            }
    }

    private fun expireSession(message: String) {
        monitorScheduler.cancelAll()
        authRepository.logout()
        _state.value = MainUiState(message = message)
    }

    private fun cancelDataJobs() {
        searchJob?.cancel()
        refreshJob?.cancel()
        sessionJob?.cancel()
        rememberJob?.cancel()
        profileJob?.cancel()
        searchJob = null
        refreshJob = null
        sessionJob = null
        rememberJob = null
        profileJob = null
    }

    private fun cancelAllJobs() {
        authJob?.cancel()
        authJob = null
        cancelDataJobs()
    }
}

private fun Throwable.userMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "GitHub rejected this session. Please sign in again."
        403 -> "GitHub denied this request or the API rate limit was reached."
        404 -> "The requested GitHub resource was not found."
        429 -> "GitHub is receiving too many requests. Wait briefly and retry."
        in 500..599 -> "GitHub is temporarily unavailable. Please retry."
        else -> "GitHub request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Something went wrong. Please retry."
}
