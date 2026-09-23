package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.core.util.ReleaseAssetClassifier
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepositoryExploreMode { Daily }

data class RepositoryExploreItem(
    val repository: GitHubRepositoryModel,
    val releaseLabel: String
)

@HiltViewModel
class RepositoryExploreTabViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {
    data class State(
        val items: List<RepositoryExploreItem> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var page = 0
    private var loadedDay: LocalDate? = null
    private var selectedPlatform = HomePlatform.All
    private var requestGeneration = 0L

    fun load(platform: HomePlatform, refresh: Boolean = false) {
        val platformChanged = platform != selectedPlatform
        if (!refresh && !platformChanged && loadedDay == LocalDate.now() && _state.value.items.isNotEmpty()) return

        selectedPlatform = platform
        page = 0
        loadedDay = LocalDate.now()
        val generation = ++requestGeneration

        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    items = if (refresh || platformChanged) emptyList() else it.items,
                    hasMore = true
                )
            }
            runCatching {
                repository.publicRepositoriesPage(
                    RepositorySearchOptions(
                        query = discoveryQuery(platform),
                        sort = RepositorySort.Stars
                    ),
                    page = 1
                )
            }.onSuccess { result ->
                if (generation != requestGeneration) return@onSuccess
                val enriched = enrich(result.repositories, platform)
                if (generation != requestGeneration) return@onSuccess
                page = 1
                _state.update { it.copy(items = enriched, loading = false, hasMore = result.hasMore) }
            }.onFailure { error ->
                if (generation == requestGeneration) {
                    _state.update { it.copy(loading = false, error = error.message ?: "Unable to load Explore.") }
                }
            }
        }
    }

    fun loadMore(platform: HomePlatform) {
        if (_state.value.loading || _state.value.loadingMore || !_state.value.hasMore) return

        selectedPlatform = platform
        val requestedPage = page + 1
        val generation = requestGeneration

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            runCatching {
                repository.publicRepositoriesPage(
                    RepositorySearchOptions(
                        query = discoveryQuery(platform),
                        sort = RepositorySort.Stars
                    ),
                    page = requestedPage
                )
            }.onSuccess { result ->
                if (generation != requestGeneration) return@onSuccess
                val enriched = enrich(result.repositories, platform)
                if (generation != requestGeneration) return@onSuccess
                page = requestedPage
                _state.update { current ->
                    current.copy(
                        items = (current.items + enriched).distinctBy { it.repository.id },
                        loadingMore = false,
                        hasMore = result.hasMore
                    )
                }
            }.onFailure { error ->
                if (generation == requestGeneration) {
                    _state.update { it.copy(loadingMore = false, error = error.message ?: "Unable to load more.") }
                }
            }
        }
    }

    private suspend fun enrich(
        repositories: List<GitHubRepositoryModel>,
        platform: HomePlatform
    ): List<RepositoryExploreItem> {
        return repositories.take(12).map { repo ->
            repo to viewModelScope.async {
                runCatching {
                    repository.releases(repo.owner.login, repo.name)
                        .firstOrNull { release ->
                            !release.draft &&
                                !release.prerelease &&
                                release.assets.any { asset ->
                                    val info = ReleaseAssetClassifier.classify(asset.name)
                                    info.isInstallablePackage &&
                                        (platform == HomePlatform.All || info.platform.name.equals(platform.name, true))
                                }
                        }
                }.getOrNull()
            }
        }.map { (repo, deferred) ->
            repo to deferred.await()
        }.mapNotNull { (repo, release) ->
            val asset = release?.assets?.firstOrNull { asset ->
                val info = ReleaseAssetClassifier.classify(asset.name)
                info.isInstallablePackage &&
                    (platform == HomePlatform.All || info.platform.name.equals(platform.name, true))
            } ?: return@mapNotNull null
            RepositoryExploreItem(repo, "\${release.tagName} · \${asset.name}")
        }
    }

    private fun discoveryQuery(platform: HomePlatform): String = when (platform) {
        HomePlatform.Android -> "android stars:>10"
        HomePlatform.MacOS -> "macos stars:>10"
        HomePlatform.Windows -> "windows stars:>10"
        HomePlatform.Linux -> "linux stars:>10"
        HomePlatform.IOS -> "ios stars:>10"
        HomePlatform.All -> "stars:>10"
    }
}
