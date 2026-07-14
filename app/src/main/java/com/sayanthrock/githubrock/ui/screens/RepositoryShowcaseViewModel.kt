package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the visual repository landing page. */
data class RepositoryShowcaseState(
    val repository: GitHubRepositoryModel? = null,
    val readme: String? = null,
    val loading: Boolean = true,
    val readmeLoading: Boolean = true,
    val error: String? = null,
    val readmeError: String? = null
)

@HiltViewModel
class RepositoryShowcaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val githubRepository: GitHubRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repoName: String = checkNotNull(savedStateHandle["repo"])
    private val demo: Boolean = savedStateHandle["demo"] ?: false

    private val _state = MutableStateFlow(RepositoryShowcaseState())
    val state: StateFlow<RepositoryShowcaseState> = _state.asStateFlow()

    private var started = false

    /** Supplies the repository already selected from Home or Repositories for instant first paint. */
    fun start(initialRepository: GitHubRepositoryModel?) {
        if (started) return
        started = true
        _state.update { it.copy(repository = initialRepository, loading = initialRepository == null) }
        viewModelScope.launch { load(initialRepository) }
    }

    fun retry() {
        viewModelScope.launch { load(_state.value.repository) }
    }

    private suspend fun load(initialRepository: GitHubRepositoryModel?) {
        if (demo) {
            val repository = initialRepository
                ?: DemoData.repositories.firstOrNull { it.owner.login == owner && it.name == repoName }
                ?: DemoData.repositories.firstOrNull()
            _state.value = RepositoryShowcaseState(
                repository = repository,
                readme = DEMO_README,
                loading = false,
                readmeLoading = false
            )
            return
        }

        _state.update {
            it.copy(
                loading = initialRepository == null && it.repository == null,
                readmeLoading = true,
                error = null,
                readmeError = null
            )
        }

        val resolvedRepository = initialRepository ?: runCatching {
            githubRepository.publicRepositories("$repoName user:$owner")
                .firstOrNull { it.owner.login.equals(owner, ignoreCase = true) && it.name.equals(repoName, ignoreCase = true) }
        }.getOrNull()

        if (resolvedRepository == null) {
            _state.update {
                it.copy(
                    loading = false,
                    readmeLoading = false,
                    error = "Unable to load this repository. Open it again from the repository list."
                )
            }
            return
        }

        _state.update { it.copy(repository = resolvedRepository, loading = false) }

        val readme = README_CANDIDATES.firstNotNullOfOrNull { path ->
            runCatching {
                githubRepository.file(
                    owner = owner,
                    repo = repoName,
                    path = path,
                    ref = resolvedRepository.defaultBranch
                ).let(SourceFileDecoder::decode)
            }.getOrNull()?.takeIf(String::isNotBlank)
        }

        _state.update {
            if (readme == null) {
                it.copy(
                    readmeLoading = false,
                    readmeError = "No readable README file was found on ${resolvedRepository.defaultBranch}."
                )
            } else {
                it.copy(readme = readme, readmeLoading = false, readmeError = null)
            }
        }
    }

    private companion object {
        val README_CANDIDATES = listOf("README.md", "README.MD", "readme.md", "README")
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
