package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.components.AppLoadingIndicator
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow
import com.sayanthrock.githubrock.ui.theme.LocalCodeColors

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
    onAccentColor: (AccentColor) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit,
    onThemeStyle: (ThemeStyle) -> Unit = {},
    onDisplaySize: (DisplaySize) -> Unit = {},
    onFontSize: (FontSize) -> Unit = {},
    onFontWeight: (FontWeightStyle) -> Unit = {},
    onFontFamily: (AppFontFamily) -> Unit = {},
    onLoadingStyle: (LoadingStyle) -> Unit = {},
    onCodeColorStyle: (CodeColorStyle) -> Unit = {},
    onLogDisplayStyle: (LogDisplayStyle) -> Unit = {},
    onShowImages: (Boolean) -> Unit = {},
    onReset: () -> Unit = {}
) {
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Customize your experience",
                    subtitle = "Choose the visual system, scale, typography, loading style, and code presentation."
                )
            }

            item { StandardSectionHeader("Theme") }
            item { ThemePreview(state) }
            item {
                ChoiceCard(
                    title = "Design style",
                    subtitle = "Six complete surface and shape systems",
                    icon = Icons.Default.Palette,
                    choices = ThemeStyle.entries.map { it to it.displayName },
                    selected = state.themeStyle,
                    onSelected = onThemeStyle
                )
            }
            item { AccentPicker(state.accentColor, !state.dynamicColor, onAccentColor) }
            item {
                ThemeControls(
                    state = state,
                    onThemeMode = onThemeMode,
                    onDynamicColor = onDynamicColor,
                    onTrueBlack = onTrueBlack,
                    onShowImages = onShowImages
                )
            }

            item { StandardSectionHeader("Display size") }
            item {
                ChoiceCard(
                    title = "Interface scale",
                    subtitle = "Changes controls, cards, spacing, and navigation app-wide",
                    icon = Icons.Default.ViewCompact,
                    choices = listOf(
                        DisplaySize.Large to "Large",
                        DisplaySize.Standard to "Standard",
                        DisplaySize.Small to "Small"
                    ),
                    selected = state.displaySize,
                    onSelected = onDisplaySize
                )
            }

            item { StandardSectionHeader("Fonts") }
            item {
                ChoiceCard(
                    title = "Font family",
                    subtitle = "System sans, serif, or developer monospace",
                    icon = Icons.Default.TextFields,
                    choices = AppFontFamily.entries.map { it to it.displayName },
                    selected = state.fontFamily,
                    onSelected = onFontFamily
                )
            }
            item {
                ChoiceCard(
                    title = "Font size",
                    subtitle = "Small, default, or large readable text",
                    icon = Icons.Default.FormatSize,
                    choices = listOf(
                        FontSize.Small to "Small",
                        FontSize.Default to "Default",
                        FontSize.Large to "Large"
                    ),
                    selected = state.fontSize,
                    onSelected = onFontSize
                )
            }
            item {
                ChoiceCard(
                    title = "Font weight",
                    subtitle = "Light, default, or stronger text",
                    icon = Icons.Default.FormatSize,
                    choices = listOf(
                        FontWeightStyle.Light to "Light",
                        FontWeightStyle.Default to "Default",
                        FontWeightStyle.Bold to "Bold"
                    ),
                    selected = state.fontWeight,
                    onSelected = onFontWeight
                )
            }
            item { TypographyPreview() }

            item { StandardSectionHeader("Loading and code") }
            item {
                ChoiceCard(
                    title = "Loading animation",
                    subtitle = "Applied to repository loading and repository operations",
                    icon = Icons.Default.PlayArrow,
                    choices = LoadingStyle.entries.map { it to it.name },
                    selected = state.loadingStyle,
                    onSelected = onLoadingStyle
                )
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLoadingIndicator(style = state.loadingStyle, reduceMotion = false)
                        Text(
                            "Live loading preview",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                ChoiceCard(
                    title = "Code colors",
                    subtitle = "Syntax colors stay isolated from the rest of the interface",
                    icon = Icons.Default.Code,
                    choices = CodeColorStyle.entries.map { it to it.displayName },
                    selected = state.codeColorStyle,
                    onSelected = onCodeColorStyle
                )
            }
            item {
                ChoiceCard(
                    title = "Log display style",
                    subtitle = "Scrollable popup or a full-screen highlighted terminal",
                    icon = Icons.Default.Code,
                    choices = listOf(
                        LogDisplayStyle.Dialog to "Popup dialog",
                        LogDisplayStyle.Terminal to "On-screen terminal"
                    ),
                    selected = state.logDisplayStyle,
                    onSelected = onLogDisplayStyle
                )
            }
            item { CodeColorPreview() }

            item {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset settings")
                }
            }
            item {
                Text(
                    "Reset restores visual defaults. Your GitHub connection, downloads, and saved repositories stay unchanged.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = {
                Text(
                    "Theme, accent and dynamic colors, true black, remote images, display size, fonts, loading, code colors, and log presentation will return to defaults."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onReset()
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemePreview(state: AppearancePreferences) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            Column(Modifier.weight(1f)) {
                Text(state.themeStyle.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${state.themeMode.name} mode · ${state.displaySize.name} display · ${state.fontSize.name} text",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.Check, contentDescription = "Selected theme", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun <T> ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                choices.forEach { (value, label) ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelected(value) },
                        label = { Text(label) },
                        leadingIcon = if (selected == value) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        }
                    )
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Accent color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (enabled) "Choose the interface highlight" else "System dynamic color is active",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF071012))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeControls(
    state: AppearancePreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit,
    onShowImages: (Boolean) -> Unit
) {
    StandardSettingsGroup {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Color mode", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Follow the system, stay light, or stay dark",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
        ToggleRow(Icons.Default.Image, "Show remote images", "Avatars and repository artwork", state.showImages, onShowImages)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.ColorLens, "System dynamic color", "Use the Android wallpaper palette", state.dynamicColor, onDynamicColor)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.DarkMode, "True black", "Pure black in dark mode", state.trueBlack, onTrueBlack)
    }
}

@Composable
private fun TypographyPreview() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Interface preview", style = MaterialTheme.typography.headlineSmall)
            Text("Clean typography preview", style = MaterialTheme.typography.titleMedium)
            Text(
                "Repositories, workflows, releases, and code remain readable at every selected size.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodeColorPreview() {
    val colors = LocalCodeColors.current
    val code = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append("fun ") }
        withStyle(SpanStyle(color = colors.type)) { append("publishRelease") }
        append("() {\n  ")
        withStyle(SpanStyle(color = colors.keyword)) { append("val ") }
        withStyle(SpanStyle(color = colors.property)) { append("version") }
        append(" = ")
        withStyle(SpanStyle(color = colors.string)) { append("\"1.0.0\"") }
        append("\n  ")
        withStyle(SpanStyle(color = colors.comment)) { append("// Signed and verified") }
        append("\n  ")
        withStyle(SpanStyle(color = colors.type)) { append("release") }
        append("(")
        withStyle(SpanStyle(color = colors.number)) { append("100") }
        append(")\n}")
    }
    GlassCard {
        Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    StandardSettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = "Toggle $title" }
            )
        }
    )
}

private val ThemeStyle.displayName: String
    get() = when (this) {
        ThemeStyle.Clean -> "Clean"
        ThemeStyle.LiquidGlass -> "Liquid glass"
        ThemeStyle.Studio -> "Studio"
        ThemeStyle.Midnight -> "Midnight"
        ThemeStyle.Aurora -> "Aurora"
        ThemeStyle.HighContrast -> "High contrast"
    }

private val AppFontFamily.displayName: String
    get() = when (this) {
        AppFontFamily.SystemSans -> "System sans"
        AppFontFamily.Serif -> "Serif"
        AppFontFamily.Monospace -> "Monospace"
    }

private val CodeColorStyle.displayName: String
    get() = when (this) {
        CodeColorStyle.Classic -> "Classic"
        CodeColorStyle.Ocean -> "Ocean"
        CodeColorStyle.Sunset -> "Sunset"
        CodeColorStyle.Monochrome -> "Mono"
    }

private val AccentColor.previewColor: Color
    get() = when (this) {
        AccentColor.Cyan -> Color(0xFF52D3DC)
        AccentColor.Blue -> Color(0xFF79B8FF)
        AccentColor.Violet -> Color(0xFFBC8CFF)
        AccentColor.Emerald -> Color(0xFF56D364)
        AccentColor.Rose -> Color(0xFFFF8FB3)
        AccentColor.Coral -> Color(0xFFFF9B8F)
        AccentColor.Amber -> Color(0xFFF2CC60)
        AccentColor.Orange -> Color(0xFFFFA657)
    }
