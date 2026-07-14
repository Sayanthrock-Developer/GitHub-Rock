package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Displays a full-width glass-styled card with optional click handling.
 *
 * @param contentPadding The padding applied around the card content.
 * @param onClick The callback invoked when the card is clicked, or `null` to disable clicking.
 * @param content The composable content displayed inside the card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(interactionModifier)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)
                ),
                shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
     * Applies the theme background color with a vertical primary-to-secondary gradient overlay.
     *
     * @return A modifier with the rock background styling applied.
     */
    @Composable
fun Modifier.rockBackground(): Modifier = this
    .background(MaterialTheme.colorScheme.background)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                Color.Transparent,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
            )
        )
    )
