package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State for the single-screen repository experience. */
data class RepositoryHubState(
    val repository: GitHubRepositoryModel? = null,
    val releases: List<Release> = emptyList(),
    val readme: String? = null,
    val loading: Boolean = true,
    val releasesLoading: Boolean = true,
    val readmeLoading: Boolean = true,
    val error: String? = null,
    val releasesError: String? = null,
    val readmeError: String? = null
)

@HiltViewModel
class RepositoryHubViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val githubRepository: GitHubRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repoName: String = checkNotNull(savedStateHandle["repo"])
    private val demo: Boolean = savedStateHandle["demo"] ?: false

    private val _state = MutableStateFlow(RepositoryHubState())
    val state: StateFlow<RepositoryHubState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentRepositoryId: Long? = null

    fun start(initialRepository: GitHubRepositoryModel?) {
        if (initialRepository?.id == currentRepositoryId && currentRepositoryId != null) return
        currentRepositoryId = initialRepository?.id
        _state.update {
            it.copy(
                repository = initialRepository ?: it.repository,
                loading = initialRepository == null && it.repository == null
            )
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(initialRepository) }
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(_state.value.repository) }
    }

    private suspend fun load(initialRepository: GitHubRepositoryModel?) {
        if (demo) {
            val repository = initialRepository
                ?: DemoData.repositories.firstOrNull {
                    it.owner.login.equals(owner, ignoreCase = true) &&
                        it.name.equals(repoName, ignoreCase = true)
                }

            if (repository == null) {
                _state.value = RepositoryHubState(
                    loading = false,
                    releasesLoading = false,
                    readmeLoading = false,
                    error = "This repository is unavailable in demo mode."
                )
                return
            }

            currentRepositoryId = repository.id
            _state.value = RepositoryHubState(
                repository = repository,
                releases = DEMO_RELEASES,
                readme = DEMO_README,
                loading = false,
                releasesLoading = false,
                readmeLoading = false
            )
            return
        }

        _state.update {
            it.copy(
                loading = initialRepository == null && it.repository == null,
                releasesLoading = true,
                readmeLoading = true,
                error = null,
                releasesError = null,
                readmeError = null
            )
        }

        val resolvedRepository = initialRepository ?: runCatchingPreservingCancellation {
            githubRepository.publicRepositories("$repoName user:$owner")
                .firstOrNull {
                    it.owner.login.equals(owner, ignoreCase = true) &&
                        it.name.equals(repoName, ignoreCase = true)
                }
        }.getOrNull()

        if (resolvedRepository == null) {
            _state.update {
                it.copy(
                    loading = false,
                    releasesLoading = false,
                    readmeLoading = false,
                    error = "Unable to load this repository. Open it again from the repository list."
                )
            }
            return
        }

        currentRepositoryId = resolvedRepository.id
        _state.update { it.copy(repository = resolvedRepository, loading = false) }

        coroutineScope {
            val releasesDeferred = async {
                runCatchingPreservingCancellation {
                    githubRepository.releases(owner, repoName)
                }
            }
            val readmeDeferred = async {
                runCatchingPreservingCancellation {
                    val rootEntries = githubRepository.contents(
                        owner = owner,
                        repo = repoName,
                        path = "",
                        ref = resolvedRepository.defaultBranch
                    )
                    val readmePath = rootEntries.firstOrNull { entry ->
                        README_CANDIDATES.any { candidate ->
                            entry.name.equals(candidate, ignoreCase = true)
                        }
                    }?.path
                    readmePath?.let { path ->
                        githubRepository.file(
                            owner = owner,
                            repo = repoName,
                            path = path,
                            ref = resolvedRepository.defaultBranch
                        ).let(SourceFileDecoder::decode)
                    }
                }.getOrNull()?.takeIf(String::isNotBlank)
            }

            val releasesResult = releasesDeferred.await()
            val readme = readmeDeferred.await()

            _state.update { current ->
                current.copy(
                    releases = releasesResult.getOrDefault(emptyList()),
                    releasesLoading = false,
                    releasesError = releasesResult.exceptionOrNull()?.let {
                        "Release information is temporarily unavailable."
                    },
                    readme = readme,
                    readmeLoading = false,
                    readmeError = if (readme == null) {
                        "No readable README file was found on ${resolvedRepository.defaultBranch}."
                    } else {
                        null
                    }
                )
            }
        }
    }

    private suspend fun <T> runCatchingPreservingCancellation(
        block: suspend () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

    private companion object {
        val README_CANDIDATES = listOf("README.md", "README.MD", "readme.md", "README")

        val DEMO_RELEASES = listOf(
            Release(
                id = 1,
                tagName = "v1.4.0",
                name = "GitHub Rock 1.4",
                body = "## What changed\n\n- Unified repository page\n- Verified APK download flow\n- Improved release and README presentation",
                publishedAt = "2026-07-14T00:00:00Z",
                assets = listOf(
                    ReleaseAsset(
                        id = 11,
                        name = "github-rock-arm64-v8a.apk",
                        size = 25_900_000,
                        downloadUrl = "https://example.com/github-rock-arm64-v8a.apk"
                    ),
                    ReleaseAsset(
                        id = 12,
                        name = "github-rock-universal.apk",
                        size = 31_400_000,
                        downloadUrl = "https://example.com/github-rock-universal.apk"
                    )
                )
            ),
            Release(
                id = 2,
                tagName = "v1.5.0-beta01",
                name = "GitHub Rock beta",
                body = "Preview build for the next repository experience.",
                prerelease = true,
                publishedAt = "2026-07-13T00:00:00Z"
            )
        )

        const val DEMO_README = """
# GitHub Rock Demo

A premium Android developer control centre for repository management, Actions, releases, verified downloads, and APK inspection.

## Highlights

- Kotlin and Jetpack Compose
- GitHub Device Flow authentication
- Repository, issue, pull request, workflow, and release tools
- Liquid Glass dark and light themes

> Demo mode uses isolated sample data and never writes to GitHub.
"""
    }
}
