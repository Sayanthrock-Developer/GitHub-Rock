package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import com.sayanthrock.githubrock.ui.theme.RockShapes
import com.sayanthrock.githubrock.ui.theme.rockSurfaceBorder
import com.sayanthrock.githubrock.ui.theme.rockSurfaceColor
import com.sayanthrock.githubrock.ui.theme.RockSurfaceRole
import com.sayanthrock.githubrock.ui.theme.rockSurfaceTokens
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
    val isTv = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    var focused = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .onFocusChanged { focused.value = it.isFocused }
            .focusable()
            .clickable(role = Role.Button, onClick = onClick)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RockShapes.Card),
        color = rockSurfaceColor(RockSurfaceRole.Card),
        border = if (isTv && onClick != null && focused.value) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            rockSurfaceBorder()
        },
        tonalElevation = rockSurfaceTokens(RockSurfaceRole.Card).elevation,
        shadowElevation = if (onClick == null) 0.dp else rockSurfaceTokens(RockSurfaceRole.Card).elevation / 2
    ) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Applies the standard Rock background used by every screen. */
@Composable
fun Modifier.rockBackground(): Modifier = this
