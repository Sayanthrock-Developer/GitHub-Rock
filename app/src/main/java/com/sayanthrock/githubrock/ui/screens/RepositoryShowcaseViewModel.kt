package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepositoryShowcaseState(
    val repository: GitHubRepositoryModel? = null,
    val readme: String? = null,
    val loading: Boolean = true,
    val readmeLoading: Boolean = true,
    val error: String? = null,
    val readmeError: String? = null,
    val translationTarget: String? = null,
    val translatedBlocks: Map<Int, String> = emptyMap(),
    val translationLoading: Boolean = false,
    val translationError: String? = null
)

@HiltViewModel
class RepositoryShowcaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val githubRepository: GitHubRepository,
    private val translationService: GoogleTranslationService
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repoName: String = checkNotNull(savedStateHandle["repo"])

    private val _state = MutableStateFlow(RepositoryShowcaseState())
    val state: StateFlow<RepositoryShowcaseState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentRepositoryId: Long? = null
    private var translationJob: Job? = null

    /** Supplies the selected repository for instant first paint, then refreshes its metadata and README. */
    fun start(initialRepository: GitHubRepositoryModel?) {
        currentRepositoryId = initialRepository?.id
        _state.update {
            it.copy(
                repository = initialRepository ?: it.repository,
                loading = initialRepository == null && it.repository == null,
                readme = if (initialRepository != null && initialRepository.id != it.repository?.id) null else it.readme
            )
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(initialRepository) }
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(_state.value.repository) }
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
                if (translatable.isEmpty()) return@runCatchingPreservingCancellation emptyMap()
                val source = translationService.detectLanguage(translatable.first().second)
                coroutineScope {
                    translatable.map { (index, text) ->
                        async {
                            index to translationService.translate(
                                text = text,
                                targetLanguage = targetLanguage,
                                sourceLanguage = source
                            )
                        }
                    }.awaitAll().toMap()
                }
            }.onSuccess { translated ->
                _state.update {
                    it.copy(
                        translationLoading = false,
                        translatedBlocks = translated,
                        translationError = null
                    )
                }
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
        _state.update {
            it.copy(
                translationTarget = null,
                translatedBlocks = emptyMap(),
                translationLoading = false,
                translationError = null
            )
        }
    }

    private suspend fun load(initialRepository: GitHubRepositoryModel?) {
        _state.update {
            it.copy(
                loading = initialRepository == null && it.repository == null,
                readmeLoading = true,
                error = null,
                readmeError = null
            )
        }

        val repositoryResult = if (initialRepository != null) {
            Result.success(initialRepository)
        } else {
            runCatchingPreservingCancellation {
                githubRepository.publicRepositories("$repoName user:$owner")
                    .firstOrNull {
                        it.owner.login.equals(owner, ignoreCase = true) &&
                            it.name.equals(repoName, ignoreCase = true)
                    }
            }
        }
        val resolvedRepository = repositoryResult.getOrNull()

        if (resolvedRepository == null) {
            _state.update {
                it.copy(
                    loading = false,
                    readmeLoading = false,
                    error = if (repositoryResult.isFailure) {
                        "Repository information is temporarily unavailable. Retry when the connection is stable."
                    } else {
                        "Unable to find this repository. Open it again from the repository list."
                    }
                )
            }
            return
        }

        currentRepositoryId = resolvedRepository.id
        _state.update { it.copy(repository = resolvedRepository, loading = false) }

        var unexpectedFailure: Throwable? = null
        var readme: String? = null
        for (path in README_CANDIDATES) {
            val result = runCatchingPreservingCancellation {
                githubRepository.file(
                    owner = owner,
                    repo = repoName,
                    path = path,
                    ref = resolvedRepository.defaultBranch
                ).let(SourceFileDecoder::decode)
            }
            result.exceptionOrNull()?.let { failure ->
                if (!RepositoryReadmePolicy.isMissing(failure) && unexpectedFailure == null) {
                    unexpectedFailure = failure
                }
            }
            val candidate = result.getOrNull()?.takeIf(String::isNotBlank)
            if (candidate != null) {
                readme = candidate
                break
            }
        }

        _state.update {
            it.copy(
                readme = readme,
                readmeLoading = false,
                readmeError = RepositoryReadmePolicy.errorMessage(
                    readme = readme,
                    failure = unexpectedFailure,
                    branch = resolvedRepository.defaultBranch
                )
            )
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
            MarkdownBlockKind.Code,
            MarkdownBlockKind.Divider,
            MarkdownBlockKind.Image,
            MarkdownBlockKind.Table -> false
        }

        val README_CANDIDATES = listOf("README.md", "README.MD", "readme.md", "README")
    }
}
