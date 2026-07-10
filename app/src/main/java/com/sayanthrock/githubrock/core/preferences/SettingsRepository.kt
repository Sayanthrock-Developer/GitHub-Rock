package com.sayanthrock.githubrock.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "github_rock_settings")

enum class ThemePreference { SYSTEM, DARK, LIGHT }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val theme: Flow<ThemePreference> = context.settingsDataStore.data.map { preferences ->
        runCatching {
            ThemePreference.valueOf(preferences[THEME] ?: ThemePreference.SYSTEM.name)
        }.getOrDefault(ThemePreference.SYSTEM)
    }

    val biometricLockEnabled: Flow<Boolean> = context.settingsDataStore.data.map {
        it[BIOMETRIC_LOCK] ?: false
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.settingsDataStore.edit { it[THEME] = theme.name }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.settingsDataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }
}
