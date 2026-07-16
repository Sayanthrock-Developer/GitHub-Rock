package com.sayanthrock.githubrock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.build.WorkflowMonitorScheduler
import com.sayanthrock.githubrock.core.model.*
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

data class MainUiState(
    val mode: AppMode? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: GitHubUser? = null,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val rateLimit: RateLimit? = null,
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

    init {
        if (authRepository.hasSession) connectExistingSession()
    }

    fun startLogin() {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, auth = DeviceAuthState(status = "Requesting a device code…")) }
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
        authJob?.cancel()
        _state.update { MainUiState(mode = AppMode.Guest, isLoading = true) }
        viewModelScope.launch {
            runCatching { githubRepository.publicRepositories("") }
                .onSuccess { repos -> _state.update { it.copy(isLoading = false, repositories = repos) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, message = error.userMessage()) } }
        }
    }

    fun enterDemo() {
        authJob?.cancel()
        _state.value = MainUiState(
            mode = AppMode.Demo,
            profile = DemoData.profile,
            repositories = DemoData.repositories,
            workflowRuns = DemoData.workflows,
            rateLimit = RateLimit(5_000, 4_862, 0),
            message = "Demo mode uses isolated sample data."
        )
    }

    fun searchRepositories(query: String) {
        val mode = _state.value.mode ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val result = when (mode) {
                AppMode.Demo -> Result.success(DemoData.repositories.filter { it.fullName.contains(query, true) || it.description.orEmpty().contains(query, true) })
                AppMode.Guest, AppMode.Connected -> runCatching { githubRepository.publicRepositories(query) }
            }
            result.onSuccess { repos -> _state.update { it.copy(isLoading = false, repositories = repos) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, message = error.userMessage()) } }
        }
    }

    fun refresh() {
        val mode = _state.value.mode ?: return
        if (_state.value.isLoading || _state.value.isRefreshing) return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, message = null) }
            when (mode) {
                AppMode.Connected -> loadConnectedDashboard()
                AppMode.Guest -> runCatching { githubRepository.publicRepositories("") }
                    .onSuccess { repositories ->
                        _state.update {
                            it.copy(
                                repositories = repositories,
                                isRefreshing = false
                            )
                        }
                    }
                    .onFailure { error ->
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
        if (_state.value.mode != AppMode.Demo) viewModelScope.launch { githubRepository.remember(repository) }
    }

    fun logout() {
        authJob?.cancel()
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
        _state.update { it.copy(mode = AppMode.Connected, auth = DeviceAuthState(), isLoading = true) }
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
        viewModelScope.launch {
            _state.update { it.copy(mode = AppMode.Connected, isLoading = true) }
            if (!runCatching { authRepository.refreshIfNeeded() }.getOrDefault(false)) {
                monitorScheduler.cancelAll()
                authRepository.logout()
                _state.value = MainUiState(message = "Your GitHub session expired. Please sign in again.")
            } else {
                loadConnectedDashboard()
            }
        }
    }

    private suspend fun loadConnectedDashboard() {
        runCatching { githubRepository.dashboard() }
            .onSuccess { payload ->
                _state.update {
                    it.copy(
                        mode = AppMode.Connected,
                        isLoading = false,
                        profile = payload.profile,
                        rateLimit = payload.rateLimit,
                        repositories = payload.repositories
                    )
                }
                val runs = payload.repositories.firstOrNull()?.let { repo ->
                    runCatching { githubRepository.runs(repo.owner.login, repo.name) }.getOrNull()
                }
                _state.update { current ->
                    current.copy(
                        workflowRuns = runs ?: current.workflowRuns,
                        isRefreshing = false
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        message = error.userMessage()
                    )
                }
            }
    }
}

private fun Throwable.userMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "GitHub rejected this session. Please sign in again."
        403 -> "GitHub denied this request or the API rate limit was reached."
        404 -> "The requested GitHub resource was not found."
        else -> "GitHub request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Something went wrong. Please retry."
}
