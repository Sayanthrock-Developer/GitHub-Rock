package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.translation.TranslationRepository
import com.sayanthrock.githubrock.core.network.TranslationLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class RepositoryTranslationState(
    val source: String? = null,
    val translated: String? = null,
    val targetLanguage: String = "en",
    val languages: List<TranslationLanguage> = emptyList(),
    val loading: Boolean = true,
    val languagesLoading: Boolean = true,
    val translating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RepositoryTranslationViewModel @Inject constructor(
    private val githubRepository: GitHubRepository,
    private val translations: TranslationRepository,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(RepositoryTranslationState())
    val state: StateFlow<RepositoryTranslationState> = _state.asStateFlow()

    private var repositoryKey: Long? = null

    fun load(repository: GitHubRepositoryModel) {
        if (repositoryKey == repository.id && _state.value.source != null) return
        repositoryKey = repository.id
        viewModelScope.launch {
            val preferred = preferences.translationLanguage.first()
            _state.update { it.copy(targetLanguage = preferred, loading = true, languagesLoading = true, error = null) }
            try {
                val languageList = translations.languages()
                _state.update { it.copy(languages = languageList, languagesLoading = false) }
            } catch (error: Throwable) {
                _state.update { it.copy(languagesLoading = false, error = "Translation service is unavailable. Check the connection and try again.") }
            }
            try {
                val entries = githubRepository.contents(repository.owner.login, repository.name, "", repository.defaultBranch)
                val path = entries.firstOrNull { it.name.equals("README.md", true) || it.name.equals("README", true) }?.path
                val source = path?.let { SourceFileDecoder.decode(githubRepository.file(repository.owner.login, repository.name, it, repository.defaultBranch)) }
                _state.update { it.copy(source = source?.takeIf(String::isNotBlank), loading = false) }
            } catch (_: Throwable) {
                _state.update { it.copy(loading = false, error = "README could not be loaded from GitHub.") }
            }
        }
    }

    suspend fun selectLanguage(code: String) {
        preferences.setTranslationLanguage(code)
        _state.update { it.copy(targetLanguage = code, translated = null, error = null) }
    }

    fun translate() {
        val source = _state.value.source ?: return
        val target = _state.value.targetLanguage
        viewModelScope.launch {
            _state.update { it.copy(translating = true, error = null) }
            try {
                val translated = translations.translate(source, target)
                _state.update { it.copy(translated = translated, translating = false) }
            } catch (error: Throwable) {
                val message = when (error) {
                    is retrofit2.HttpException -> when (error.code()) {
                        429 -> "Translation service rate limit reached. Try again later."
                        400, 422 -> "This language or content is not supported by the translation service."
                        else -> "Translation failed (${error.code()})."
                    }
                    else -> "Translation failed. Check the connection and try again."
                }
                _state.update { it.copy(translating = false, error = message) }
            }
        }
    }
}
