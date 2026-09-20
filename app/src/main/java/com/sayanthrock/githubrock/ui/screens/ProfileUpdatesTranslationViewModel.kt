package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.translation.GoogleTranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUpdatesTranslationState(
    val targetLanguage: String? = null,
    val translatedTitle: String? = null,
    val translatedSubtitle: String? = null,
    val translatedDescription: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileUpdatesTranslationViewModel @Inject constructor(
    private val translationService: GoogleTranslationService
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUpdatesTranslationState())
    val state: StateFlow<ProfileUpdatesTranslationState> = _state.asStateFlow()

    fun selectLanguage(targetLanguage: String) {
        if (targetLanguage.isBlank()) return
        _state.update { it.copy(targetLanguage = targetLanguage, error = null) }
    }

    fun translate(
        title: String,
        subtitle: String,
        description: String,
        targetLanguage: String
    ) {
        if (targetLanguage.isBlank()) return
        _state.update {
            it.copy(
                targetLanguage = targetLanguage,
                loading = true,
                error = null
            )
        }
        viewModelScope.launch {
            runCatching {
                listOf(title, subtitle, description)
                    .map { text -> async { translationService.translate(text, targetLanguage) } }
                    .awaitAll()
            }.onSuccess { translated ->
                _state.update {
                    it.copy(
                        translatedTitle = translated[0],
                        translatedSubtitle = translated[1],
                        translatedDescription = translated[2],
                        loading = false,
                        error = null
                    )
                }
            }.onFailure { failure ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = failure.message ?: "Google translation is unavailable right now."
                    )
                }
            }
        }
    }

    fun clearTranslation() {
        _state.value = ProfileUpdatesTranslationState()
    }
}
