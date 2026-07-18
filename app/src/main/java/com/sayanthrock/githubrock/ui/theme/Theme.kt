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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.AccentColor
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.CodeColorStyle
import com.sayanthrock.githubrock.data.settings.DisplaySize
import com.sayanthrock.githubrock.data.settings.FontSize
import com.sayanthrock.githubrock.data.settings.FontWeightStyle
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.data.settings.ThemeStyle

val LocalRemoteImagesEnabled = staticCompositionLocalOf { true }
val LocalLoadingStyle = staticCompositionLocalOf { LoadingStyle.Spinner }
val LocalCodeColorStyle = staticCompositionLocalOf { CodeColorStyle.Classic }

data class CodeColors(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val type: Color,
    val property: Color
)

val LocalCodeColors = staticCompositionLocalOf {
    CodeColors(
        keyword = Color(0xFF79B8FF),
        string = Color(0xFF85E89D),
        comment = Color(0xFF8B949E),
        number = Color(0xFFFFAB70),
        type = Color(0xFFBC8CFF),
        property = Color(0xFFFF7B72)
    )
}

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
    AccentColor.Emerald -> AccentPalette(Color(0xFF56D364), Color(0xFF173D25), Color(0xFF1A7F37), Color(0xFFDAFBE1))
    AccentColor.Rose -> AccentPalette(Color(0xFFFF8FB3), Color(0xFF5A2338), Color(0xFFBF3989), Color(0xFFFFE5F1))
    AccentColor.Coral -> AccentPalette(Color(0xFFFF9B8F), Color(0xFF5B2923), Color(0xFFB42318), Color(0xFFFFE2DE))
    AccentColor.Amber -> AccentPalette(Color(0xFFF2CC60), Color(0xFF4E3D0D), Color(0xFF7A5D00), Color(0xFFFFEDB0))
    AccentColor.Orange -> AccentPalette(Color(0xFFFFA657), Color(0xFF553018), Color(0xFFBC4C00), Color(0xFFFFE2C6))
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
    ThemeStyle.Midnight -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    ThemeStyle.Aurora -> Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp)
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
    ThemeStyle.Midnight -> copy(
        background = if (darkTheme) Color(0xFF070B14) else Color(0xFFF4F7FC),
        surface = if (darkTheme) Color(0xFF0D1422) else Color(0xFFFFFFFF),
        surfaceContainer = if (darkTheme) Color(0xFF121C2D) else Color(0xFFEAF0FA),
        surfaceContainerHigh = if (darkTheme) Color(0xFF19263A) else Color(0xFFDDE7F5),
        outlineVariant = primary.copy(alpha = .28f)
    )
    ThemeStyle.Aurora -> copy(
        background = if (darkTheme) Color(0xFF07110F) else Color(0xFFF4FBF8),
        surface = if (darkTheme) Color(0xFF0D1A17) else Color.White,
        surfaceContainer = if (darkTheme) Color(0xFF13231F) else Color(0xFFE8F6F0),
        surfaceContainerHigh = if (darkTheme) Color(0xFF1A2E29) else Color(0xFFD9EEE6),
        outlineVariant = tertiary.copy(alpha = .30f)
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

private fun codeColors(style: CodeColorStyle, darkTheme: Boolean): CodeColors = when (style) {
    CodeColorStyle.Classic -> CodeColors(
        keyword = if (darkTheme) Color(0xFF79B8FF) else Color(0xFF0550AE),
        string = if (darkTheme) Color(0xFF85E89D) else Color(0xFF116329),
        comment = if (darkTheme) Color(0xFF8B949E) else Color(0xFF6E7781),
        number = if (darkTheme) Color(0xFFFFAB70) else Color(0xFF953800),
        type = if (darkTheme) Color(0xFFBC8CFF) else Color(0xFF8250DF),
        property = if (darkTheme) Color(0xFFFF7B72) else Color(0xFFCF222E)
    )
    CodeColorStyle.Ocean -> CodeColors(
        keyword = Color(0xFF58A6FF),
        string = Color(0xFF7EE787),
        comment = Color(0xFF8B949E),
        number = Color(0xFF79C0FF),
        type = Color(0xFFD2A8FF),
        property = Color(0xFF39C5CF)
    )
    CodeColorStyle.Sunset -> CodeColors(
        keyword = Color(0xFFFF7B72),
        string = Color(0xFFF2CC60),
        comment = Color(0xFF9DA7B3),
        number = Color(0xFFFFA657),
        type = Color(0xFFD2A8FF),
        property = Color(0xFFFF8FB3)
    )
    CodeColorStyle.Monochrome -> CodeColors(
        keyword = if (darkTheme) Color.White else Color.Black,
        string = if (darkTheme) Color(0xFFD0D7DE) else Color(0xFF24292F),
        comment = if (darkTheme) Color(0xFF8C959F) else Color(0xFF6E7781),
        number = if (darkTheme) Color(0xFFE6EDF3) else Color(0xFF24292F),
        type = if (darkTheme) Color.White else Color.Black,
        property = if (darkTheme) Color(0xFFC9D1D9) else Color(0xFF24292F)
    )
}

private fun ColorScheme.applyCodeStyle(colors: CodeColors): ColorScheme = copy(
    secondary = colors.number,
    tertiary = colors.string
)

private fun DisplaySize.scale(): Float = when (this) {
    DisplaySize.Small -> .90f
    DisplaySize.Standard -> 1f
    DisplaySize.Large -> 1.12f
}

private fun FontSize.scale(): Float = when (this) {
    FontSize.Small -> .90f
    FontSize.Default -> 1f
    FontSize.Large -> 1.16f
}

/** Material 3 visual system with selectable personality, density, typography, and code palette. */
@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    trueBlack: Boolean = false,
    accentColor: AccentColor = AccentColor.Cyan,
    themeStyle: ThemeStyle = ThemeStyle.Clean,
    displaySize: DisplaySize = DisplaySize.Standard,
    fontSize: FontSize = FontSize.Default,
    fontWeight: FontWeightStyle = FontWeightStyle.Default,
    fontFamily: AppFontFamily = AppFontFamily.SystemSans,
    loadingStyle: LoadingStyle = LoadingStyle.Spinner,
    codeColorStyle: CodeColorStyle = CodeColorStyle.Classic,
    showImages: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val codeColors = codeColors(codeColorStyle, darkTheme)
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> darkColors(accentColor)
        else -> lightColors(accentColor)
    }.applyStyle(themeStyle, darkTheme)
        .applyCodeStyle(codeColors)
        .let { scheme ->
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

    val scaledDensity = Density(
        density = baseDensity.density * displaySize.scale(),
        fontScale = baseDensity.fontScale * fontSize.scale()
    )

    CompositionLocalProvider(
        LocalRemoteImagesEnabled provides showImages,
        LocalLoadingStyle provides loadingStyle,
        LocalCodeColorStyle provides codeColorStyle,
        LocalCodeColors provides codeColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = rockTypography(fontFamily, fontWeight),
            shapes = shapesFor(themeStyle),
            content = content
        )
    }
}
