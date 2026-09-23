package com.sayanthrock.githubrock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
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
    val loadingSemantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalizedProgress?.let {
            ProgressBarRangeInfo(current = it, range = 0f..1f)
        } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalizedProgress?.let {
            "${(it * 100).toInt()} percent loaded"
        } ?: "Loading"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(loadingSemantics),
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
                    PulseDot(compact = compact, scale = 1f, alpha = 1f)
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
                    val alpha by transition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 700),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "app-loading-pulse-alpha"
                    )
                    PulseDot(compact = compact, scale = pulse, alpha = alpha)
                }
            }

            LoadingStyle.Skeleton -> {
                SkeletonLoader(compact = compact, reduceMotion = reduceMotion)
            }

            LoadingStyle.Liquid -> {
                LiquidLoader(compact = compact, reduceMotion = reduceMotion)
            }

            LoadingStyle.Orbit -> {
                OrbitLoader(compact = compact, reduceMotion = reduceMotion)
            }

            LoadingStyle.Shimmer -> {
                ShimmerLoader(compact = compact, reduceMotion = reduceMotion)
            }

            LoadingStyle.Morph -> {
                MorphLoader(compact = compact, reduceMotion = reduceMotion)
            }

            LoadingStyle.RockRing -> {
                RockRingLoader(compact = compact, reduceMotion = reduceMotion)
            }
        }
    }
}


/**
 * Repository-specific loading surface. It keeps the repository context visible while
 * GitHub operations are in flight and never invents progress.
 */
@Composable
fun RepositoryOperationLoader(
    modifier: Modifier = Modifier,
    label: String = "Loading repository",
    compact: Boolean = false,
    reduceMotion: Boolean = LocalReduceMotion.current
) {
    val size = if (compact) 44.dp else 56.dp
    val iconSize = if (compact) 20.dp else 26.dp
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .42f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 10.dp else 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(size),
                    shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                ) {}
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (!reduceMotion) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size - 2.dp),
                        strokeWidth = if (compact) 2.dp else 2.5.dp
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                androidx.compose.material3.Text(
                    label,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                androidx.compose.material3.Text(
                    "Syncing with GitHub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RepositoryOperationSkeleton(modifier: Modifier = Modifier, reduceMotion: Boolean = LocalReduceMotion.current) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .32f)
        )
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLoadingIndicator(
                modifier = Modifier.fillMaxWidth(),
                compact = true,
                style = LoadingStyle.Skeleton,
                reduceMotion = reduceMotion
            )
            AppLoadingIndicator(
                modifier = Modifier.fillMaxWidth(),
                compact = true,
                style = LoadingStyle.Skeleton,
                reduceMotion = reduceMotion
            )
            AppLoadingIndicator(
                modifier = Modifier.fillMaxWidth(0.62f),
                compact = true,
                style = LoadingStyle.Skeleton,
                reduceMotion = reduceMotion
            )
        }
    }
}

/** Compatibility overload for callers using style and reduce-motion as the leading arguments. */
@Composable
fun AppLoadingIndicator(
    style: LoadingStyle,
    reduceMotion: Boolean
) {
    AppLoadingIndicator(
        modifier = Modifier,
        progress = null,
        compact = false,
        style = style,
        reduceMotion = reduceMotion
    )
}

@Composable
private fun SkeletonLoader(compact: Boolean, reduceMotion: Boolean) {
    if (reduceMotion) {
        SkeletonBars(shimmerOffset = 0f, compact = compact)
        return
    }

    val transition = rememberInfiniteTransition(label = "app-loading-skeleton")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "app-loading-skeleton-offset"
    )
    SkeletonBars(shimmerOffset = offset, compact = compact)
}

@Composable
private fun SkeletonBars(shimmerOffset: Float, compact: Boolean) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 12.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val phase = ((shimmerOffset + index * .42f).coerceIn(0f, 1f))
            val alpha = .72f + (.28f * phase)
            Box(
                modifier = Modifier
                    .weight(if (index == 0) 1.3f else 1f)
                    .height(if (compact) 8.dp else 10.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(
                        color = if (phase > .35f && phase < .8f) highlight else base,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun LiquidLoader(compact: Boolean, reduceMotion: Boolean) {
    if (reduceMotion) {
        LiquidBar(offset = 0f, compact = compact)
        return
    }
    val transition = rememberInfiniteTransition(label = "app-loading-liquid")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "app-loading-liquid-offset"
    )
    LiquidBar(offset = offset, compact = compact)
}

@Composable
private fun LiquidBar(offset: Float, compact: Boolean) {
    Box(
        modifier = Modifier
            .size(width = if (compact) 58.dp else 82.dp, height = if (compact) 12.dp else 16.dp)
            .graphicsLayer { translationX = offset * if (compact) 8f else 14f; scaleX = 0.88f + ((offset + 1f) * .06f) }
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
    )
}

@Composable
private fun OrbitLoader(compact: Boolean, reduceMotion: Boolean) {
    val size = if (compact) 22.dp else 30.dp
    if (reduceMotion) {
        Box(Modifier.size(size), contentAlignment = Alignment.TopCenter) {
            Surface(Modifier.size(if (compact) 6.dp else 8.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary) {}
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "app-loading-orbit")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Restart),
        label = "app-loading-orbit-rotation"
    )
    Box(Modifier.size(size).graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.TopCenter) {
        Surface(Modifier.size(if (compact) 6.dp else 8.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary) {}
    }
}

@Composable
private fun ShimmerLoader(compact: Boolean, reduceMotion: Boolean) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    if (reduceMotion) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 12.dp else 8.dp)
                .height(if (compact) 8.dp else 10.dp)
                .background(base, RoundedCornerShape(50))
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "app-loading-shimmer")
    val offset by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1050), repeatMode = RepeatMode.Restart),
        label = "app-loading-shimmer-offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(offset * 500f, 0f),
        end = androidx.compose.ui.geometry.Offset((offset + .8f) * 500f, 0f)
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 12.dp else 8.dp)
            .height(if (compact) 8.dp else 10.dp)
            .background(brush, RoundedCornerShape(50))
    )
}

@Composable
private fun MorphLoader(compact: Boolean, reduceMotion: Boolean) {
    val baseSize = if (compact) 18.dp else 24.dp
    if (reduceMotion) {
        Surface(Modifier.size(baseSize), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary) {}
        return
    }
    val transition = rememberInfiniteTransition(label = "app-loading-morph")
    val scale by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "app-loading-morph-scale"
    )
    val rotation by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "app-loading-morph-rotation"
    )
    Surface(
        Modifier.size(baseSize).graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        },
        shape = RoundedCornerShape(38),
        color = MaterialTheme.colorScheme.primary
    ) {}
}

@Composable
private fun RockRingLoader(compact: Boolean, reduceMotion: Boolean) {
    val size = if (compact) 32.dp else 58.dp
    val stroke = if (compact) 4.dp else 6.dp
    if (reduceMotion) {
        Canvas(
            modifier = Modifier.size(size),
            onDraw = {
                drawArc(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .18f),
                    startAngle = -90f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke.toPx(),
                        cap = StrokeCap.Round
                    )
                )
                drawArc(
                    color = MaterialTheme.colorScheme.primary,
                    startAngle = -90f,
                    sweepAngle = 95f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "app-loading-rock-ring")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Restart
        ),
        label = "app-loading-rock-ring-rotation"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        drawArc(
            color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
            startAngle = -90f,
            sweepAngle = 300f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke.toPx(),
                cap = StrokeCap.Round
            )
        )
        drawArc(
            color = MaterialTheme.colorScheme.primary,
            startAngle = -90f,
            sweepAngle = 95f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun PulseDot(compact: Boolean, scale: Float, alpha: Float) {
    Surface(
        modifier = Modifier
            .size(if (compact) 20.dp else 28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary
    ) {}
}
