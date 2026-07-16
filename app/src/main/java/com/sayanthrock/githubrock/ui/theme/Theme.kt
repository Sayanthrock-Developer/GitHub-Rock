package com.sayanthrock.githubrock.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.AccentColor

private data class AccentPalette(
    val dark: Color,
    val darkContainer: Color,
    val light: Color,
    val lightContainer: Color
)

private fun AccentColor.palette(): AccentPalette = when (this) {
    AccentColor.Cyan -> AccentPalette(Color(0xFF52D3DC), Color(0xFF143F43), Color(0xFF007A83), Color(0xFFB7EEF1))
    AccentColor.Blue -> AccentPalette(Color(0xFF79B8FF), Color(0xFF173B63), Color(0xFF0969DA), Color(0xFFDDF4FF))
    AccentColor.Violet -> AccentPalette(Color(0xFFBC8CFF), Color(0xFF3C2361), Color(0xFF8250DF), Color(0xFFEFE3FF))
    AccentColor.Coral -> AccentPalette(Color(0xFFFF9B8F), Color(0xFF5B2923), Color(0xFFB42318), Color(0xFFFFE2DE))
    AccentColor.Amber -> AccentPalette(Color(0xFFF2CC60), Color(0xFF4E3D0D), Color(0xFF7A5D00), Color(0xFFFFEDB0))
}

private fun darkColors(accentColor: AccentColor) = accentColor.palette().let { accent ->
    darkColorScheme(
        primary = accent.dark,
        onPrimary = Color(0xFF071012),
        primaryContainer = accent.darkContainer,
        onPrimaryContainer = accent.dark,
        secondary = Color(0xFFB6C2CF),
        onSecondary = Color(0xFF1B242D),
        tertiary = RockGreen,
        background = RockDarkBackground,
        surface = RockDarkSurface,
        surfaceVariant = RockDarkSurfaceHigh,
        surfaceContainerLowest = RockDarkBackground,
        surfaceContainerLow = Color(0xFF121820),
        surfaceContainer = RockDarkSurface,
        surfaceContainerHigh = RockDarkSurfaceHigh,
        surfaceContainerHighest = Color(0xFF292F36),
        outline = RockDarkBorder,
        outlineVariant = Color(0xFF252B33),
        error = RockRed,
        onBackground = RockDarkText,
        onSurface = RockDarkText,
        onSurfaceVariant = RockDarkMuted
    )
}

private fun lightColors(accentColor: AccentColor) = accentColor.palette().let { accent ->
    lightColorScheme(
        primary = accent.light,
        onPrimary = Color.White,
        primaryContainer = accent.lightContainer,
        onPrimaryContainer = Color(0xFF102A2D),
        secondary = Color(0xFF59636E),
        onSecondary = Color.White,
        tertiary = RockLightGreen,
        background = RockLightBackground,
        surface = RockLightSurface,
        surfaceVariant = RockLightSurfaceHigh,
        surfaceContainerLowest = RockLightSurface,
        surfaceContainerLow = Color(0xFFF0F3F6),
        surfaceContainer = RockLightSurface,
        surfaceContainerHigh = RockLightSurfaceHigh,
        surfaceContainerHighest = Color(0xFFDDE2E7),
        outline = RockLightBorder,
        outlineVariant = Color(0xFFE1E5EA),
        error = RockLightRed,
        onBackground = RockLightText,
        onSurface = RockLightText,
        onSurfaceVariant = RockLightMuted
    )
}

private val RockShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Clean Material 3 visual system with restrained surfaces and one selectable accent.
 */
@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    trueBlack: Boolean = false,
    accentColor: AccentColor = AccentColor.Cyan,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> darkColors(accentColor)
        else -> lightColors(accentColor)
    }.let { scheme ->
        if (darkTheme && trueBlack) {
            scheme.copy(
                background = Color.Black,
                surface = Color(0xFF080A0D),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF06080A),
                surfaceContainer = Color(0xFF0A0D10)
            )
        } else {
            scheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = RockTypography,
        shapes = RockShapes,
        content = content
    )
}
