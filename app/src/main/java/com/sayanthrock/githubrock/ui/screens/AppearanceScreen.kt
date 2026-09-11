package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurComponent
import com.sayanthrock.githubrock.ui.components.AppLoadingIndicator
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow
import com.sayanthrock.githubrock.ui.theme.LocalCodeColors
import com.sayanthrock.githubrock.ui.theme.parseAccentHex

@Composable
fun AppearanceScreen(onBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val blurState by viewModel.blurState.collectAsStateWithLifecycle()
    AppearanceContent(
        state = state,
        onBack = onBack,
        onThemeMode = viewModel::setThemeMode,
        onAccentColor = viewModel::setAccentColor,
        onDynamicColor = viewModel::setDynamicColor,
        onTrueBlack = viewModel::setTrueBlack,
        onCustomAccentHex = viewModel::setCustomAccentHex,
        onSystemDynamicAccent = viewModel::setSystemDynamicAccent,
        onThemeStyle = viewModel::setThemeStyle,
        onDisplaySize = viewModel::setDisplaySize,
        onFontSize = viewModel::setFontSize,
        onFontWeight = viewModel::setFontWeight,
        onFontFamily = viewModel::setFontFamily,
        onLoadingStyle = viewModel::setLoadingStyle,
        onAnimationStyle = viewModel::setAnimationStyle,
        onCodeColorStyle = viewModel::setCodeColorStyle,
        onLogDisplayStyle = viewModel::setLogDisplayStyle,
        onShowImages = viewModel::setShowImages,
        onNavigationBarStyle = viewModel::setNavigationBarStyle,
        onReset = viewModel::resetAppearance,
        blurState = blurState,
        onBlurMode = viewModel::setBlurMode,
        onBlurPreset = viewModel::setBlurPreset,
        onBlurProfile = viewModel::setBlurProfile,
        onBlurCustomTint = viewModel::setBlurCustomTint,
        onBlurComponentProfile = viewModel::setBlurComponentProfile,
        onBlurReset = viewModel::resetBlurSettings,
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
    onCustomAccentHex: (String) -> Unit = {},
    onSystemDynamicAccent: () -> Unit = {},
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
    onReset: () -> Unit = {},
    blurState: ApplicationBlurSettings = ApplicationBlurPreset.Clean.toSettings(),
    onBlurMode: (ApplicationBlurMode) -> Unit = {},
    onBlurPreset: (ApplicationBlurPreset) -> Unit = {},
    onBlurProfile: (ApplicationBlurProfile) -> Unit = {},
    onBlurCustomTint: (String) -> Unit = {},
    onBlurComponentProfile: (ApplicationBlurComponent, ApplicationBlurProfile?) -> Unit = { _, _ -> },
    onBlurReset: () -> Unit = {},
) {
    var confirmReset by remember { mutableStateOf(false) }
    var showAccentPicker by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StandardScreenHeader("Customize your experience", "Choose the visual system, scale, typography, loading style, animation, and code presentation.") }
            item { StandardSectionHeader("Theme") }
            item { ThemePreview(state) }
            item { ChoiceCard("Design style", "Complete surface and shape system", Icons.Default.Palette, ThemeStyle.entries.map { it to it.displayName }, state.themeStyle, onThemeStyle) }
            item { AccentPicker(state, onAccentColor, onSystemDynamicAccent, onCustomAccentHex) { showAccentPicker = true } }
            item { ThemeControls(state, onThemeMode, onDynamicColor, onTrueBlack, onShowImages) }
            item { StandardSectionHeader("Navigation") }
            item { NavigationBarStyleControl(state.navigationBarStyle, onNavigationBarStyle) }
            item { StandardSectionHeader("Application Blur") }
            item {
                ApplicationBlurEditor(
                    settings = blurState,
                    onMode = onBlurMode,
                    onPreset = onBlurPreset,
                    onProfile = onBlurProfile,
                    onCustomTint = onBlurCustomTint,
                    onComponentProfile = onBlurComponentProfile,
                    onReset = onBlurReset,
                )
            }
            item { StandardSectionHeader("Display size") }
            item { ChoiceCard("Interface scale", "Changes controls, cards, spacing, and navigation app-wide", Icons.Default.ViewCompact, listOf(DisplaySize.Large to "Large", DisplaySize.Standard to "Standard", DisplaySize.Small to "Small"), state.displaySize, onDisplaySize) }
            item { StandardSectionHeader("Fonts") }
            item { ChoiceCard("Font family", "System sans, serif, or developer monospace", Icons.Default.TextFields, AppFontFamily.entries.map { it to it.displayName }, state.fontFamily, onFontFamily) }
            item { ChoiceCard("Font size", "Readable text scale", Icons.Default.FormatSize, listOf(FontSize.Small to "Small", FontSize.Default to "Default", FontSize.Large to "Large"), state.fontSize, onFontSize) }
            item { ChoiceCard("Font weight", "Light, default, or stronger text", Icons.Default.FormatSize, listOf(FontWeightStyle.Light to "Light", FontWeightStyle.Default to "Default", FontWeightStyle.Bold to "Bold"), state.fontWeight, onFontWeight) }
            item { TypographyPreview() }
            item { StandardSectionHeader("Animation") }
            item { AnimationStyleControl(state.animationStyle, state.reduceMotion, onAnimationStyle) }
            item { StandardSectionHeader("Loading and code") }
            item { ChoiceCard("Loading animation", "Applied to repository operations", Icons.Default.PlayArrow, LoadingStyle.entries.map { it to it.name }, state.loadingStyle, onLoadingStyle) }
            item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AppLoadingIndicator(state.loadingStyle, state.reduceMotion); Text("Live loading preview", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }
            item { ChoiceCard("Code colors", "Syntax colors stay isolated from the interface", Icons.Default.Code, CodeColorStyle.entries.map { it to it.displayName }, state.codeColorStyle, onCodeColorStyle) }
            item { ChoiceCard("Log display style", "Popup or highlighted terminal", Icons.Default.Code, listOf(LogDisplayStyle.Dialog to "Popup dialog", LogDisplayStyle.Terminal to "On-screen terminal"), state.logDisplayStyle, onLogDisplayStyle) }
            item { CodeColorPreview() }
            item { OutlinedButton(onClick = { confirmReset = true }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Reset settings") } }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset settings?") }, text = { Text("Theme, accent, dynamic colors, true black, remote images, navigation, display, fonts, loading, animation, code colors, and log presentation will return to defaults.") }, confirmButton = { Button(onClick = { confirmReset = false; onReset() }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
    if (showAccentPicker) AccentColorPickerDialog(state.customAccentHex.ifBlank { "#52D3DC" }, state.recentAccentColors, { showAccentPicker = false }) { hex -> showAccentPicker = false; onCustomAccentHex(hex) }
}

@Composable private fun NavigationBarStyleControl(selected: NavigationBarStyle, onSelected: (NavigationBarStyle) -> Unit) = GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.ViewCompact, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text("Navigation Bar Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Choose how the five main destinations are presented.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { NavigationBarStyle.entries.forEach { style -> FilterChip(selected == style, { onSelected(style) }, label = { Text(style.displayName) }, leadingIcon = if (selected == style) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null) } } } }

@Composable private fun AnimationStyleControl(selected: AnimationStyle, reduceMotion: Boolean, onSelected: (AnimationStyle) -> Unit) { val styles = AnimationStyle.entries; var value by remember(selected) { mutableFloatStateOf(styles.indexOf(selected).coerceAtLeast(0).toFloat()) }; val current = styles[value.toInt().coerceIn(0, styles.lastIndex)]; GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Animation style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(current.displayName, style = MaterialTheme.typography.titleSmall); Slider(value = value, onValueChange = { value = it }, onValueChangeFinished = { onSelected(styles[value.toInt().coerceIn(0, styles.lastIndex)]) }, valueRange = 0f..styles.lastIndex.toFloat(), steps = (styles.size - 2).coerceAtLeast(0), enabled = !reduceMotion); Text(if (reduceMotion) "Reduced motion is enabled." else "Liquid · Spring · Cinematic · Magnetic · Dynamic", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }

@Composable private fun ThemePreview(state: AppearancePreferences) = GlassCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Surface(Modifier.size(52.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) } }; Column(Modifier.weight(1f)) { Text(state.themeStyle.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${state.themeMode.name} mode · ${state.displaySize.name} display · ${state.fontSize.name} text", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.Check, "Selected theme", tint = MaterialTheme.colorScheme.primary) } }

@Composable private fun <T> ChoiceCard(title: String, subtitle: String, icon: ImageVector, choices: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) = GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { choices.forEach { (v, label) -> FilterChip(selected == v, { onSelected(v) }, label = { Text(label) }, leadingIcon = if (selected == v) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null) } } } }

@Composable private fun AccentPicker(state: AppearancePreferences, onSelected: (AccentColor) -> Unit, onSystemDynamic: () -> Unit, onCustomHex: (String) -> Unit, onOpenPicker: () -> Unit) {
    val presets = listOf(AccentColor.DefaultGitHubRock, AccentColor.Red, AccentColor.Orange, AccentColor.Yellow, AccentColor.Green, AccentColor.Teal, AccentColor.Cyan, AccentColor.Blue, AccentColor.Indigo, AccentColor.Purple, AccentColor.Pink)
    val customColor = parseAccentHex(state.customAccentHex) ?: MaterialTheme.colorScheme.primary
    val customSelected = state.accentColor == AccentColor.Custom && !state.dynamicColor
    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f)) { Text("Accent color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(if (state.dynamicColor) "System Dynamic is active" else "Accent works independently of Light / Dark mode", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
        FilterChip(state.dynamicColor, onSystemDynamic, label = { Text("System Dynamic") }, leadingIcon = if (state.dynamicColor) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            presets.forEach { accent -> val selected = !state.dynamicColor && state.accentColor == accent; Surface(Modifier.size(48.dp).selectable(selected, true, Role.RadioButton) { onSelected(accent) }.semantics { contentDescription = "Use ${accent.displayName} accent" }, shape = CircleShape, color = accent.previewColor, border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)) { if (selected) Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.Black) } } }
            Surface(Modifier.size(48.dp).selectable(customSelected, true, Role.Button) { onOpenPicker() }.semantics { contentDescription = "Open custom accent color picker" }, shape = CircleShape, color = customColor, border = BorderStroke(if (customSelected) 3.dp else 1.dp, MaterialTheme.colorScheme.outline)) { Box(contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold, color = readableOn(customColor)) } }
        }
        if (state.recentAccentColors.isNotEmpty()) { Text("Recent colors", style = MaterialTheme.typography.labelLarge); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { state.recentAccentColors.forEach { hex -> val color = parseAccentHex(hex) ?: return@forEach; Surface(Modifier.size(38.dp).selectable(false, true, Role.Button) { onCustomHex(hex) }, shape = CircleShape, color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {} } } }
        if (customSelected) Text("${state.customAccentHex} · custom accent", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    } }
}

private fun readableOn(color: Color): Color = if (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue > 0.55f) Color.Black else Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AccentColorPickerDialog(initialHex: String, recentColors: List<String>, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    val initial = parseAccentHex(initialHex) ?: Color(0xFF52D3DC)
    val hsv = remember(initialHex) { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember(initialHex) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialHex) { mutableFloatStateOf(hsv[1]) }
    var brightness by remember(initialHex) { mutableFloatStateOf(hsv[2]) }
    var hexText by remember(initialHex) { mutableStateOf(initialHex.uppercase()) }
    var pickerWidth by remember { mutableIntStateOf(0) }
    var pickerHeight by remember { mutableIntStateOf(0) }
    val selected = Color.hsv(hue, saturation, brightness)
    val hueColor = Color.hsv(hue, 1f, 1f)
    val valid = parseAccentHex(hexText) != null

    fun updateFromPoint(point: Offset) {
        if (pickerWidth <= 0 || pickerHeight <= 0) return
        saturation = (point.x / pickerWidth.toFloat()).coerceIn(0f, 1f)
        brightness = (1f - point.y / pickerHeight.toFloat()).coerceIn(0f, 1f)
        hexText = selectedHex(hue, saturation, brightness)
    }

    fun updateFromHex(text: String) {
        val normalized = text.take(9).uppercase()
        hexText = normalized
        val color = parseAccentHex(normalized) ?: return
        val next = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), next)
        hue = next[0]
        saturation = next[1]
        brightness = next[2]
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Custom accent color") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp).onSizeChanged { pickerWidth = it.width; pickerHeight = it.height }.pointerInput(hueColor, pickerWidth, pickerHeight) { detectTapGestures { point -> updateFromPoint(point) } }.pointerInput(hueColor, pickerWidth, pickerHeight) { detectDragGestures(onDragStart = { point -> updateFromPoint(point) }, onDrag = { change, _ -> updateFromPoint(change.position); change.consume() }) }.semantics { contentDescription = "Saturation and brightness color picker" }) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val x = saturation.coerceIn(0f, 1f) * size.width
            val y = (1f - brightness.coerceIn(0f, 1f)) * size.height
            drawCircle(Color.White, 9.dp.toPx(), center = Offset(x, y))
        }
        Slider(value = hue, onValueChange = { hue = it; hexText = selectedHex(it, saturation, brightness) }, valueRange = 0f..360f, modifier = Modifier.semantics { contentDescription = "Hue slider" })
        TextField(value = hexText, onValueChange = ::updateFromHex, label = { Text("HEX") }, singleLine = true, supportingText = { Text("Use #RRGGBB or #AARRGGBB") }, isError = hexText.isNotBlank() && !valid, modifier = Modifier.fillMaxWidth())
        Surface(Modifier.fillMaxWidth().height(52.dp), color = selected, shape = MaterialTheme.shapes.medium) {}
        if (recentColors.isNotEmpty()) { Text("Recent", style = MaterialTheme.typography.labelLarge); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { recentColors.forEach { recent -> val color = parseAccentHex(recent) ?: return@forEach; Surface(Modifier.size(34.dp).selectable(false, true, Role.Button) { updateFromHex(recent) }, shape = CircleShape, color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {} } } }
    } }, confirmButton = { Button(onClick = { if (valid) onApply(hexText) }, enabled = valid) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun selectedHex(hue: Float, saturation: Float, brightness: Float): String {
    val color = Color.hsv(hue, saturation, brightness)
    return "#%02X%02X%02X".format((color.red * 255f).toInt().coerceIn(0, 255), (color.green * 255f).toInt().coerceIn(0, 255), (color.blue * 255f).toInt().coerceIn(0, 255))
}

@Composable private fun ThemeControls(state: AppearancePreferences, onThemeMode: (ThemeMode) -> Unit, onDynamicColor: (Boolean) -> Unit, onTrueBlack: (Boolean) -> Unit, onShowImages: (Boolean) -> Unit) = StandardSettingsGroup {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary); Column { Text("Color mode", style = MaterialTheme.typography.titleSmall); Text("Follow the system, stay light, or stay dark", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(state.themeMode == mode, { onThemeMode(mode) }, label = { Text(mode.name) }, modifier = Modifier.weight(1f)) } } }
    StandardSettingsDivider(); ToggleRow(Icons.Default.Image, "Show remote images", "Avatars and repository artwork", state.showImages, onShowImages); StandardSettingsDivider(); ToggleRow(Icons.Default.ColorLens, "System dynamic color", "Use the Android wallpaper palette", state.dynamicColor, onDynamicColor); StandardSettingsDivider(); ToggleRow(Icons.Default.DarkMode, "True black", "Pure black in dark mode", state.trueBlack, onTrueBlack)
}

@Composable private fun TypographyPreview() = GlassCard { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Interface preview", style = MaterialTheme.typography.headlineSmall); Text("Clean typography preview", style = MaterialTheme.typography.titleMedium); Text("Repositories, workflows, releases, and code remain readable at every selected size.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun CodeColorPreview() { val colors = LocalCodeColors.current; val code = buildAnnotatedString { withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append("fun ") }; withStyle(SpanStyle(color = colors.type)) { append("publishRelease") }; append("() {\n  "); withStyle(SpanStyle(color = colors.keyword)) { append("val ") }; withStyle(SpanStyle(color = colors.property)) { append("version") }; append(" = "); withStyle(SpanStyle(color = colors.string)) { append("\"1.0.0\"") }; append("\n  "); withStyle(SpanStyle(color = colors.comment)) { append("// Signed and verified") }; append("\n  "); withStyle(SpanStyle(color = colors.type)) { append("release") }; append("("); withStyle(SpanStyle(color = colors.number)) { append("100") }; append(")\n}") }; GlassCard { Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { StandardSettingsRow(icon, title, subtitle) { Switch(checked, onCheckedChange) } }

private val AnimationStyle.displayName: String get() = when (this) { AnimationStyle.Liquid -> "Liquid"; AnimationStyle.Spring -> "Spring"; AnimationStyle.Cinematic -> "Cinematic"; AnimationStyle.Magnetic -> "Magnetic"; AnimationStyle.Dynamic -> "Dynamic" }
private val ThemeStyle.displayName: String get() = when (this) { ThemeStyle.Clean -> "Clean"; ThemeStyle.LiquidGlass -> "Liquid glass"; ThemeStyle.Studio -> "Studio"; ThemeStyle.Midnight -> "Midnight"; ThemeStyle.Aurora -> "Aurora"; ThemeStyle.HighContrast -> "High contrast"; ThemeStyle.Obsidian -> "Obsidian" }
private val AppFontFamily.displayName: String get() = when (this) { AppFontFamily.SystemSans -> "System sans"; AppFontFamily.Serif -> "Serif"; AppFontFamily.Monospace -> "Monospace" }
private val CodeColorStyle.displayName: String get() = when (this) { CodeColorStyle.Classic -> "Classic"; CodeColorStyle.Ocean -> "Ocean"; CodeColorStyle.Sunset -> "Sunset"; CodeColorStyle.Monochrome -> "Mono"; CodeColorStyle.GitHub -> "GitHub" }
private val NavigationBarStyle.displayName: String get() = when (this) { NavigationBarStyle.FloatingCapsule -> "Floating Capsule"; NavigationBarStyle.Classic -> "Classic"; NavigationBarStyle.Minimal -> "Minimal"; NavigationBarStyle.Glass -> "Glass"; NavigationBarStyle.Compact -> "Compact"; NavigationBarStyle.Ios -> "iOS" }
private val AccentColor.displayName: String get() = when (this) { AccentColor.DefaultGitHubRock -> "Default GitHub Rock"; AccentColor.Red -> "Red"; AccentColor.Orange -> "Orange"; AccentColor.Yellow -> "Yellow"; AccentColor.Green -> "Green"; AccentColor.Teal -> "Teal"; AccentColor.Cyan -> "Cyan"; AccentColor.Blue -> "Blue"; AccentColor.Indigo -> "Indigo"; AccentColor.Purple -> "Purple"; AccentColor.Pink -> "Pink"; AccentColor.Custom -> "Custom"; AccentColor.Violet -> "Violet"; AccentColor.Emerald -> "Emerald"; AccentColor.Rose -> "Rose"; AccentColor.Coral -> "Coral"; AccentColor.Amber -> "Amber" }
private val AccentColor.previewColor: Color get() = when (this) { AccentColor.DefaultGitHubRock -> Color(0xFF52D3DC); AccentColor.Red -> Color(0xFFFF4D4D); AccentColor.Orange -> Color(0xFFFFA657); AccentColor.Yellow -> Color(0xFFF2CC60); AccentColor.Green -> Color(0xFF56D364); AccentColor.Teal -> Color(0xFF5DD6C7); AccentColor.Cyan -> Color(0xFF52D3DC); AccentColor.Blue -> Color(0xFF79B8FF); AccentColor.Indigo -> Color(0xFFB7A1FF); AccentColor.Purple -> Color(0xFFD0A8FF); AccentColor.Pink -> Color(0xFFFF9BC1); AccentColor.Custom -> Color(0xFF52D3DC); AccentColor.Violet -> Color(0xFFBC8CFF); AccentColor.Emerald -> Color(0xFF56D364); AccentColor.Rose -> Color(0xFFFF8FB3); AccentColor.Coral -> Color(0xFFFF9B8F); AccentColor.Amber -> Color(0xFFF2CC60) }
