package com.sayanthrock.githubrock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val RockBackground = Color(0xFF0D1117)
val RockSurface = Color(0xFF161B22)
val RockBlue = Color(0xFF2F81F7)
val RockGreen = Color(0xFF3FB950)
val RockOrange = Color(0xFFD29922)
val RockRed = Color(0xFFF85149)
val RockText = Color(0xFFF0F6FC)
val RockMuted = Color(0xFF8B949E)

private val DarkColors = darkColorScheme(
    primary = RockBlue,
    secondary = RockGreen,
    tertiary = RockOrange,
    background = RockBackground,
    surface = RockSurface,
    surfaceVariant = Color(0xFF21262D),
    onPrimary = Color.White,
    onBackground = RockText,
    onSurface = RockText,
    onSurfaceVariant = RockMuted,
    error = RockRed
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0969DA),
    secondary = Color(0xFF1A7F37),
    tertiary = Color(0xFF9A6700),
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAEFF4),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFF59636E),
    error = Color(0xFFCF222E)
)

/**
 * Applies the GitHub Rock Material theme and configures system bar appearance.
 *
 * @param darkTheme Whether to use a dark color scheme.
 * @param dynamicColor Whether to use the system dynamic color scheme when supported.
 * @param content The composable content to display with the theme.
 */
@Composable
fun GitHubRockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * Displays content over a full-screen radial gradient background based on the current theme.
 *
 * @param content The content to display inside the background container.
 */
@Composable
fun LiquidBackground(content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.18f),
                        colors.background,
                        colors.background
                    ),
                    radius = 1200f
                )
            ),
        content = content
    )
}

/**
 * Renders content in a rounded, bordered card with a translucent surface.
 *
 * @param modifier Modifier applied to the card.
 * @param content Content displayed inside the card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) { content() }
}
