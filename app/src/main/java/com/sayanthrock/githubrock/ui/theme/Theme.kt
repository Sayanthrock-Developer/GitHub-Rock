package com.sayanthrock.githubrock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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
    secondary = RockPurple,
    tertiary = RockGreen,
    background = RockBackground,
    surface = RockSurface,
    surfaceVariant = Color(0xFF21262D),
    outline = RockBorder,
    error = RockRed,
    onPrimary = Color.White,
    onBackground = RockText,
    onSurface = RockText,
    onSurfaceVariant = RockMuted
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0969DA),
    secondary = Color(0xFF8250DF),
    tertiary = Color(0xFF1A7F37),
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAEEF2),
    outline = Color(0xFFD0D7DE),
    error = Color(0xFFCF222E),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFF59636E)
)

@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = RockTypography, content = content)
}

