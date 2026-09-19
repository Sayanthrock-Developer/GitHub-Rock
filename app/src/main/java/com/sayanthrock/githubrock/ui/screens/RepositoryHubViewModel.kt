package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.translation.GoogleTranslationService
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.RepositoryReadmePolicy
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val readmeError: String? = null,
    val translationTarget: String? = null,
    val translatedBlocks: Map<Int, String> = emptyMap(),
    val translationLoading: Boolean = false,
    val translationError: String? = null
)

@HiltViewModel
class RepositoryHubViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val githubRepository: GitHubRepository,
    private val translationService: GoogleTranslationService
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repoName: String = checkNotNull(savedStateHandle["repo"])

    private val _state = MutableStateFlow(RepositoryHubState())
    val state: StateFlow<RepositoryHubState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentRepositoryId: Long? = null
    private var translationJob: Job? = null

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

    fun translateReadme(blocks: List<MarkdownBlock>, targetLanguage: String) {
        if (targetLanguage.isBlank()) return
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    translationTarget = targetLanguage,
                    translationLoading = true,
                    translationError = null,
                    translatedBlocks = emptyMap()
                )
            }
            runCatchingPreservingCancellation {
                val translatable = blocks.mapIndexedNotNull { index, block ->
                    if (block.kind.isTranslatable()) index to block.text else null
                }.filter { it.second.isNotBlank() }
                buildMap {
                    translatable.forEach { (index, text) ->
                        put(index, translationService.translate(text, targetLanguage))
                    }
                }
            }.onSuccess { translated ->
                _state.update { it.copy(translationLoading = false, translatedBlocks = translated, translationError = null) }
            }.onFailure { failure ->
                _state.update {
                    it.copy(
                        translationLoading = false,
                        translatedBlocks = emptyMap(),
                        translationError = failure.message ?: "Google translation is unavailable right now."
                    )
                }
            }
        }
    }

    fun clearTranslation() {
        translationJob?.cancel()
        _state.update { it.copy(translationTarget = null, translatedBlocks = emptyMap(), translationLoading = false, translationError = null) }
    }

    private suspend fun load(initialRepository: GitHubRepositoryModel?) {

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

        val repositoryResult = if (initialRepository != null) {
            Result.success(initialRepository)
        } else {
            runCatchingPreservingCancellation {
                githubRepository.repository(owner, repoName)
            }
        }
        val resolvedRepository = repositoryResult.getOrNull()

        if (resolvedRepository == null) {
            _state.update {
                it.copy(
                    loading = false,
                    releasesLoading = false,
                    readmeLoading = false,
                    error = when (val failure = repositoryResult.exceptionOrNull()) {
                        is retrofit2.HttpException -> if (failure.code() == 404) {
                            "This repository does not exist or your GitHub account cannot access it."
                        } else {
                            "Repository information is temporarily unavailable. Retry when the connection is stable."
                        }
                        else -> "Repository information is temporarily unavailable. Retry when the connection is stable."
                    }
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
                }
            }

            val releasesResult = releasesDeferred.await()
            val readmeResult = readmeDeferred.await()
            val readme = readmeResult.getOrNull()?.takeIf(String::isNotBlank)

            _state.update { current ->
                current.copy(
                    releases = releasesResult.getOrDefault(emptyList()),
                    releasesLoading = false,
                    releasesError = releasesResult.exceptionOrNull()?.let {
                        "Release information is temporarily unavailable."
                    },
                    readme = readme,
                    readmeLoading = false,
                    readmeError = RepositoryReadmePolicy.errorMessage(
                        readme = readme,
                        failure = readmeResult.exceptionOrNull(),
                        branch = resolvedRepository.defaultBranch
                    )
                )
            }
        }
    }

    private companion object {
        fun MarkdownBlockKind.isTranslatable(): Boolean = when (this) {
            MarkdownBlockKind.Heading,
            MarkdownBlockKind.Paragraph,
            MarkdownBlockKind.Bullet,
            MarkdownBlockKind.Task,
            MarkdownBlockKind.Quote,
            MarkdownBlockKind.Alert -> true
            else -> false
        }

        val README_CANDIDATES = listOf("README.md", "README.MD", "readme.md", "README")
    }
}
