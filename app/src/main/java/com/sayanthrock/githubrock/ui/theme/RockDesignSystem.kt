package com.sayanthrock.githubrock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.RockShapePreset
import com.sayanthrock.githubrock.data.settings.RockSurfaceStyle

/**
 * Rock Adaptive is the single visual contract for GitHub Rock.
 *
 * Screens choose semantic roles; they do not choose their own surface treatment.
 * Theme, surface and shape preferences are resolved here so the whole app changes
 * together instead of accumulating screen-specific styling.
 */
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
    RockSurfaceTokens(
        opacity = 1f,
        borderAlpha = 0.38f,
        elevation = 2.dp,
        blur = 0.dp,
        contrast = 1f,
        backgroundDepth = 0.08f
    )
}

private fun resolvedSurfaceStyle(style: RockSurfaceStyle, dark: Boolean): RockSurfaceStyle =
    when (style) {
        RockSurfaceStyle.Adaptive -> if (dark) RockSurfaceStyle.Soft else RockSurfaceStyle.Solid
        else -> style
    }

/**
 * Resolves one semantic surface role into a stable token set.
 *
 * Blur is intentionally not part of the visual treatment anymore. The value is
 * kept at zero for compatibility with persisted preferences and existing callers.
 */
@Composable
fun rockSurfaceTokens(role: RockSurfaceRole): RockSurfaceTokens {
    val background = MaterialTheme.colorScheme.background
    val dark = (background.red * 0.2126f + background.green * 0.7152f + background.blue * 0.0722f) < 0.5f
    val style = resolvedSurfaceStyle(LocalRockSurfaceStyle.current, dark)

    val base = when (style) {
        RockSurfaceStyle.Solid -> when (role) {
            RockSurfaceRole.Background -> RockSurfaceTokens(1f, 0f, 0.dp, 0.dp, 1f, 0f)
            RockSurfaceRole.Card -> RockSurfaceTokens(1f, 0.32f, 0.dp, 0.dp, 1f, 0.02f)
            RockSurfaceRole.Elevated -> RockSurfaceTokens(1f, 0.38f, 3.dp, 0.dp, 1f, 0.03f)
            RockSurfaceRole.Interactive -> RockSurfaceTokens(1f, 0.42f, 1.dp, 0.dp, 1f, 0.04f)
            RockSurfaceRole.Selected -> RockSurfaceTokens(1f, 0.50f, 1.dp, 0.dp, 1f, 0.06f)
            RockSurfaceRole.Navigation -> RockSurfaceTokens(1f, 0.44f, 2.dp, 0.dp, 1f, 0.04f)
            RockSurfaceRole.Sheet -> RockSurfaceTokens(1f, 0.44f, 4.dp, 0.dp, 1f, 0.05f)
            RockSurfaceRole.Code -> RockSurfaceTokens(1f, 0.28f, 0.dp, 0.dp, 1f, 0.02f)
        }

        RockSurfaceStyle.Soft -> when (role) {
            RockSurfaceRole.Background -> RockSurfaceTokens(1f, 0f, 0.dp, 0.dp, 1f, 0.05f)
            RockSurfaceRole.Card -> RockSurfaceTokens(0.98f, 0.36f, 1.dp, 0.dp, 1f, 0.08f)
            RockSurfaceRole.Elevated -> RockSurfaceTokens(0.99f, 0.42f, 4.dp, 0.dp, 1f, 0.10f)
            RockSurfaceRole.Interactive -> RockSurfaceTokens(0.98f, 0.46f, 2.dp, 0.dp, 1f, 0.10f)
            RockSurfaceRole.Selected -> RockSurfaceTokens(0.99f, 0.52f, 2.dp, 0.dp, 1f, 0.12f)
            RockSurfaceRole.Navigation -> RockSurfaceTokens(0.97f, 0.46f, 3.dp, 0.dp, 1f, 0.10f)
            RockSurfaceRole.Sheet -> RockSurfaceTokens(0.99f, 0.48f, 5.dp, 0.dp, 1f, 0.12f)
            RockSurfaceRole.Code -> RockSurfaceTokens(0.98f, 0.30f, 0.dp, 0.dp, 1f, 0.06f)
        }

        RockSurfaceStyle.Glass -> when (role) {
            RockSurfaceRole.Background -> RockSurfaceTokens(1f, 0f, 0.dp, 0.dp, 1f, 0.08f)
            RockSurfaceRole.Card -> RockSurfaceTokens(0.90f, 0.44f, 1.dp, 0.dp, 1f, 0.12f)
            RockSurfaceRole.Elevated -> RockSurfaceTokens(0.94f, 0.50f, 4.dp, 0.dp, 1f, 0.15f)
            RockSurfaceRole.Interactive -> RockSurfaceTokens(0.92f, 0.52f, 2.dp, 0.dp, 1f, 0.14f)
            RockSurfaceRole.Selected -> RockSurfaceTokens(0.96f, 0.58f, 2.dp, 0.dp, 1f, 0.16f)
            RockSurfaceRole.Navigation -> RockSurfaceTokens(0.90f, 0.54f, 3.dp, 0.dp, 1f, 0.14f)
            RockSurfaceRole.Sheet -> RockSurfaceTokens(0.95f, 0.54f, 5.dp, 0.dp, 1f, 0.16f)
            RockSurfaceRole.Code -> RockSurfaceTokens(0.88f, 0.36f, 0.dp, 0.dp, 1f, 0.10f)
        }

        RockSurfaceStyle.Frosted -> when (role) {
            RockSurfaceRole.Background -> RockSurfaceTokens(1f, 0f, 0.dp, 0.dp, 1.02f, 0.10f)
            RockSurfaceRole.Card -> RockSurfaceTokens(0.94f, 0.48f, 2.dp, 0.dp, 1.02f, 0.14f)
            RockSurfaceRole.Elevated -> RockSurfaceTokens(0.97f, 0.56f, 5.dp, 0.dp, 1.02f, 0.18f)
            RockSurfaceRole.Interactive -> RockSurfaceTokens(0.95f, 0.58f, 2.dp, 0.dp, 1.02f, 0.16f)
            RockSurfaceRole.Selected -> RockSurfaceTokens(0.98f, 0.62f, 2.dp, 0.dp, 1.02f, 0.20f)
            RockSurfaceRole.Navigation -> RockSurfaceTokens(0.94f, 0.60f, 4.dp, 0.dp, 1.02f, 0.18f)
            RockSurfaceRole.Sheet -> RockSurfaceTokens(0.97f, 0.60f, 6.dp, 0.dp, 1.02f, 0.20f)
            RockSurfaceRole.Code -> RockSurfaceTokens(0.92f, 0.40f, 0.dp, 0.dp, 1.02f, 0.12f)
        }

        RockSurfaceStyle.Adaptive -> error("Adaptive must resolve before token selection")
    }

    val customization = LocalRockSurfaceCustomization.current
    return base.copy(
        opacity = (base.opacity * customization.opacity).coerceIn(0f, 1f),
        borderAlpha = (base.borderAlpha * customization.borderAlpha).coerceIn(0f, 1f),
        elevation = customization.elevation,
        // Legacy blur preferences are intentionally ignored.
        blur = 0.dp,
        contrast = customization.contrast,
        backgroundDepth = customization.backgroundDepth
    )
}
