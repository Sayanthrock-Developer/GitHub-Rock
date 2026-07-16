package com.sayanthrock.githubrock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "github_rock_preferences")

enum class ThemeMode {
    System,
    Light,
    Dark;

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: System
    }
}

enum class AccentColor {
    Cyan,
    Blue,
    Violet,
    Coral,
    Amber;

    companion object {
        fun fromStored(value: String?): AccentColor =
            entries.firstOrNull { it.name == value } ?: Cyan
    }
}

data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val accentColor: AccentColor = AccentColor.Cyan,
    val dynamicColor: Boolean = false,
    val trueBlack: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appearance: Flow<AppearancePreferences> = context.dataStore.data.map { preferences ->
        AppearancePreferences(
            themeMode = ThemeMode.fromStored(preferences[THEME_MODE]),
            accentColor = AccentColor.fromStored(preferences[ACCENT_COLOR]),
            dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
            trueBlack = preferences[TRUE_BLACK] ?: false
        )
    }
    val dynamicColor: Flow<Boolean> = appearance.map(AppearancePreferences::dynamicColor)
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        context.dataStore.edit { it[ACCENT_COLOR] = color.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setTrueBlack(enabled: Boolean) {
        context.dataStore.edit { it[TRUE_BLACK] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }

    suspend fun monitoredWorkflowRun(monitorKey: String): Long? =
        context.dataStore.data.first()[longPreferencesKey("workflow_monitor_$monitorKey")]

    suspend fun setMonitoredWorkflowRun(monitorKey: String, runId: Long) {
        context.dataStore.edit { it[longPreferencesKey("workflow_monitor_$monitorKey")] = runId }
    }

    suspend fun clearMonitoredWorkflowRun(monitorKey: String) {
        context.dataStore.edit { it.remove(longPreferencesKey("workflow_monitor_$monitorKey")) }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val TRUE_BLACK = booleanPreferencesKey("true_black")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }
}
