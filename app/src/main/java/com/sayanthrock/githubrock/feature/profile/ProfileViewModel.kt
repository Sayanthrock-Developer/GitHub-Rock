package com.sayanthrock.githubrock.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubUserDto
import com.sayanthrock.githubrock.core.model.RateLimitCore
import com.sayanthrock.githubrock.core.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ProfileUiState(
    val loading: Boolean = false,
    val user: GitHubUserDto? = null,
    val rateLimit: RateLimitCore? = null,
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(demoMode: Boolean, guestMode: Boolean) {
        viewModelScope.launch {
            if (demoMode) {
                _state.value = ProfileUiState(
                    user = GitHubUserDto(
                        login = "SayanthRock-demo",
                        id = 1,
                        name = "Sayanth Rock",
                        bio = "Demo account — no GitHub data is connected.",
                        avatarUrl = null,
                        htmlUrl = "https://github.com/SayanthRock",
                        publicRepos = 24,
                        followers = 128,
                        following = 42
                    ),
                    rateLimit = RateLimitCore(5000, 180, 4820, 0)
                )
                return@launch
            }

            _state.value = ProfileUiState(loading = true)
            runCatching {
                if (guestMode) {
                    null to repository.rateLimit()
                } else {
                    val user = async { repository.profile() }
                    val rate = async { repository.rateLimit() }
                    user.await() to rate.await()
                }
            }.onSuccess { (user, rate) ->
                _state.value = ProfileUiState(user = user, rateLimit = rate)
            }.onFailure { error ->
                _state.value = ProfileUiState(message = error.message ?: "Unable to load profile.")
            }
        }
    }
}
