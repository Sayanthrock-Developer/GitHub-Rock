package com.sayanthrock.githubrock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "github_rock_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
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
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }
}
