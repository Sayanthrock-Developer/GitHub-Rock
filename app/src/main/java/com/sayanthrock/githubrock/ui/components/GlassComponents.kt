package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = if (onClick == null) 0.dp else 1.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Applies the standard Rock background used by every screen. */
@Composable
fun Modifier.rockBackground(): Modifier = this
