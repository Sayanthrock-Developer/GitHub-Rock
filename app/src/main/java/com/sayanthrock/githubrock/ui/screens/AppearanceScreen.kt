package com.sayanthrock.githubrock.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
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
        onDynamicColor = viewModel::setDynamicColor,
        onTrueBlack = viewModel::setTrueBlack,
        onShowImages = viewModel::setShowImages,
        onWorkflowPreview = viewModel::setWorkflowPreview,
        onWorkflowStepDetails = viewModel::setWorkflowStepDetails,
        onStatusColors = viewModel::setStatusColors,
        onActionsControls = viewModel::setActionsControls,
        onRepositoryManager = viewModel::setRepositoryManager,
        onFileTools = viewModel::setFileTools,
        onCompactCards = viewModel::setCompactCards,
        onReduceMotion = viewModel::setReduceMotion,
        onReset = viewModel::resetAppearance
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceContent(
    state: AppearancePreferences,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onThemeStyle: (ThemeStyle) -> Unit = {},
    onAccentColor: (AccentColor) -> Unit,
    onDisplaySize: (DisplaySize) -> Unit = {},
    onFontSize: (FontSize) -> Unit = {},
    onFontWeight: (FontWeightStyle) -> Unit = {},
    onFontFamily: (AppFontFamily) -> Unit = {},
    onLoadingStyle: (LoadingStyle) -> Unit = {},
    onCodeColorStyle: (CodeColorStyle) -> Unit = {},
    onDynamicColor: (Boolean) -> Unit,
    onTrueBlack: (Boolean) -> Unit,
    onShowImages: (Boolean) -> Unit = {},
    onWorkflowPreview: (Boolean) -> Unit = {},
    onWorkflowStepDetails: (Boolean) -> Unit = {},
    onStatusColors: (Boolean) -> Unit = {},
    onActionsControls: (Boolean) -> Unit = {},
    onRepositoryManager: (Boolean) -> Unit = {},
    onFileTools: (Boolean) -> Unit = {},
    onCompactCards: (Boolean) -> Unit = {},
    onReduceMotion: (Boolean) -> Unit = {},
    onReset: () -> Unit = {}
) {
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Make GitHub Rock yours",
                    subtitle = "Themes, colors, display size, fonts, loading motion, and code colors update immediately across the app."
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
                ThemeSettings(
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
                    subtitle = "Large, standard, or small controls and spacing",
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
                TypographyControls(
                    state = state,
                    onFontFamily = onFontFamily,
                    onFontSize = onFontSize,
                    onFontWeight = onFontWeight
                )
            }

            item { StandardSectionHeader("Loading & code") }
            item {
                LoadingAndCodeControls(
                    state = state,
                    onLoadingStyle = onLoadingStyle,
                    onCodeColorStyle = onCodeColorStyle
                )
            }

            item { StandardSectionHeader("Feature controls") }
            item {
                FeatureControls(
                    state = state,
                    onWorkflowPreview = onWorkflowPreview,
                    onWorkflowStepDetails = onWorkflowStepDetails,
                    onStatusColors = onStatusColors,
                    onActionsControls = onActionsControls,
                    onRepositoryManager = onRepositoryManager,
                    onFileTools = onFileTools,
                    onCompactCards = onCompactCards,
                    onReduceMotion = onReduceMotion
                )
            }

            item {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset appearance")
                }
            }
            item {
                Text(
                    "Reset restores clean standard defaults only. Your GitHub connection, downloads, repositories, and favorites are not removed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset appearance?") },
            text = { Text("Themes, colors, display, fonts, loading, and visual feature controls will return to their clean defaults.") },
            confirmButton = {
                Button(onClick = {
                    confirmReset = false
                    onReset()
                }) { Text("Reset") }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        state.themeStyle.previewSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(Icons.Default.Check, contentDescription = "Selected theme", tint = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PreviewPill("${state.displaySize.name} display")
                PreviewPill("${state.fontSize.name} text")
                PreviewPill(state.fontFamily.displayName)
            }
        }
    }
}

@Composable
private fun PreviewPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
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
                Column {
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
                        } else null
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
                        if (enabled) "Choose the highlight used for actions and selection" else "System dynamic color is active",
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
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF071012), modifier = Modifier.size(22.dp))
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
        ToggleRow(Icons.Default.Image, "Show remote images", "Load avatars and repository artwork", state.showImages, onShowImages)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.ColorLens, "System dynamic color", "Use your Android wallpaper palette", state.dynamicColor, onDynamicColor)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.DarkMode, "True black", "Pure black background in dark mode", state.trueBlack, onTrueBlack)
    }
}

@Composable
private fun TypographyControls(
    state: AppearancePreferences,
    onFontFamily: (AppFontFamily) -> Unit,
    onFontSize: (FontSize) -> Unit,
    onFontWeight: (FontWeightStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = "Font family",
            subtitle = "System sans, serif, or developer monospace",
            icon = Icons.Default.TextFields,
            choices = AppFontFamily.entries.map { it to it.displayName },
            selected = state.fontFamily,
            onSelected = onFontFamily
        )
        ChoiceCard(
            title = "Font size",
            subtitle = "Readable scaling that respects Android accessibility",
            icon = Icons.Default.FormatSize,
            choices = listOf(FontSize.Small to "Small", FontSize.Default to "Default", FontSize.Large to "Large"),
            selected = state.fontSize,
            onSelected = onFontSize
        )
        ChoiceCard(
            title = "Font weight",
            subtitle = "Choose lighter, default, or stronger text",
            icon = Icons.Default.FormatSize,
            choices = listOf(FontWeightStyle.Light to "Light", FontWeightStyle.Default to "Default", FontWeightStyle.Bold to "Bold"),
            selected = state.fontWeight,
            onSelected = onFontWeight
        )
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("GitHub Rock", style = MaterialTheme.typography.headlineSmall)
                Text("Clean typography preview", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Repository, workflow, release, and code tools stay readable at every selected size.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingAndCodeControls(
    state: AppearancePreferences,
    onLoadingStyle: (LoadingStyle) -> Unit,
    onCodeColorStyle: (CodeColorStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = "Loading animation",
            subtitle = "Spinner, progress bar, or soft pulse",
            icon = Icons.Default.Animation,
            choices = LoadingStyle.entries.map { it to it.name },
            selected = state.loadingStyle,
            onSelected = onLoadingStyle
        )
        LoadingPreview(state.loadingStyle, state.reduceMotion)
        ChoiceCard(
            title = "Code colors",
            subtitle = "Change syntax colors without changing repository content",
            icon = Icons.Default.Code,
            choices = CodeColorStyle.entries.map { it to it.displayName },
            selected = state.codeColorStyle,
            onSelected = onCodeColorStyle
        )
        CodeColorPreview()
    }
}

@Composable
private fun LoadingPreview(style: LoadingStyle, reduceMotion: Boolean) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                when (style) {
                    LoadingStyle.Spinner -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    LoadingStyle.Linear -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    LoadingStyle.Pulse -> {
                        val transition = rememberInfiniteTransition(label = "loading-pulse")
                        val pulse by transition.animateFloat(
                            initialValue = if (reduceMotion) 1f else .72f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                            label = "loading-pulse-scale"
                        )
                        Surface(
                            modifier = Modifier.size(28.dp).graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                                alpha = pulse
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
            }
            Column {
                Text("Loading preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (reduceMotion) "Reduced motion is active" else "Used for repository and workflow progress",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
private fun FeatureControls(
    state: AppearancePreferences,
    onWorkflowPreview: (Boolean) -> Unit,
    onWorkflowStepDetails: (Boolean) -> Unit,
    onStatusColors: (Boolean) -> Unit,
    onActionsControls: (Boolean) -> Unit,
    onRepositoryManager: (Boolean) -> Unit,
    onFileTools: (Boolean) -> Unit,
    onCompactCards: (Boolean) -> Unit,
    onReduceMotion: (Boolean) -> Unit
) {
    StandardSettingsGroup {
        ToggleRow(Icons.Default.Visibility, "Workflow code preview", "Show the active workflow YAML inside the app", state.workflowPreview, onWorkflowPreview)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.Code, "Workflow step details", "Show every job and step result", state.workflowStepDetails, onWorkflowStepDetails)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.Palette, "Status colors", "Red problems and green successful states", state.statusColors, onStatusColors)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.PlayArrow, "Actions controls", "Allow workflow run, refresh, and artifacts", state.actionsControls, onActionsControls)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.Tune, "Repository manager", "Show code, Issues, Pull Requests, Actions, and Releases", state.repositoryManager, onRepositoryManager)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.FolderOpen, "File tools", "Allow file viewing, copying, and review-branch uploads", state.fileTools, onFileTools)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.ViewCompact, "Compact cards", "Use tighter spacing for information-dense screens", state.compactCards, onCompactCards)
        StandardSettingsDivider()
        ToggleRow(Icons.Default.MotionPhotosOff, "Reduce motion", "Limit nonessential animation", state.reduceMotion, onReduceMotion)
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

private val ThemeStyle.previewSubtitle: String
    get() = when (this) {
        ThemeStyle.Clean -> "Calm surfaces · clear hierarchy · Material 3"
        ThemeStyle.LiquidGlass -> "Large curves · layered surfaces · premium spacing"
        ThemeStyle.Studio -> "Sharper frames · dense developer workspace"
        ThemeStyle.Midnight -> "Deep navy surfaces · focused low-light workspace"
        ThemeStyle.Aurora -> "Soft green layers · bright modern contrast"
        ThemeStyle.HighContrast -> "Maximum separation · stronger borders · clear text"
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
