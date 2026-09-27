package com.sayanthrock.githubrock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class RockSurfaceStyle { Solid, Soft, Glass, Frosted, Adaptive }

enum class RockShapePreset(val scale: Float) {
    Sharp(0.08f),
    Compact(0.28f),
    Rounded(0.50f),
    Soft(0.68f),
    ExtraRounded(0.82f),
    Pill(1f)
}

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

private fun resolvedSurfaceStyle(style: RockSurfaceStyle, dark: Boolean): RockSurfaceStyle =
    if (style != RockSurfaceStyle.Adaptive) style else if (dark) RockSurfaceStyle.Soft else RockSurfaceStyle.Solid

@Composable
fun rockSurfaceTokens(role: RockSurfaceRole): RockSurfaceTokens {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val style = resolvedSurfaceStyle(LocalRockSurfaceStyle.current, dark)
    return when (style) {
        RockSurfaceStyle.Solid -> RockSurfaceTokens(1f, 0.30f, 0.dp, 0.dp, 1f, 0f)
        RockSurfaceStyle.Soft -> RockSurfaceTokens(0.96f, 0.38f, if (role == RockSurfaceRole.Elevated) 2.dp else 1.dp, 0.dp, 1f, 0.08f)
        RockSurfaceStyle.Glass -> RockSurfaceTokens(0.84f, 0.48f, if (role == RockSurfaceRole.Elevated) 3.dp else 1.dp, 10.dp, 1f, 0.12f)
        RockSurfaceStyle.Frosted -> RockSurfaceTokens(0.76f, 0.52f, if (role == RockSurfaceRole.Elevated) 4.dp else 2.dp, 18.dp, 1.02f, 0.18f)
        RockSurfaceStyle.Adaptive -> error("Adaptive must resolve before token selection")
    }
}
