package com.sayanthrock.githubrock.feature.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sayanthrock.githubrock.core.model.RepositorySummary
import com.sayanthrock.githubrock.core.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {
    private val demoMode = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")

    val repositories: Flow<PagingData<RepositorySummary>> = combine(
        demoMode,
        searchQuery
    ) { demo, query -> demo to query }
        .flatMapLatest { (demo, query) -> repository.repositories(demo, query).flow }
        .cachedIn(viewModelScope)

    fun setDemoMode(enabled: Boolean) {
        demoMode.value = enabled
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun markOpened(item: RepositorySummary) {
        viewModelScope.launch { repository.markOpened(item) }
    }
}
