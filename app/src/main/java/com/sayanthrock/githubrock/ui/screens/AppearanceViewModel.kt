package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
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
    fun setDisplaySize(size: DisplaySize) = viewModelScope.launch { preferences.setDisplaySize(size) }
    fun setFontSize(size: FontSize) = viewModelScope.launch { preferences.setFontSize(size) }
    fun setFontWeight(weight: FontWeightStyle) = viewModelScope.launch { preferences.setFontWeight(weight) }
    fun setFontFamily(family: AppFontFamily) = viewModelScope.launch { preferences.setFontFamily(family) }
    fun setLoadingStyle(style: LoadingStyle) = viewModelScope.launch { preferences.setLoadingStyle(style) }
    fun setCodeColorStyle(style: CodeColorStyle) = viewModelScope.launch { preferences.setCodeColorStyle(style) }
    fun setLogDisplayStyle(style: LogDisplayStyle) = viewModelScope.launch { preferences.setLogDisplayStyle(style) }
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
    fun setAllFeatureControls(enabled: Boolean) = viewModelScope.launch { preferences.setAllFeatureControls(enabled) }
    fun resetAppearance() = viewModelScope.launch { preferences.resetAppearance() }
}
