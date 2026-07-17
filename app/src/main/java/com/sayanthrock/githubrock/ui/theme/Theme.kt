package com.sayanthrock.githubrock.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.ThemeStyle

val LocalRemoteImagesEnabled = staticCompositionLocalOf { true }

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

private fun shapesFor(style: ThemeStyle): Shapes = when (style) {
    ThemeStyle.Clean -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(9.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )
    ThemeStyle.LiquidGlass -> Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(34.dp)
    )
    ThemeStyle.Studio -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(7.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(13.dp),
        extraLarge = RoundedCornerShape(18.dp)
    )
    ThemeStyle.HighContrast -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp)
    )
}

private fun ColorScheme.applyStyle(style: ThemeStyle, darkTheme: Boolean): ColorScheme = when (style) {
    ThemeStyle.Clean -> this
    ThemeStyle.LiquidGlass -> copy(
        surface = surface.copy(alpha = .94f),
        surfaceContainer = surfaceContainer.copy(alpha = .90f),
        surfaceContainerHigh = surfaceContainerHigh.copy(alpha = .92f),
        surfaceVariant = surfaceVariant.copy(alpha = .88f),
        outlineVariant = primary.copy(alpha = .20f)
    )
    ThemeStyle.Studio -> copy(
        background = if (darkTheme) Color(0xFF0B0E12) else Color(0xFFF6F7F9),
        surface = if (darkTheme) Color(0xFF11161D) else Color.White,
        surfaceContainer = if (darkTheme) Color(0xFF151B23) else Color(0xFFF0F2F5),
        surfaceContainerHigh = if (darkTheme) Color(0xFF1B222C) else Color(0xFFE7EBF0),
        outlineVariant = if (darkTheme) Color(0xFF313A46) else Color(0xFFD4D9E0)
    )
    ThemeStyle.HighContrast -> copy(
        background = if (darkTheme) Color.Black else Color.White,
        surface = if (darkTheme) Color(0xFF050505) else Color.White,
        surfaceContainer = if (darkTheme) Color(0xFF0D0D0D) else Color(0xFFF4F4F4),
        surfaceContainerHigh = if (darkTheme) Color(0xFF161616) else Color(0xFFEAEAEA),
        outline = if (darkTheme) Color.White else Color.Black,
        outlineVariant = if (darkTheme) Color(0xFFBDBDBD) else Color(0xFF2B2B2B),
        onBackground = if (darkTheme) Color.White else Color.Black,
        onSurface = if (darkTheme) Color.White else Color.Black,
        onSurfaceVariant = if (darkTheme) Color(0xFFE3E3E3) else Color(0xFF222222)
    )
}

/** Material 3 visual system with selectable personality, accent, and image loading policy. */
@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    trueBlack: Boolean = false,
    accentColor: AccentColor = AccentColor.Cyan,
    themeStyle: ThemeStyle = ThemeStyle.Clean,
    showImages: Boolean = true,
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
    }.applyStyle(themeStyle, darkTheme).let { scheme ->
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

    CompositionLocalProvider(LocalRemoteImagesEnabled provides showImages) {
        MaterialTheme(
            colorScheme = colors,
            typography = RockTypography,
            shapes = shapesFor(themeStyle),
            content = content
        )
    }
}
