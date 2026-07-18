package com.sayanthrock.githubrock.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.ui.theme.LocalLoadingStyle
import com.sayanthrock.githubrock.ui.theme.LocalReduceMotion

/** Consistent loader used by production screens and the customization preview. */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    compact: Boolean = false,
    style: LoadingStyle = LocalLoadingStyle.current,
    reduceMotion: Boolean = LocalReduceMotion.current
) {
    val normalizedProgress = progress?.coerceIn(0f, 1f)
    val height = if (compact) 32.dp else 44.dp

    Box(
        modifier = modifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LoadingStyle.Spinner -> {
                if (reduceMotion || normalizedProgress != null) {
                    CircularProgressIndicator(
                        progress = { normalizedProgress ?: .72f },
                        modifier = Modifier.size(if (compact) 22.dp else 30.dp),
                        strokeWidth = if (compact) 2.dp else 3.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (compact) 22.dp else 30.dp),
                        strokeWidth = if (compact) 2.dp else 3.dp
                    )
                }
            }

            LoadingStyle.Linear -> {
                if (reduceMotion || normalizedProgress != null) {
                    LinearProgressIndicator(
                        progress = { normalizedProgress ?: .45f },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            LoadingStyle.Pulse -> {
                if (reduceMotion) {
                    PulseDot(compact = compact, scale = 1f)
                } else {
                    val transition = rememberInfiniteTransition(label = "app-loading-pulse")
                    val pulse by transition.animateFloat(
                        initialValue = .72f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 700),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "app-loading-pulse-scale"
                    )
                    PulseDot(compact = compact, scale = pulse)
                }
            }
        }
    }
}

@Composable
private fun PulseDot(compact: Boolean, scale: Float) {
    Surface(
        modifier = Modifier
            .size(if (compact) 20.dp else 28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary
    ) {}
}
