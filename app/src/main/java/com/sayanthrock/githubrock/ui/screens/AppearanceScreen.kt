package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
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
        onAccentColor = viewModel::setAccentColor,
        onDynamicColor = viewModel::setDynamicColor,
        onTrueBlack = viewModel::setTrueBlack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceContent(
    state: AppearancePreferences,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAccentColor: (AccentColor) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Clean, comfortable, yours",
                    subtitle = "Choose a readable Material 3 style without changing how GitHub Rock works."
                )
            }
            item { StandardSectionHeader("Look & feel") }
            item { PersonalityPreview() }
            item {
                AccentPicker(
                    selected = state.accentColor,
                    enabled = !state.dynamicColor,
                    onSelected = onAccentColor
                )
            }
            item {
                ThemeSettings(
                    state = state,
                    onThemeMode = onThemeMode,
                    onDynamicColor = onDynamicColor,
                    onTrueBlack = onTrueBlack
                )
            }
            item { StandardSectionHeader("Language & accessibility") }
            item {
                StandardSettingsGroup {
                    StandardSettingsRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "Follow system"
                    )
                    StandardSettingsDivider()
                    StandardSettingsRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Text and display size",
                        subtitle = "Uses your Android accessibility settings"
                    )
                }
            }
            item {
                Text(
                    "Theme changes apply immediately. Touch targets, contrast, and system font scaling remain available in every style.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PersonalityPreview() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Personality", style = MaterialTheme.typography.titleMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Clean standard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Calm surfaces · clear hierarchy · Material 3",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AccentPicker(
    selected: AccentColor,
    enabled: Boolean,
    onSelected: (AccentColor) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Accent", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (enabled) "Choose one restrained highlight color" else "System dynamic color is currently active",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentColor.entries.forEach { accent ->
                    val isSelected = selected == accent
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = isSelected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onSelected(accent) }
                            )
                            .semantics { contentDescription = "Use ${accent.name} accent" },
                        shape = CircleShape,
                        color = accent.previewColor,
                        border = BorderStroke(
                            if (isSelected) 3.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF071012),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettings(
    state: AppearancePreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit
) {
    StandardSettingsGroup {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Mode", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = { Text(mode.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        StandardSettingsDivider()
        StandardSettingsRow(
            icon = Icons.Default.ColorLens,
            title = "System dynamic color",
            subtitle = "Use your Android wallpaper palette",
            trailing = {
                Switch(
                    checked = state.dynamicColor,
                    onCheckedChange = onDynamicColor,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle dynamic color"
                    }
                )
            }
        )
        StandardSettingsDivider()
        StandardSettingsRow(
            icon = Icons.Default.DarkMode,
            title = "True black",
            subtitle = "Pure-black background in dark mode",
            trailing = {
                Switch(
                    checked = state.trueBlack,
                    onCheckedChange = onTrueBlack,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle true black"
                    }
                )
            }
        )
    }
}

private val AccentColor.previewColor: Color
    get() = when (this) {
        AccentColor.Cyan -> Color(0xFF52D3DC)
        AccentColor.Blue -> Color(0xFF79B8FF)
        AccentColor.Violet -> Color(0xFFBC8CFF)
        AccentColor.Coral -> Color(0xFFFF9B8F)
        AccentColor.Amber -> Color(0xFFF2CC60)
    }
