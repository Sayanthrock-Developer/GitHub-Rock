package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AnimationStyle
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ApplicationBlurPreferences
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle
import com.sayanthrock.githubrock.data.settings.RemoteImageAnimation
import com.sayanthrock.githubrock.data.settings.RemoteImageNetworkPolicy
import com.sayanthrock.githubrock.data.settings.RemoteImageOverride
import com.sayanthrock.githubrock.data.settings.RemoteImagePlaceholder
import com.sayanthrock.githubrock.data.settings.RemoteImageQuality
import com.sayanthrock.githubrock.data.settings.RemoteImageShape
import com.sayanthrock.githubrock.data.settings.RemoteImageSize
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurComponent
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.toSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val blurPreferences: ApplicationBlurPreferences
) : ViewModel() {
    val state: StateFlow<AppearancePreferences> = preferences.appearance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearancePreferences())
    val blurState: StateFlow<ApplicationBlurSettings> = blurPreferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationBlurPreset.Clean.toSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setThemeStyle(style: ThemeStyle) = viewModelScope.launch { preferences.setThemeStyle(style) }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch { preferences.setAccentColor(color) }
    fun setSystemDynamicAccent() = viewModelScope.launch { preferences.setSystemDynamicAccent() }
    fun setCustomAccentHex(hex: String) = viewModelScope.launch { preferences.setCustomAccentHex(hex) }
    fun setDisplaySize(size: DisplaySize) = viewModelScope.launch { preferences.setDisplaySize(size) }
    fun setFontSize(size: FontSize) = viewModelScope.launch { preferences.setFontSize(size) }
    fun setFontWeight(weight: FontWeightStyle) = viewModelScope.launch { preferences.setFontWeight(weight) }
    fun setFontFamily(family: AppFontFamily) = viewModelScope.launch { preferences.setFontFamily(family) }
    fun setLoadingStyle(style: LoadingStyle) = viewModelScope.launch { preferences.setLoadingStyle(style) }
    fun setAnimationStyle(style: AnimationStyle) = viewModelScope.launch { preferences.setAnimationStyle(style) }
    fun setCodeColorStyle(style: CodeColorStyle) = viewModelScope.launch { preferences.setCodeColorStyle(style) }
    fun setLogDisplayStyle(style: LogDisplayStyle) = viewModelScope.launch { preferences.setLogDisplayStyle(style) }
    fun setNavigationBarStyle(style: NavigationBarStyle) = viewModelScope.launch { preferences.setNavigationBarStyle(style) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColor(enabled) }
    fun setTrueBlack(enabled: Boolean) = viewModelScope.launch { preferences.setTrueBlack(enabled) }
    fun setShowImages(enabled: Boolean) = viewModelScope.launch { preferences.setShowImages(enabled) }
    fun setRemoteImagesEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setRemoteImagesEnabled(enabled) }
    fun setRemoteImageAvatarOverride(value: RemoteImageOverride) = viewModelScope.launch { preferences.setRemoteImageAvatarOverride(value) }
    fun setRemoteImageRepositoryArtworkOverride(value: RemoteImageOverride) = viewModelScope.launch { preferences.setRemoteImageRepositoryArtworkOverride(value) }
    fun setRemoteImageProfileRepositoryOverride(value: RemoteImageOverride) = viewModelScope.launch { preferences.setRemoteImageProfileRepositoryOverride(value) }
    fun setRemoteImageNetworkPolicy(value: RemoteImageNetworkPolicy) = viewModelScope.launch { preferences.setRemoteImageNetworkPolicy(value) }
    fun setRemoteImageQuality(value: RemoteImageQuality) = viewModelScope.launch { preferences.setRemoteImageQuality(value) }
    fun setRemoteImageCache(enabled: Boolean) = viewModelScope.launch { preferences.setRemoteImageCache(enabled) }
    fun setRemoteImageShape(value: RemoteImageShape) = viewModelScope.launch { preferences.setRemoteImageShape(value) }
    fun setRemoteImageSize(value: RemoteImageSize) = viewModelScope.launch { preferences.setRemoteImageSize(value) }
    fun setRemoteImagePlaceholder(value: RemoteImagePlaceholder) = viewModelScope.launch { preferences.setRemoteImagePlaceholder(value) }
    fun setRemoteImageAnimation(value: RemoteImageAnimation) = viewModelScope.launch { preferences.setRemoteImageAnimation(value) }
    fun resetRemoteImageSettings() = viewModelScope.launch { preferences.resetRemoteImageSettings() }
    fun setWorkflowPreview(enabled: Boolean) = viewModelScope.launch { preferences.setWorkflowPreview(enabled) }
    fun setWorkflowStepDetails(enabled: Boolean) = viewModelScope.launch { preferences.setWorkflowStepDetails(enabled) }
    fun setStatusColors(enabled: Boolean) = viewModelScope.launch { preferences.setStatusColors(enabled) }
    fun setActionsControls(enabled: Boolean) = viewModelScope.launch { preferences.setActionsControls(enabled) }
    fun setRepositoryManager(enabled: Boolean) = viewModelScope.launch { preferences.setRepositoryManager(enabled) }
    fun setFileTools(enabled: Boolean) = viewModelScope.launch { preferences.setFileTools(enabled) }
    fun setCompactCards(enabled: Boolean) = viewModelScope.launch { preferences.setCompactCards(enabled) }
    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { preferences.setReduceMotion(enabled) }

    fun setBlurMode(mode: ApplicationBlurMode) = viewModelScope.launch { blurPreferences.setMode(mode) }
    fun setBlurPreset(preset: ApplicationBlurPreset) = viewModelScope.launch { blurPreferences.setPreset(preset) }
    fun setBlurProfile(profile: ApplicationBlurProfile) = viewModelScope.launch { blurPreferences.setProfile(profile) }
    fun setBlurCustomTint(hex: String) = viewModelScope.launch { blurPreferences.setCustomTint(hex) }
    fun setBlurComponentProfile(component: ApplicationBlurComponent, profile: ApplicationBlurProfile?) = viewModelScope.launch { blurPreferences.setComponentProfile(component, profile) }
    fun resetBlurSettings() = viewModelScope.launch { blurPreferences.reset() }

    fun resetAppearance() = viewModelScope.launch { preferences.resetAppearance() }
}
