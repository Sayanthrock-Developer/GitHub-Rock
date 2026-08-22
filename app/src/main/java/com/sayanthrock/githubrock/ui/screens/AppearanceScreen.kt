package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.NavigationStyle
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.components.AppLoadingIndicator
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceContent(
        state = state,
        onBack = onBack,
        onThemeMode = viewModel::setThemeMode,
        onThemeStyle = viewModel::setThemeStyle,
        onNavigationStyle = viewModel::setNavigationStyle,
        onAccentColor = viewModel::setAccentColor,
        onDisplaySize = viewModel::setDisplaySize,
        onFontSize = viewModel::setFontSize,
        onFontWeight = viewModel::setFontWeight,
        onFontFamily = viewModel::setFontFamily,
        onLoadingStyle = viewModel::setLoadingStyle,
        onCodeColorStyle = viewModel::setCodeColorStyle,
        onLogDisplayStyle = viewModel::setLogDisplayStyle,
        onDynamicColor = viewModel::setDynamicColor,
        onTrueBlack = viewModel::setTrueBlack,
        onShowImages = viewModel::setShowImages,
        onReset = viewModel::resetAppearance
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceContent(
    state: AppearancePreferences,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onThemeStyle: (ThemeStyle) -> Unit,
    onNavigationStyle: (NavigationStyle) -> Unit,
    onAccentColor: (AccentColor) -> Unit,
    onDisplaySize: (DisplaySize) -> Unit,
    onFontSize: (FontSize) -> Unit,
    onFontWeight: (FontWeightStyle) -> Unit,
    onFontFamily: (AppFontFamily) -> Unit,
    onLoadingStyle: (LoadingStyle) -> Unit,
    onCodeColorStyle: (CodeColorStyle) -> Unit,
    onLogDisplayStyle: (LogDisplayStyle) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit,
    onShowImages: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { StandardScreenHeader("Make GitHub Rock yours", "Theme, color, navigation, typography and motion all follow one visual system.") }
            item { StandardSectionHeader("Theme") }
            item {
                ChoiceCard("Theme mode", "System follows Android automatically; Light and Dark stay fixed.", Icons.Default.DarkMode,
                    ThemeMode.entries.map { it to label(it.name) }, state.themeMode, onThemeMode)
            }
            item {
                ChoiceCard("Design style", "Controls surfaces, shapes and the overall visual personality.", Icons.Default.Palette,
                    ThemeStyle.entries.map { it to label(it.name) }, state.themeStyle, onThemeStyle)
            }
            item {
                ChoiceCard("Navigation style", "Four clean styles for the five main destinations.", Icons.Default.SettingsSuggest,
                    NavigationStyle.entries.map { it to label(it.name) }, state.navigationStyle, onNavigationStyle)
            }
            item {
                ChoiceCard("Accent color", if (state.dynamicColor) "System dynamic color is active." else "Used consistently across navigation, controls and highlights.", Icons.Default.Palette,
                    AccentColor.entries.map { it to label(it.name) }, state.accentColor, onAccentColor, enabled = !state.dynamicColor)
            }
            item {
                StandardSettingsGroup {
                    StandardSettingsRow("Dynamic / system color", "Use Android's wallpaper-derived color palette.", state.dynamicColor, onDynamicColor)
                    StandardSettingsRow("True black", "Use OLED-friendly black surfaces in dark mode.", state.trueBlack, onTrueBlack)
                    StandardSettingsRow("Show remote images", "Allow profile and repository artwork to load.", state.showImages, onShowImages)
                }
            }
            item { StandardSectionHeader("Display & typography") }
            item { ChoiceCard("Display size", "Adjust interface density and spacing.", Icons.Default.FormatSize, DisplaySize.entries.map { it to label(it.name) }, state.displaySize, onDisplaySize) }
            item { ChoiceCard("Font family", "Choose the reading personality of the interface.", Icons.Default.FormatSize, AppFontFamily.entries.map { it to label(it.name) }, state.fontFamily, onFontFamily) }
            item { ChoiceCard("Font size", "Small, default or large text.", Icons.Default.FormatSize, FontSize.entries.map { it to label(it.name) }, state.fontSize, onFontSize) }
            item { ChoiceCard("Font weight", "Tune text emphasis across the app.", Icons.Default.FormatSize, FontWeightStyle.entries.map { it to label(it.name) }, state.fontWeight, onFontWeight) }
            item { StandardSectionHeader("Motion & developer surfaces") }
            item { ChoiceCard("Loading animation", "Used by repository loading and operations.", Icons.Default.SettingsSuggest, LoadingStyle.entries.map { it to label(it.name) }, state.loadingStyle, onLoadingStyle) }
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AppLoadingIndicator(style = state.loadingStyle, reduceMotion = false)
                        Column {
                            Text("Live preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("The selected animation is previewed here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { ChoiceCard("Code colors", "Keep repository code distinct from the app chrome.", Icons.Default.SettingsSuggest, CodeColorStyle.entries.map { it to label(it.name) }, state.codeColorStyle, onCodeColorStyle) }
            item { ChoiceCard("Log display", "Choose popup logs or an on-screen terminal.", Icons.Default.SettingsSuggest, LogDisplayStyle.entries.map { it to label(it.name) }, state.logDisplayStyle, onLogDisplayStyle) }
            item {
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Reset appearance")
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset appearance?") },
            text = { Text("All visual preferences, including navigation style, theme, colors and typography will return to their defaults.") },
            confirmButton = { Button(onClick = { confirmReset = false; onReset() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun <T> ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { (value, text) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelected(value) },
                        enabled = enabled,
                        label = { Text(text) },
                        leadingIcon = if (selected == value) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null
                    )
                }
            }
        }
    }
}

private fun label(value: String): String = value
    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
