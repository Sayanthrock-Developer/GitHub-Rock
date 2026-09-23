package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.data.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RepositoryVisibilityViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {
    val hiddenRepositories: StateFlow<Set<String>> = preferences.hiddenRepositories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet()
    )

    fun hide(fullName: String) {
        viewModelScope.launch { preferences.setRepositoryHidden(fullName, true) }
    }

    fun unhide(fullName: String) {
        viewModelScope.launch { preferences.setRepositoryHidden(fullName, false) }
    }
}
