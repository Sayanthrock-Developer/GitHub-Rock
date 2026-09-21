package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import com.sayanthrock.githubrock.ui.theme.LocalRockDesignTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * GitHub Rock's primary content surface.
 *
 * The legacy GlassCard name is retained for source compatibility, but the
 * visual treatment is now a clean tonal surface rather than a glass effect:
 * large radius, restrained elevation, no heavy outlines, and Material dynamic
 * color support through the active theme.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }

    val design = LocalRockDesignTokens.current
    val densityPadding = (18f * design.cardDensity).dp
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = design.surfaceOpacity.coerceIn(.72f, 1f)),
        tonalElevation = design.elevation.dp,
        shadowElevation = if (onClick == null) 0.dp else (design.elevation * .5f).dp,
        border = if (design.borderWidth > 0f) BorderStroke(design.borderWidth.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = (design.borderWidth / 3f).coerceIn(.08f, .45f))) else null
    ) {
        Box(modifier = Modifier.padding(if (contentPadding == PaddingValues(18.dp)) PaddingValues(densityPadding) else contentPadding), content = content)
    }
}

/** Applies the standard Rock background used by every screen. */
@Composable
fun Modifier.rockBackground(): Modifier = this
