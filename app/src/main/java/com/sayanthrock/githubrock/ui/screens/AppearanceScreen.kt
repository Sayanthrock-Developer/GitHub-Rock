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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.sayanthrock.githubrock.data.settings.AnimationStyle
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle
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
fun AppearanceScreen(onBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceContent(state, onBack, viewModel::setThemeMode, viewModel::setAccentColor, viewModel::setDynamicColor, viewModel::setTrueBlack, viewModel::setThemeStyle, viewModel::setDisplaySize, viewModel::setFontSize, viewModel::setFontWeight, viewModel::setFontFamily, viewModel::setLoadingStyle, viewModel::setAnimationStyle, viewModel::setCodeColorStyle, viewModel::setLogDisplayStyle, viewModel::setShowImages, viewModel::setNavigationBarStyle, viewModel::resetAppearance)
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
    onAnimationStyle: (AnimationStyle) -> Unit = {},
    onCodeColorStyle: (CodeColorStyle) -> Unit = {},
    onLogDisplayStyle: (LogDisplayStyle) -> Unit = {},
    onShowImages: (Boolean) -> Unit = {},
    onNavigationBarStyle: (NavigationBarStyle) -> Unit = {},
    onReset: () -> Unit = {}
) {
    var confirmReset by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { StandardScreenHeader("Customize your experience", "Choose the visual system, scale, typography, loading style, animation, and code presentation.") }
            item { StandardSectionHeader("Theme") }
            item { ThemePreview(state) }
            item { ChoiceCard("Design style", "Six complete surface and shape systems", Icons.Default.Palette, ThemeStyle.entries.map { it to it.displayName }, state.themeStyle, onThemeStyle) }
            item { AccentPicker(state.accentColor, !state.dynamicColor, onAccentColor) }
            item { ThemeControls(state, onThemeMode, onDynamicColor, onTrueBlack, onShowImages) }
            item { StandardSectionHeader("Navigation") }
            item { NavigationBarStyleControl(state.navigationBarStyle, onNavigationBarStyle) }
            item { StandardSectionHeader("Display size") }
            item { ChoiceCard("Interface scale", "Changes controls, cards, spacing, and navigation app-wide", Icons.Default.ViewCompact, listOf(DisplaySize.Large to "Large", DisplaySize.Standard to "Standard", DisplaySize.Small to "Small"), state.displaySize, onDisplaySize) }
            item { StandardSectionHeader("Fonts") }
            item { ChoiceCard("Font family", "System sans, serif, or developer monospace", Icons.Default.TextFields, AppFontFamily.entries.map { it to it.displayName }, state.fontFamily, onFontFamily) }
            item { ChoiceCard("Font size", "Small, default, or large readable text", Icons.Default.FormatSize, listOf(FontSize.Small to "Small", FontSize.Default to "Default", FontSize.Large to "Large"), state.fontSize, onFontSize) }
            item { ChoiceCard("Font weight", "Light, default, or stronger text", Icons.Default.FormatSize, listOf(FontWeightStyle.Light to "Light", FontWeightStyle.Default to "Default", FontWeightStyle.Bold to "Bold"), state.fontWeight, onFontWeight) }
            item { TypographyPreview() }
            item { StandardSectionHeader("Animation") }
            item { AnimationStyleControl(state.animationStyle, state.reduceMotion, onAnimationStyle) }
            item { StandardSectionHeader("Loading and code") }
            item { ChoiceCard("Loading animation", "Applied to repository loading and repository operations", Icons.Default.PlayArrow, LoadingStyle.entries.map { it to it.name }, state.loadingStyle, onLoadingStyle) }
            item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AppLoadingIndicator(state.loadingStyle, state.reduceMotion); Text("Live loading preview", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }
            item { ChoiceCard("Code colors", "Syntax colors stay isolated from the rest of the interface", Icons.Default.Code, CodeColorStyle.entries.map { it to it.displayName }, state.codeColorStyle, onCodeColorStyle) }
            item { ChoiceCard("Log display style", "Scrollable popup or a full-screen highlighted terminal", Icons.Default.Code, listOf(LogDisplayStyle.Dialog to "Popup dialog", LogDisplayStyle.Terminal to "On-screen terminal"), state.logDisplayStyle, onLogDisplayStyle) }
            item { CodeColorPreview() }
            item { OutlinedButton(onClick = { confirmReset = true }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.RestartAlt, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Reset settings") } }
            item { Text("Reset restores visual defaults. Your GitHub connection, downloads, and saved repositories stay unchanged.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset settings?") }, text = { Text("Theme, accent and dynamic colors, true black, remote images, navigation bar style, display size, fonts, loading, animation, code colors, and log presentation will return to defaults.") }, confirmButton = { Button(onClick = { confirmReset = false; onReset() }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
private fun NavigationBarStyleControl(selected: NavigationBarStyle, onSelected: (NavigationBarStyle) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ViewCompact, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Navigation Bar Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose how Home, Repositories, Builds, Downloads, and Profile are presented.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavigationBarStyle.entries.forEach { style ->
                    FilterChip(
                        selected = selected == style,
                        onClick = { onSelected(style) },
                        label = { Text(style.displayName) },
                        leadingIcon = if (selected == style) ({ Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }) else null,
                        modifier = Modifier.semantics { contentDescription = "Navigation bar style: ${style.displayName}" }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationStyleControl(selected: AnimationStyle, reduceMotion: Boolean, onSelected: (AnimationStyle) -> Unit) {
    val styles = AnimationStyle.entries
    var value by remember(selected) { mutableFloatStateOf(styles.indexOf(selected).coerceAtLeast(0).toFloat()) }
    val current = styles[value.toInt().coerceIn(0, styles.lastIndex)]
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) { Text("Animation style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Swipe the pill to change the motion system", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Slider(value = value, onValueChange = { value = it }, onValueChangeFinished = { onSelected(styles[value.toInt().coerceIn(0, styles.lastIndex)]) }, valueRange = 0f..styles.lastIndex.toFloat(), steps = styles.size - 2, enabled = !reduceMotion, modifier = Modifier.semantics { contentDescription = "Animation style slider" })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(styles.first().displayName, style = MaterialTheme.typography.labelSmall); Text(styles.last().displayName, style = MaterialTheme.typography.labelSmall) }
                }
            }
            Text(if (reduceMotion) "Reduced motion is enabled; animation effects are minimized." else "Liquid · Spring · Cinematic · Magnetic · Dynamic", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ThemePreview(state: AppearancePreferences) { GlassCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Surface(Modifier.size(52.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }; Column(Modifier.weight(1f)) { Text(state.themeStyle.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${state.themeMode.name} mode · ${state.displaySize.name} display · ${state.fontSize.name} text", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.Check, contentDescription = "Selected theme", tint = MaterialTheme.colorScheme.primary) } } }

@Composable private fun <T> ChoiceCard(title: String, subtitle: String, icon: ImageVector, choices: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { choices.forEach { (value, label) -> FilterChip(selected == value, { onSelected(value) }, label = { Text(label) }, leadingIcon = if (selected == value) ({ Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }) else null) } } } } }

@Composable private fun AccentPicker(selected: AccentColor, enabled: Boolean, onSelected: (AccentColor) -> Unit) { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Column { Text("Accent color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(if (enabled) "Choose the interface highlight" else "System dynamic color is active", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) { AccentColor.entries.forEach { accent -> val isSelected = selected == accent; Surface(Modifier.size(48.dp).selectable(isSelected, enabled, Role.RadioButton) { onSelected(accent) }.semantics { contentDescription = "Use ${accent.name} accent" }, shape = CircleShape, color = accent.previewColor, border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)) { if (isSelected) Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF071012)) } } } } } } }

@Composable private fun ThemeControls(state: AppearancePreferences, onThemeMode: (ThemeMode) -> Unit, onDynamicColor: (Boolean) -> Unit, onTrueBlack: (Boolean) -> Unit, onShowImages: (Boolean) -> Unit) { StandardSettingsGroup { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Column { Text("Color mode", style = MaterialTheme.typography.titleSmall); Text("Follow the system, stay light, or stay dark", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(state.themeMode == mode, { onThemeMode(mode) }, label = { Text(mode.name) }, modifier = Modifier.weight(1f)) } } }; StandardSettingsDivider(); ToggleRow(Icons.Default.Image, "Show remote images", "Avatars and repository artwork", state.showImages, onShowImages); StandardSettingsDivider(); ToggleRow(Icons.Default.ColorLens, "System dynamic color", "Use the Android wallpaper palette", state.dynamicColor, onDynamicColor); StandardSettingsDivider(); ToggleRow(Icons.Default.DarkMode, "True black", "Pure black in dark mode", state.trueBlack, onTrueBlack) } }

@Composable private fun TypographyPreview() { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Interface preview", style = MaterialTheme.typography.headlineSmall); Text("Clean typography preview", style = MaterialTheme.typography.titleMedium); Text("Repositories, workflows, releases, and code remain readable at every selected size.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun CodeColorPreview() { val colors = LocalCodeColors.current; val code = buildAnnotatedString { withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append("fun ") }; withStyle(SpanStyle(color = colors.type)) { append("publishRelease") }; append("() {\n  "); withStyle(SpanStyle(color = colors.keyword)) { append("val ") }; withStyle(SpanStyle(color = colors.property)) { append("version") }; append(" = "); withStyle(SpanStyle(color = colors.string)) { append("\"1.0.0\"") }; append("\n  "); withStyle(SpanStyle(color = colors.comment)) { append("// Signed and verified") }; append("\n  "); withStyle(SpanStyle(color = colors.type)) { append("release") }; append("("); withStyle(SpanStyle(color = colors.number)) { append("100") }; append(")\n}") }; GlassCard { Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { StandardSettingsRow(icon, title, subtitle) { Switch(checked, onCheckedChange, Modifier.semantics { contentDescription = "Toggle $title" }) } }

private val AnimationStyle.displayName: String get() = when (this) { AnimationStyle.Liquid -> "Liquid"; AnimationStyle.Spring -> "Spring"; AnimationStyle.Cinematic -> "Cinematic"; AnimationStyle.Magnetic -> "Magnetic"; AnimationStyle.Dynamic -> "Dynamic" }
private val ThemeStyle.displayName: String get() = when (this) { ThemeStyle.Clean -> "Clean"; ThemeStyle.LiquidGlass -> "Liquid glass"; ThemeStyle.Studio -> "Studio"; ThemeStyle.Midnight -> "Midnight"; ThemeStyle.Aurora -> "Aurora"; ThemeStyle.HighContrast -> "High contrast" }
private val AppFontFamily.displayName: String get() = when (this) { AppFontFamily.SystemSans -> "System sans"; AppFontFamily.Serif -> "Serif"; AppFontFamily.Monospace -> "Monospace" }
private val CodeColorStyle.displayName: String get() = when (this) { CodeColorStyle.Classic -> "Classic"; CodeColorStyle.Ocean -> "Ocean"; CodeColorStyle.Sunset -> "Sunset"; CodeColorStyle.Monochrome -> "Mono"; CodeColorStyle.GitHub -> "GitHub" }
private val NavigationBarStyle.displayName: String get() = when (this) { NavigationBarStyle.FloatingCapsule -> "Floating Capsule"; NavigationBarStyle.Classic -> "Classic"; NavigationBarStyle.Minimal -> "Minimal"; NavigationBarStyle.Glass -> "Glass"; NavigationBarStyle.Compact -> "Compact" }
private val AccentColor.previewColor: Color get() = when (this) { AccentColor.Cyan -> Color(0xFF52D3DC); AccentColor.Blue -> Color(0xFF79B8FF); AccentColor.Violet -> Color(0xFFBC8CFF); AccentColor.Emerald -> Color(0xFF56D364); AccentColor.Rose -> Color(0xFFFF8FB3); AccentColor.Coral -> Color(0xFFFF9B8F); AccentColor.Amber -> Color(0xFFF2CC60); AccentColor.Orange -> Color(0xFFFFA657) }
