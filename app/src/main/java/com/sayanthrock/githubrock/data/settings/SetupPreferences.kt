package com.sayanthrock.githubrock.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists first-run setup completion outside the Compose/UI layer. */
@Singleton
class SetupPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _setupComplete = MutableStateFlow(
        preferences.getBoolean(KEY_SETUP_COMPLETE, false)
    )

    val setupComplete: StateFlow<Boolean> = _setupComplete

    fun markSetupComplete() {
        preferences.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
        _setupComplete.value = true
    }

    private companion object {
        const val PREFERENCES_NAME = "github_rock_setup"
        const val KEY_SETUP_COMPLETE = "setup_complete"
    }
}
