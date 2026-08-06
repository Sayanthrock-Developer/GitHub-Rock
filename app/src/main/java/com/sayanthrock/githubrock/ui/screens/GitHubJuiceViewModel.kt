package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.data.repository.GitHubJuiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.serialization.Serializable

@Serializable
data class GitHubJuiceState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dailySummary: String = "Loading...",
    val repositoryHealthScore: Int = 0,
    val commitActivity: String = "Loading...",
    val openIssuesSummary: String = "Loading...",
    val pullRequestStatus: String = "Loading...",
    val workflowStatus: String = "Loading...",
    val recentReleases: List<String> = emptyList(),
    val trendingRepositories: List<GitHubRepositoryModel> = emptyList(),
    val trendingDevelopers: List<String> = emptyList(),
    val repositoryGrowth: String = "Loading...",
    val starGrowth: String = "Loading...",
    val forkGrowth: String = "Loading...",
    val topContributors: List<String> = emptyList(),
    val recentContributors: List<String> = emptyList(),
    val commitStreak: Int = 0,
    val languageBreakdown: Map<String, Double> = emptyMap(),
    val repositorySize: String = "Loading...",
    val licenseDetection: String = "Loading...",
    val readmeStatus: String = "Loading...",
    val latestTags: List<String> = emptyList(),
    val securityAdvisories: List<String> = emptyList(),
    val codeFrequency: String = "Loading...",
    val activityTimeline: List<String> = emptyList(),
    val repositoryInsights: String = "Loading...",
    val contributorLeaderboard: List<String> = emptyList(),
    val recentlyUpdatedRepositories: List<GitHubRepositoryModel> = emptyList(),
    val recentlyStarredRepositories: List<GitHubRepositoryModel> = emptyList(),
    val savedRepositories: List<GitHubRepositoryModel> = emptyList()
)

@HiltViewModel
class GitHubJuiceViewModel @Inject constructor(
    private val repository: GitHubJuiceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GitHubJuiceState())
    val state: StateFlow<GitHubJuiceState> = _state.asStateFlow()

    private var hasLoaded = false

    init {
        loadJuiceData()
    }

    fun loadJuiceData() {
        if (hasLoaded) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Try cache first
            repository.getCachedState()?.let { cached ->
                _state.value = cached.copy(isLoading = true, error = null)
            }

            try {
                val newState = repository.fetchJuiceState()
                _state.value = newState
                repository.saveCachedState(newState)
                hasLoaded = true
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "An error occurred fetching GitHub Juice insights.") }
            }
        }
    }
}
