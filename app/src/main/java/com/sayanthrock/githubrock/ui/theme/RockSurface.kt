package com.sayanthrock.githubrock.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GitHub Rock's single surface/shape vocabulary.
 *
 * Components should consume these semantic roles instead of inventing their own
 * corner radii, transparency values, or theme-breaking surface colors.
 */
object RockShapes {
    @Composable
    private fun radius(max: Float): androidx.compose.ui.unit.Dp =
        (max * LocalRockShapeScale.current.coerceIn(0f, 1f)).dp

    val Control: androidx.compose.ui.unit.Dp @Composable get() = radius(16f)
    val SmallCard: androidx.compose.ui.unit.Dp @Composable get() = radius(20f)
    val Card: androidx.compose.ui.unit.Dp @Composable get() = radius(28f)
    val LargeCard: androidx.compose.ui.unit.Dp @Composable get() = radius(34f)
    val Sheet: androidx.compose.ui.unit.Dp @Composable get() = radius(40f)
    val Navigation: androidx.compose.ui.unit.Dp @Composable get() = radius(36f)
    val Pill: androidx.compose.ui.unit.Dp @Composable get() = radius(999f)
}

object RockSurfaceAlpha {
    const val Background = 0.96f
    const val Low = 0.72f
    const val Medium = 0.84f
    const val High = 0.94f
    const val Interactive = 0.90f
    const val Selected = 0.96f
    const val Border = 0.55f
}

/** Semantic surface roles used throughout the app. */
sealed interface RockSurfaceRole {
    data object Background : RockSurfaceRole
    data object Card : RockSurfaceRole
    data object Elevated : RockSurfaceRole
    data object Interactive : RockSurfaceRole
    data object Selected : RockSurfaceRole
    data object Navigation : RockSurfaceRole
    data object Sheet : RockSurfaceRole
    data object Code : RockSurfaceRole
}

@Composable
fun rockSurfaceColor(role: RockSurfaceRole): Color {
    val tokens = rockSurfaceTokens(role)
    val base = when (role) {
        RockSurfaceRole.Background -> MaterialTheme.colorScheme.background
        RockSurfaceRole.Card -> MaterialTheme.colorScheme.surface
        RockSurfaceRole.Elevated -> MaterialTheme.colorScheme.surfaceContainerHigh
        RockSurfaceRole.Interactive -> MaterialTheme.colorScheme.surfaceContainerHighest
        RockSurfaceRole.Selected -> MaterialTheme.colorScheme.primaryContainer
        RockSurfaceRole.Navigation -> MaterialTheme.colorScheme.surfaceContainerHigh
        RockSurfaceRole.Sheet -> MaterialTheme.colorScheme.surfaceContainer
        RockSurfaceRole.Code -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    return base.copy(alpha = tokens.opacity)
}

@Composable
fun rockSurfaceBorder(): BorderStroke {
    val tokens = rockSurfaceTokens(RockSurfaceRole.Card)
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = tokens.borderAlpha))
}

@Composable
fun rockContentColor(role: RockSurfaceRole): Color = when (role) {
    RockSurfaceRole.Selected -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSurface
}
