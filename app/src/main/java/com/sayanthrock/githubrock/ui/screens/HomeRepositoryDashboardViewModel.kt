package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.data.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeRepositoryDashboardViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val repository: GitHubRepository,
) : ViewModel() {
    val enabled: StateFlow<Boolean> = preferences.appearance
        .map { it.repositoryDashboard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _watched = MutableStateFlow<Set<String>>(emptySet())
    val watched: StateFlow<Set<String>> = _watched
    private val _watchError = MutableStateFlow<String?>(null)
    val watchError: StateFlow<String?> = _watchError

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { preferences.setRepositoryDashboard(value) }
    }

    fun toggleWatch(item: GitHubRepositoryModel) {
        viewModelScope.launch {
            val key = item.fullName.lowercase()
            _watchError.value = null
            runCatching {
                val current = repository.isRepositoryWatched(item.owner.login, item.name)
                if (current) {
                    check(repository.setRepositoryWatched(item.owner.login, item.name, false)) {
                        "Unable to stop watching ${item.fullName}"
                    }
                } else {
                    check(repository.setRepositoryWatched(item.owner.login, item.name, true)) {
                        "Unable to watch ${item.fullName}"
                    }
                }
                _watched.update { currentSet ->
                    if (current) currentSet - key else currentSet + key
                }
            }.onFailure { error ->
                _watchError.value = error.message ?: "Unable to update repository watch status."
            }
        }
    }
}
