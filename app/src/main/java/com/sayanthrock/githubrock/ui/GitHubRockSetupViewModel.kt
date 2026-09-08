package com.sayanthrock.githubrock.ui

import androidx.lifecycle.ViewModel
import com.sayanthrock.githubrock.data.settings.SetupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GitHubRockSetupViewModel @Inject constructor(
    private val setupPreferences: SetupPreferences
) : ViewModel() {
    val setupComplete: StateFlow<Boolean> = setupPreferences.setupComplete

    fun completeSetup() {
        setupPreferences.markSetupComplete()
    }
}
