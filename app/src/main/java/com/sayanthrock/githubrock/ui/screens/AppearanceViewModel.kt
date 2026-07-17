package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {
    val state: StateFlow<AppearancePreferences> = preferences.appearance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearancePreferences()
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setThemeStyle(style: ThemeStyle) = viewModelScope.launch { preferences.setThemeStyle(style) }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch { preferences.setAccentColor(color) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColor(enabled) }
    fun setTrueBlack(enabled: Boolean) = viewModelScope.launch { preferences.setTrueBlack(enabled) }
    fun setShowImages(enabled: Boolean) = viewModelScope.launch { preferences.setShowImages(enabled) }
    fun setWorkflowPreview(enabled: Boolean) = viewModelScope.launch { preferences.setWorkflowPreview(enabled) }
    fun setWorkflowStepDetails(enabled: Boolean) = viewModelScope.launch { preferences.setWorkflowStepDetails(enabled) }
    fun setStatusColors(enabled: Boolean) = viewModelScope.launch { preferences.setStatusColors(enabled) }
    fun setActionsControls(enabled: Boolean) = viewModelScope.launch { preferences.setActionsControls(enabled) }
    fun setRepositoryManager(enabled: Boolean) = viewModelScope.launch { preferences.setRepositoryManager(enabled) }
    fun setFileTools(enabled: Boolean) = viewModelScope.launch { preferences.setFileTools(enabled) }
    fun setCompactCards(enabled: Boolean) = viewModelScope.launch { preferences.setCompactCards(enabled) }
    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { preferences.setReduceMotion(enabled) }
}
