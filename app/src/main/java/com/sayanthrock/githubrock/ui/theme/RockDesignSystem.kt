package com.sayanthrock.githubrock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import com.sayanthrock.githubrock.data.settings.RockSurfaceStyle
import com.sayanthrock.githubrock.data.settings.RockShapePreset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp

data class RockSurfaceTokens(
    val opacity: Float,
    val borderAlpha: Float,
    val elevation: Dp,
    val blur: Dp,
    val contrast: Float,
    val backgroundDepth: Float
)

val LocalRockSurfaceStyle = staticCompositionLocalOf { RockSurfaceStyle.Adaptive }
val LocalRockShapeScale = staticCompositionLocalOf { RockShapePreset.Rounded.scale }
val LocalRockSurfaceCustomization = staticCompositionLocalOf {
    RockSurfaceTokens(1f, 0.38f, 2.dp, 0.dp, 1f, 0.08f)
}

private fun resolvedSurfaceStyle(style: RockSurfaceStyle, dark: Boolean): RockSurfaceStyle =
    if (style != RockSurfaceStyle.Adaptive) style else if (dark) RockSurfaceStyle.Soft else RockSurfaceStyle.Solid

@Composable
fun rockSurfaceTokens(role: RockSurfaceRole): RockSurfaceTokens {
    val background = MaterialTheme.colorScheme.background
    val dark = (background.red * 0.2126f + background.green * 0.7152f + background.blue * 0.0722f) < 0.5f
    val style = resolvedSurfaceStyle(LocalRockSurfaceStyle.current, dark)
    val baseTokens = when (style) {
        RockSurfaceStyle.Solid -> RockSurfaceTokens(1f, 0.30f, 0.dp, 0.dp, 1f, 0f)
        RockSurfaceStyle.Soft -> RockSurfaceTokens(0.96f, 0.38f, if (role == RockSurfaceRole.Elevated) 2.dp else 1.dp, 0.dp, 1f, 0.08f)
        RockSurfaceStyle.Glass -> RockSurfaceTokens(0.84f, 0.48f, if (role == RockSurfaceRole.Elevated) 3.dp else 1.dp, 10.dp, 1f, 0.12f)
        RockSurfaceStyle.Frosted -> RockSurfaceTokens(0.76f, 0.52f, if (role == RockSurfaceRole.Elevated) 4.dp else 2.dp, 18.dp, 1.02f, 0.18f)
        RockSurfaceStyle.Adaptive -> error("Adaptive must resolve before token selection")
    }
    val customization = LocalRockSurfaceCustomization.current
    return baseTokens.copy(
        opacity = (baseTokens.opacity * customization.opacity).coerceIn(0f, 1f),
        borderAlpha = (baseTokens.borderAlpha * customization.borderAlpha).coerceIn(0f, 1f),
        elevation = customization.elevation,
        blur = customization.blur,
        contrast = customization.contrast,
        backgroundDepth = customization.backgroundDepth
    )
}
