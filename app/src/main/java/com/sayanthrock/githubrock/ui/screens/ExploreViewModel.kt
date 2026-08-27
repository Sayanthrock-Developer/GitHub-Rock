package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: GitHubRepository,
) : ViewModel() {
    data class State(
        val repositories: List<GitHubRepositoryModel> = emptyList(),
        val query: String = "",
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val error: String? = null,
        val mode: ExploreMode = ExploreMode.Trending,
    )

    enum class ExploreMode(val title: String, val description: String) {
        Trending("Trending", "Repositories gaining attention"),
        Popular("Popular", "Highly starred projects"),
        New("Recently created", "Fresh projects worth discovering"),
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { load() }

    fun load(mode: ExploreMode = _state.value.mode, query: String = _state.value.query, refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(mode = mode, query = query, loading = !refresh, refreshing = refresh, error = null) }
            val options = when (mode) {
                ExploreMode.Trending -> RepositorySearchOptions(query = query.ifBlank { "stars:>1000" }, sort = RepositorySort.Stars)
                ExploreMode.Popular -> RepositorySearchOptions(query = query.ifBlank { "stars:>500" }, sort = RepositorySort.Stars)
                ExploreMode.New -> RepositorySearchOptions(query = query.ifBlank { "created:>2026-01-01" }, sort = RepositorySort.Updated)
            }
            runCatching { repository.publicRepositories(options) }
                .onSuccess { repos -> _state.update { it.copy(repositories = repos, loading = false, refreshing = false) } }
                .onFailure { error -> _state.update { it.copy(loading = false, refreshing = false, error = error.message ?: "Unable to load GitHub discovery.") } }
        }
    }

    fun search(query: String) = load(_state.value.mode, query)
    fun refresh() = load(refresh = true)
    fun clearError() = _state.update { it.copy(error = null) }
}
