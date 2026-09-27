package com.sayanthrock.githubrock.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GitHub Rock's semantic shape vocabulary.
 *
 * Components consume roles instead of inventing radii. The selected global
 * shape scale is applied consistently to controls, cards, sheets and navigation.
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
    const val Background = 1f
    const val Low = 0.92f
    const val Medium = 0.96f
    const val High = 0.98f
    const val Interactive = 0.96f
    const val Selected = 1f
    const val Border = 0.55f
}

/** One semantic surface role for every major UI layer. */
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
fun rockSurfaceBorder(role: RockSurfaceRole = RockSurfaceRole.Card): BorderStroke {
    val tokens = rockSurfaceTokens(role)
    return BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = tokens.borderAlpha)
    )
}

@Composable
fun rockContentColor(role: RockSurfaceRole): Color = when (role) {
    RockSurfaceRole.Selected -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSurface
}
