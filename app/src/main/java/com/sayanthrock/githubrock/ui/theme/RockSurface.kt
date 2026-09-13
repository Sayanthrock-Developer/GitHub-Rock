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
    val Control = 14.dp
    val SmallCard = 18.dp
    val Card = 24.dp
    val LargeCard = 28.dp
    val Sheet = 32.dp
    val Navigation = 28.dp
    val Pill = 999.dp
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
fun rockSurfaceColor(role: RockSurfaceRole): Color = when (role) {
    RockSurfaceRole.Background -> MaterialTheme.colorScheme.background.copy(alpha = RockSurfaceAlpha.Background)
    RockSurfaceRole.Card -> MaterialTheme.colorScheme.surface.copy(alpha = RockSurfaceAlpha.Low)
    RockSurfaceRole.Elevated -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = RockSurfaceAlpha.Medium)
    RockSurfaceRole.Interactive -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = RockSurfaceAlpha.Interactive)
    RockSurfaceRole.Selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = RockSurfaceAlpha.Selected)
    RockSurfaceRole.Navigation -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = RockSurfaceAlpha.Medium)
    RockSurfaceRole.Sheet -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = RockSurfaceAlpha.High)
    RockSurfaceRole.Code -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = RockSurfaceAlpha.High)
}

@Composable
fun rockSurfaceBorder(): BorderStroke = BorderStroke(
    1.dp,
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = RockSurfaceAlpha.Border)
)

@Composable
fun rockContentColor(role: RockSurfaceRole): Color = when (role) {
    RockSurfaceRole.Selected -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSurface
}
