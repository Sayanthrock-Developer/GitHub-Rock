package com.sayanthrock.githubrock.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = RockBlue,
    onPrimary = Color(0xFF260C0A),
    primaryContainer = Color(0xFF3D1C1A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = RockPurple,
    tertiary = RockGreen,
    background = RockBackground,
    surface = RockSurface,
    surfaceVariant = Color(0xFF171719),
    surfaceContainer = Color(0xFF111113),
    surfaceContainerHigh = Color(0xFF19191C),
    outline = RockBorder,
    outlineVariant = Color(0xFF202024),
    error = RockRed,
    onBackground = RockText,
    onSurface = RockText,
    onSurfaceVariant = RockMuted
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color.White,
    secondary = Color(0xFF8C1D18),
    tertiary = Color(0xFF147A4B),
    background = Color(0xFFFFF8F7),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF5E7E5),
    outline = Color(0xFFD8C2BF),
    error = Color(0xFFBA1A1A),
    onBackground = Color(0xFF241918),
    onSurface = Color(0xFF241918),
    onSurfaceVariant = Color(0xFF665A58)
)

/**
 * Dark-first GitHub Rock visual system. Dynamic color remains opt-in so the
 * product keeps its distinct Obsidian Coral identity by default.
 */
@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = RockTypography, content = content)
}
