package com.sayanthrock.githubrock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "github_rock_preferences")

enum class ThemeMode { System, Light, Dark; companion object { fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: System } }
enum class ThemeStyle { Clean, LiquidGlass, Studio, Midnight, Aurora, HighContrast, Obsidian; companion object { fun fromStored(value: String?): ThemeStyle = entries.firstOrNull { it.name == value } ?: Clean } }
enum class AccentColor {
    DefaultGitHubRock, Red, Orange, Yellow, Green, Teal, Cyan, Blue, Indigo, Purple, Pink,
    // Kept for migration compatibility with existing installations.
    Violet, Emerald, Rose, Coral, Amber;
    companion object {
        fun fromStored(value: String?): AccentColor = entries.firstOrNull { it.name == value } ?: DefaultGitHubRock
    }
}
enum class DisplaySize { Small, Standard, Large; companion object { fun fromStored(value: String?): DisplaySize = entries.firstOrNull { it.name == value } ?: Standard } }
enum class FontSize { Small, Default, Large; companion object { fun fromStored(value: String?): FontSize = entries.firstOrNull { it.name == value } ?: Default } }
enum class FontWeightStyle { Light, Default, Bold; companion object { fun fromStored(value: String?): FontWeightStyle = entries.firstOrNull { it.name == value } ?: Default } }
enum class AppFontFamily { SystemSans, Serif, Monospace; companion object { fun fromStored(value: String?): AppFontFamily = entries.firstOrNull { it.name == value } ?: SystemSans } }
enum class LoadingStyle { Spinner, Linear, Pulse, Skeleton, Liquid, Orbit, Shimmer, Morph; companion object { fun fromStored(value: String?): LoadingStyle = entries.firstOrNull { it.name == value } ?: Spinner } }
enum class AnimationStyle { Liquid, Spring, Cinematic, Magnetic, Dynamic; companion object { fun fromStored(value: String?): AnimationStyle = entries.firstOrNull { it.name == value } ?: Spring } }
enum class CodeColorStyle { Classic, Ocean, Sunset, Monochrome, GitHub; companion object { fun fromStored(value: String?): CodeColorStyle = entries.firstOrNull { it.name == value } ?: Classic } }
enum class LogDisplayStyle { Dialog, Terminal; companion object { fun fromStored(value: String?): LogDisplayStyle = entries.firstOrNull { it.name == value } ?: Terminal } }
enum class NavigationBarStyle { FloatingCapsule, Classic, Minimal, Glass, Compact; companion object { fun fromStored(value: String?): NavigationBarStyle = entries.firstOrNull { it.name == value } ?: FloatingCapsule } }

data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val themeStyle: ThemeStyle = ThemeStyle.Clean,
    val accentColor: AccentColor = AccentColor.DefaultGitHubRock,
    val customAccentHex: String = "",
    val recentAccentColors: List<String> = emptyList(),
    val displaySize: DisplaySize = DisplaySize.Standard,
    val fontSize: FontSize = FontSize.Default,
    val fontWeight: FontWeightStyle = FontWeightStyle.Default,
    val fontFamily: AppFontFamily = AppFontFamily.SystemSans,
    val loadingStyle: LoadingStyle = LoadingStyle.Spinner,
    val animationStyle: AnimationStyle = AnimationStyle.Spring,
    val codeColorStyle: CodeColorStyle = CodeColorStyle.Classic,
    val logDisplayStyle: LogDisplayStyle = LogDisplayStyle.Terminal,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.FloatingCapsule,
    val dynamicColor: Boolean = true,
    val trueBlack: Boolean = false,
    val showImages: Boolean = true,
    val workflowPreview: Boolean = true,
    val workflowStepDetails: Boolean = true,
    val statusColors: Boolean = true,
    val actionsControls: Boolean = true,
    val repositoryManager: Boolean = true,
    val fileTools: Boolean = true,
    val compactCards: Boolean = false,
    val reduceMotion: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    val appearance: Flow<AppearancePreferences> = context.dataStore.data.map { preferences ->
        AppearancePreferences(
            themeMode = ThemeMode.fromStored(preferences[THEME_MODE]),
            themeStyle = ThemeStyle.fromStored(preferences[THEME_STYLE]),
            accentColor = AccentColor.fromStored(preferences[ACCENT_COLOR]),
            customAccentHex = preferences[CUSTOM_ACCENT_HEX].orEmpty(),
            recentAccentColors = preferences[RECENT_ACCENT_COLORS].orEmpty().toList().take(MAX_RECENT_ACCENTS),
            displaySize = DisplaySize.fromStored(preferences[DISPLAY_SIZE]),
            fontSize = FontSize.fromStored(preferences[FONT_SIZE]),
            fontWeight = FontWeightStyle.fromStored(preferences[FONT_WEIGHT]),
            fontFamily = AppFontFamily.fromStored(preferences[FONT_FAMILY]),
            loadingStyle = LoadingStyle.fromStored(preferences[LOADING_STYLE]),
            animationStyle = AnimationStyle.fromStored(preferences[ANIMATION_STYLE]),
            codeColorStyle = CodeColorStyle.fromStored(preferences[CODE_COLOR_STYLE]),
            logDisplayStyle = LogDisplayStyle.fromStored(preferences[LOG_DISPLAY_STYLE]),
            navigationBarStyle = NavigationBarStyle.fromStored(preferences[NAVIGATION_BAR_STYLE]),
            dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
            trueBlack = preferences[TRUE_BLACK] ?: false,
            showImages = preferences[SHOW_IMAGES] ?: true,
            workflowPreview = preferences[WORKFLOW_PREVIEW] ?: true,
            workflowStepDetails = preferences[WORKFLOW_STEP_DETAILS] ?: true,
            statusColors = preferences[STATUS_COLORS] ?: true,
            actionsControls = preferences[ACTIONS_CONTROLS] ?: true,
            repositoryManager = preferences[REPOSITORY_MANAGER] ?: true,
            fileTools = preferences[FILE_TOOLS] ?: true,
            compactCards = preferences[COMPACT_CARDS] ?: false,
            reduceMotion = preferences[REDUCE_MOTION] ?: false
        )
    }
    val dynamicColor: Flow<Boolean> = appearance.map { it.dynamicColor }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }
    val favoriteRepositories: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITE_REPOSITORIES].orEmpty() }
    val repositorySearchHistory: Flow<List<String>> = context.dataStore.data.map { preferences -> preferences[REPOSITORY_SEARCH_HISTORY]?.split(HISTORY_SEPARATOR)?.map(String::trim)?.filter(String::isNotBlank).orEmpty() }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setThemeStyle(style: ThemeStyle) = context.dataStore.edit { it[THEME_STYLE] = style.name }
    suspend fun setAccentColor(color: AccentColor) = context.dataStore.edit { it[ACCENT_COLOR] = color.name; if (color != AccentColor.DefaultGitHubRock || color != AccentColor.DefaultGitHubRock) it[DYNAMIC_COLOR] = false }
    suspend fun setCustomAccentHex(hex: String) = context.dataStore.edit { preferences ->
        val normalized = normalizeHex(hex) ?: return@edit
        preferences[CUSTOM_ACCENT_HEX] = normalized
        preferences[ACCENT_COLOR] = AccentColor.DefaultGitHubRock.name
        preferences[DYNAMIC_COLOR] = false
        val current = preferences[RECENT_ACCENT_COLORS].orEmpty().toMutableList()
        current.removeAll { it.equals(normalized, ignoreCase = true) }
        current.add(0, normalized)
        preferences[RECENT_ACCENT_COLORS] = current.take(MAX_RECENT_ACCENTS).toSet()
    }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun clearCustomAccent() = context.dataStore.edit { it[CUSTOM_ACCENT_HEX] = "" }
    suspend fun setDisplaySize(size: DisplaySize) = context.dataStore.edit { it[DISPLAY_SIZE] = size.name }
    suspend fun setFontSize(size: FontSize) = context.dataStore.edit { it[FONT_SIZE] = size.name }
    suspend fun setFontWeight(weight: FontWeightStyle) = context.dataStore.edit { it[FONT_WEIGHT] = weight.name }
    suspend fun setFontFamily(family: AppFontFamily) = context.dataStore.edit { it[FONT_FAMILY] = family.name }
    suspend fun setLoadingStyle(style: LoadingStyle) = context.dataStore.edit { it[LOADING_STYLE] = style.name }
    suspend fun setAnimationStyle(style: AnimationStyle) = context.dataStore.edit { it[ANIMATION_STYLE] = style.name }
    suspend fun setCodeColorStyle(style: CodeColorStyle) = context.dataStore.edit { it[CODE_COLOR_STYLE] = style.name }
    suspend fun setLogDisplayStyle(style: LogDisplayStyle) = context.dataStore.edit { it[LOG_DISPLAY_STYLE] = style.name }
    suspend fun setNavigationBarStyle(style: NavigationBarStyle) = context.dataStore.edit { it[NAVIGATION_BAR_STYLE] = style.name }
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

    suspend fun addRepositorySearch(query: String) {
        val normalized = query.trim().replace(HISTORY_SEPARATOR, " ").takeIf(String::isNotBlank) ?: return
        context.dataStore.edit { preferences ->
            val current = preferences[REPOSITORY_SEARCH_HISTORY]?.split(HISTORY_SEPARATOR)?.filter(String::isNotBlank).orEmpty()
            val updated = buildList { add(normalized); current.filterNot { it.equals(normalized, ignoreCase = true) }.forEach(::add) }.take(MAX_SEARCH_HISTORY)
            preferences[REPOSITORY_SEARCH_HISTORY] = updated.joinToString(HISTORY_SEPARATOR)
        }
    }
    suspend fun clearRepositorySearchHistory() = context.dataStore.edit { it.remove(REPOSITORY_SEARCH_HISTORY) }
    suspend fun resetAppearance() = context.dataStore.edit { preferences ->
        listOf(THEME_MODE, THEME_STYLE, ACCENT_COLOR, CUSTOM_ACCENT_HEX, RECENT_ACCENT_COLORS, DISPLAY_SIZE, FONT_SIZE, FONT_WEIGHT, FONT_FAMILY, LOADING_STYLE, ANIMATION_STYLE, CODE_COLOR_STYLE, LOG_DISPLAY_STYLE, NAVIGATION_BAR_STYLE, DYNAMIC_COLOR, TRUE_BLACK, SHOW_IMAGES, WORKFLOW_PREVIEW, WORKFLOW_STEP_DETAILS, STATUS_COLORS, ACTIONS_CONTROLS, REPOSITORY_MANAGER, FILE_TOOLS, COMPACT_CARDS, REDUCE_MOTION).forEach(preferences::remove)
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
    suspend fun monitoredWorkflowRun(monitorKey: String): Long? = context.dataStore.data.first()[longPreferencesKey("workflow_monitor_$monitorKey")]
    suspend fun setMonitoredWorkflowRun(monitorKey: String, runId: Long) { context.dataStore.edit { it[longPreferencesKey("workflow_monitor_$monitorKey")] = runId } }
    suspend fun clearMonitoredWorkflowRun(monitorKey: String) { context.dataStore.edit { it.remove(longPreferencesKey("workflow_monitor_$monitorKey")) } }

    private companion object {
        const val HISTORY_SEPARATOR = "\u001F"
        const val MAX_SEARCH_HISTORY = 8
        const val MAX_RECENT_ACCENTS = 8
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val RECENT_ACCENT_COLORS = stringSetPreferencesKey("recent_accent_colors")
        val DISPLAY_SIZE = stringPreferencesKey("display_size")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LOADING_STYLE = stringPreferencesKey("loading_style")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val CODE_COLOR_STYLE = stringPreferencesKey("code_color_style")
        val LOG_DISPLAY_STYLE = stringPreferencesKey("log_display_style")
        val NAVIGATION_BAR_STYLE = stringPreferencesKey("navigation_bar_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val TRUE_BLACK = booleanPreferencesKey("true_black")
        val SHOW_IMAGES = booleanPreferencesKey("show_images")
        val WORKFLOW_PREVIEW = booleanPreferencesKey("workflow_preview")
        val WORKFLOW_STEP_DETAILS = booleanPreferencesKey("workflow_step_details")
        val STATUS_COLORS = booleanPreferencesKey("status_colors")
        val ACTIONS_CONTROLS = booleanPreferencesKey("actions_controls")
        val REPOSITORY_MANAGER = booleanPreferencesKey("repository_manager")
        val FILE_TOOLS = booleanPreferencesKey("file_tools")
        val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val FAVORITE_REPOSITORIES = stringSetPreferencesKey("favorite_repositories")
        val REPOSITORY_SEARCH_HISTORY = stringPreferencesKey("repository_search_history")
    }

    private fun normalizeHex(value: String): String? {
        val clean = value.trim().removePrefix("#")
        if (clean.length != 6 && clean.length != 8) return null
        if (!clean.all { it in "0123456789abcdefABCDEF" }) return null
        return "#${clean.uppercase()}"
    }
}
