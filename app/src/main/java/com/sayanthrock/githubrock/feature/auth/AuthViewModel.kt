package com.sayanthrock.githubrock.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.auth.AuthRepository
import com.sayanthrock.githubrock.core.auth.DeviceAuthUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AuthUiState(
    val initializing: Boolean = true,
    val isAuthenticated: Boolean = false,
    val clientConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            val authenticated = runCatching { repository.refreshIfNeeded() }
                .getOrDefault(repository.hasSession())
            _state.value = AuthUiState(
                initializing = false,
                isAuthenticated = authenticated,
                clientConfigured = repository.isClientConfigured()
            )
        }
    }

    /**
     * Starts the GitHub device authentication flow and updates the UI state with its progress.
     */
    fun startDeviceFlow() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, errorMessage = null, statusMessage = "Requesting a GitHub verification code…")
            }
            runCatching { repository.requestDeviceCode() }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, statusMessage = null, errorMessage = error.message ?: "Unable to start GitHub sign-in.")
                    }
                }
                .onSuccess { code ->
                    _state.update {
                        it.copy(
                            isLoading = true,
                            userCode = code.userCode,
                            verificationUri = code.verificationUri,
                            statusMessage = "Enter this code on GitHub, then return to the app."
                        )
                    }
                    repository.pollForToken(code)
                        .catch { error ->
                            _state.update {
                                it.copy(isLoading = false, errorMessage = error.message ?: "GitHub sign-in failed.")
                            }
                        }
                        .collect { update ->
                            when (update) {
                                is DeviceAuthUpdate.Waiting -> _state.update {
                                    it.copy(statusMessage = "Waiting for GitHub authorization…")
                                }
                                is DeviceAuthUpdate.SlowDown -> _state.update {
                                    it.copy(statusMessage = "GitHub requested slower polling. Waiting safely…")
                                }
                                DeviceAuthUpdate.Success -> _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isAuthenticated = true,
                                        userCode = null,
                                        verificationUri = null,
                                        statusMessage = "Connected to GitHub.",
                                        errorMessage = null
                                    )
                                }
                                is DeviceAuthUpdate.Failure -> _state.update {
                                    it.copy(isLoading = false, statusMessage = null, errorMessage = update.message)
                                }
                            }
                        }
                }
        }
    }

    /**
     * Logs out the current user and resets the authentication state.
     */
    fun logout() {
        pollingJob?.cancel()
        repository.logout()
        _state.update {
            AuthUiState(initializing = false, clientConfigured = repository.isClientConfigured())
        }
    }
}
