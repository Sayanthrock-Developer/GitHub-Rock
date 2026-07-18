package com.sayanthrock.githubrock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: System
    }
}

enum class ThemeStyle {
    Clean,
    LiquidGlass,
    Studio,
    Midnight,
    Aurora,
    HighContrast;

    companion object {
        fun fromStored(value: String?): ThemeStyle = entries.firstOrNull { it.name == value } ?: Clean
    }
}

enum class AccentColor {
    Cyan,
    Blue,
    Violet,
    Emerald,
    Rose,
    Coral,
    Amber,
    Orange;

    companion object {
        fun fromStored(value: String?): AccentColor = entries.firstOrNull { it.name == value } ?: Cyan
    }
}

enum class DisplaySize {
    Small,
    Standard,
    Large;

    companion object {
        fun fromStored(value: String?): DisplaySize = entries.firstOrNull { it.name == value } ?: Standard
    }
}

enum class FontSize {
    Small,
    Default,
    Large;

    companion object {
        fun fromStored(value: String?): FontSize = entries.firstOrNull { it.name == value } ?: Default
    }
}

enum class FontWeightStyle {
    Light,
    Default,
    Bold;

    companion object {
        fun fromStored(value: String?): FontWeightStyle = entries.firstOrNull { it.name == value } ?: Default
    }
}

enum class AppFontFamily {
    SystemSans,
    Serif,
    Monospace;

    companion object {
        fun fromStored(value: String?): AppFontFamily = entries.firstOrNull { it.name == value } ?: SystemSans
    }
}

enum class LoadingStyle {
    Spinner,
    Linear,
    Pulse;

    companion object {
        fun fromStored(value: String?): LoadingStyle = entries.firstOrNull { it.name == value } ?: Spinner
    }
}

enum class CodeColorStyle {
    Classic,
    Ocean,
    Sunset,
    Monochrome;

    companion object {
        fun fromStored(value: String?): CodeColorStyle = entries.firstOrNull { it.name == value } ?: Classic
    }
}

enum class LogDisplayStyle {
    Dialog,
    Terminal;

    companion object {
        fun fromStored(value: String?): LogDisplayStyle = entries.firstOrNull { it.name == value } ?: Terminal
    }
}

data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val themeStyle: ThemeStyle = ThemeStyle.Clean,
    val accentColor: AccentColor = AccentColor.Cyan,
    val displaySize: DisplaySize = DisplaySize.Standard,
    val fontSize: FontSize = FontSize.Default,
    val fontWeight: FontWeightStyle = FontWeightStyle.Default,
    val fontFamily: AppFontFamily = AppFontFamily.SystemSans,
    val loadingStyle: LoadingStyle = LoadingStyle.Spinner,
    val codeColorStyle: CodeColorStyle = CodeColorStyle.Classic,
    val logDisplayStyle: LogDisplayStyle = LogDisplayStyle.Terminal,
    val dynamicColor: Boolean = false,
    val trueBlack: Boolean = false,
    val showImages: Boolean = true,
    val workflowPreview: Boolean = true,
    val workflowStepDetails: Boolean = true,
    val statusColors: Boolean = true,
    val actionsControls: Boolean = true,
    val repositoryManager: Boolean = true,
    val fileTools: Boolean = true,
    val compactCards: Boolean = true,
    val reduceMotion: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appearance: Flow<AppearancePreferences> = context.dataStore.data.map { preferences ->
        AppearancePreferences(
            themeMode = ThemeMode.fromStored(preferences[THEME_MODE]),
            themeStyle = ThemeStyle.fromStored(preferences[THEME_STYLE]),
            accentColor = AccentColor.fromStored(preferences[ACCENT_COLOR]),
            displaySize = DisplaySize.fromStored(preferences[DISPLAY_SIZE]),
            fontSize = FontSize.fromStored(preferences[FONT_SIZE]),
            fontWeight = FontWeightStyle.fromStored(preferences[FONT_WEIGHT]),
            fontFamily = AppFontFamily.fromStored(preferences[FONT_FAMILY]),
            loadingStyle = LoadingStyle.fromStored(preferences[LOADING_STYLE]),
            codeColorStyle = CodeColorStyle.fromStored(preferences[CODE_COLOR_STYLE]),
            logDisplayStyle = LogDisplayStyle.fromStored(preferences[LOG_DISPLAY_STYLE]),
            dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
            trueBlack = preferences[TRUE_BLACK] ?: false,
            showImages = preferences[SHOW_IMAGES] ?: true,
            workflowPreview = preferences[WORKFLOW_PREVIEW] ?: true,
            workflowStepDetails = preferences[WORKFLOW_STEP_DETAILS] ?: true,
            statusColors = preferences[STATUS_COLORS] ?: true,
            actionsControls = preferences[ACTIONS_CONTROLS] ?: true,
            repositoryManager = preferences[REPOSITORY_MANAGER] ?: true,
            fileTools = preferences[FILE_TOOLS] ?: true,
            compactCards = preferences[COMPACT_CARDS] ?: true,
            reduceMotion = preferences[REDUCE_MOTION] ?: false
        )
    }
    val dynamicColor: Flow<Boolean> = appearance.map { it.dynamicColor }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }
    val favoriteRepositories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_REPOSITORIES].orEmpty()
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setThemeStyle(style: ThemeStyle) = context.dataStore.edit { it[THEME_STYLE] = style.name }
    suspend fun setAccentColor(color: AccentColor) = context.dataStore.edit { it[ACCENT_COLOR] = color.name }
    suspend fun setDisplaySize(size: DisplaySize) = context.dataStore.edit { it[DISPLAY_SIZE] = size.name }
    suspend fun setFontSize(size: FontSize) = context.dataStore.edit { it[FONT_SIZE] = size.name }
    suspend fun setFontWeight(weight: FontWeightStyle) = context.dataStore.edit { it[FONT_WEIGHT] = weight.name }
    suspend fun setFontFamily(family: AppFontFamily) = context.dataStore.edit { it[FONT_FAMILY] = family.name }
    suspend fun setLoadingStyle(style: LoadingStyle) = context.dataStore.edit { it[LOADING_STYLE] = style.name }
    suspend fun setCodeColorStyle(style: CodeColorStyle) = context.dataStore.edit { it[CODE_COLOR_STYLE] = style.name }
    suspend fun setLogDisplayStyle(style: LogDisplayStyle) = context.dataStore.edit { it[LOG_DISPLAY_STYLE] = style.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setTrueBlack(enabled: Boolean) = context.dataStore.edit { it[TRUE_BLACK] = enabled }
    suspend fun setShowImages(enabled: Boolean) = context.dataStore.edit { it[SHOW_IMAGES] = enabled }
    suspend fun setWorkflowPreview(enabled: Boolean) = context.dataStore.edit { it[WORKFLOW_PREVIEW] = enabled }
    suspend fun setWorkflowStepDetails(enabled: Boolean) = context.dataStore.edit { it[WORKFLOW_STEP_DETAILS] = enabled }
    suspend fun setStatusColors(enabled: Boolean) = context.dataStore.edit { it[STATUS_COLORS] = enabled }
    suspend fun setActionsControls(enabled: Boolean) = context.dataStore.edit { it[ACTIONS_CONTROLS] = enabled }
    suspend fun setRepositoryManager(enabled: Boolean) = context.dataStore.edit { it[REPOSITORY_MANAGER] = enabled }
    suspend fun setFileTools(enabled: Boolean) = context.dataStore.edit { it[FILE_TOOLS] = enabled }
    suspend fun setCompactCards(enabled: Boolean) = context.dataStore.edit { it[COMPACT_CARDS] = enabled }
    suspend fun setReduceMotion(enabled: Boolean) = context.dataStore.edit { it[REDUCE_MOTION] = enabled }
    suspend fun setBiometricLock(enabled: Boolean) = context.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }

    suspend fun resetAppearance() = context.dataStore.edit { preferences ->
        preferences.remove(THEME_MODE)
        preferences.remove(THEME_STYLE)
        preferences.remove(ACCENT_COLOR)
        preferences.remove(DISPLAY_SIZE)
        preferences.remove(FONT_SIZE)
        preferences.remove(FONT_WEIGHT)
        preferences.remove(FONT_FAMILY)
        preferences.remove(LOADING_STYLE)
        preferences.remove(CODE_COLOR_STYLE)
        preferences.remove(LOG_DISPLAY_STYLE)
        preferences.remove(DYNAMIC_COLOR)
        preferences.remove(TRUE_BLACK)
        preferences.remove(SHOW_IMAGES)
        preferences.remove(WORKFLOW_PREVIEW)
        preferences.remove(WORKFLOW_STEP_DETAILS)
        preferences.remove(STATUS_COLORS)
        preferences.remove(COMPACT_CARDS)
        preferences.remove(REDUCE_MOTION)
    }

    suspend fun toggleFavoriteRepository(fullName: String) {
        val normalized = fullName.trim().takeIf { it.count { character -> character == '/' } == 1 } ?: return
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITE_REPOSITORIES].orEmpty().toMutableSet()
            val existing = current.firstOrNull { it.equals(normalized, ignoreCase = true) }
            if (existing == null) current += normalized else current -= existing
            preferences[FAVORITE_REPOSITORIES] = current
        }
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
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DISPLAY_SIZE = stringPreferencesKey("display_size")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LOADING_STYLE = stringPreferencesKey("loading_style")
        val CODE_COLOR_STYLE = stringPreferencesKey("code_color_style")
        val LOG_DISPLAY_STYLE = stringPreferencesKey("log_display_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val TRUE_BLACK = booleanPreferencesKey("true_black")
        val SHOW_IMAGES = booleanPreferencesKey("show_images")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val WORKFLOW_PREVIEW = booleanPreferencesKey("workflow_preview")
        val WORKFLOW_STEP_DETAILS = booleanPreferencesKey("workflow_step_details")
        val STATUS_COLORS = booleanPreferencesKey("status_colors")
        val ACTIONS_CONTROLS = booleanPreferencesKey("actions_controls")
        val REPOSITORY_MANAGER = booleanPreferencesKey("repository_manager")
        val FILE_TOOLS = booleanPreferencesKey("file_tools")
        val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val FAVORITE_REPOSITORIES = stringSetPreferencesKey("favorite_repositories")
    }
}
