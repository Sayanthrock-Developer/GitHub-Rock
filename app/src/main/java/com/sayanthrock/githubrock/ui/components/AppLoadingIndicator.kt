package com.sayanthrock.githubrock.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.data.settings.LoadingStyle
import com.sayanthrock.githubrock.ui.theme.LocalLoadingStyle
import com.sayanthrock.githubrock.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val PentagonAngles = List(5) { index -> -PI / 2.0 + index * 2.0 * PI / 5.0 }

@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    compact: Boolean = false,
    style: LoadingStyle = LocalLoadingStyle.current,
    reduceMotion: Boolean = LocalReduceMotion.current
) {
    val normalized = progress?.coerceIn(0f, 1f)
    val indicatorSize = if (compact) 28.dp else 44.dp
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalized?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalized?.let { "${(it * 100).toInt()} percent loaded" } ?: "Loading"
        contentDescription = normalized?.let { "Loading progress ${(it * 100).toInt()} percent" } ?: "Loading"
    }
    Box(modifier = modifier.height(if (compact) 36.dp else 52.dp).then(semantics), contentAlignment = Alignment.Center) {
        when (style) {
            LoadingStyle.SystemDefault, LoadingStyle.MaterialExpressive -> {
                if (normalized == null) {
                    CircularProgressIndicator(modifier = Modifier.size(if (reduceMotion) 32.dp else indicatorSize), strokeWidth = if (compact) 2.5.dp else 3.dp)
                } else {
                    CircularProgressIndicator(progress = { normalized }, modifier = Modifier.size(if (compact) 24.dp else 32.dp), strokeWidth = if (compact) 2.5.dp else 3.dp)
                }
            }
            LoadingStyle.SmoothRing -> SmoothRing(normalized, reduceMotion, indicatorSize)
            LoadingStyle.WavyRing -> WavyRing(normalized, reduceMotion, indicatorSize)
            LoadingStyle.SegmentedRing -> SegmentedRing(normalized, reduceMotion, indicatorSize)
            LoadingStyle.RockPentagon -> RockPentagon(normalized, reduceMotion, indicatorSize, false)
            LoadingStyle.PentagonOrbit -> RockPentagon(normalized, reduceMotion, indicatorSize, true)
        }
    }
}

/** Compatibility overload for the existing Appearance preview call site. */
@Composable
fun AppLoadingIndicator(style: LoadingStyle, reduceMotion: Boolean) =
    AppLoadingIndicator(style = style, reduceMotion = reduceMotion, compact = false)

@Composable
fun RockContainedLoadingIndicator(modifier: Modifier = Modifier, reduceMotion: Boolean = LocalReduceMotion.current) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
        }
    }
}

@Composable
fun RockLinearProgressIndicator(modifier: Modifier = Modifier, progress: Float? = null, reduceMotion: Boolean = LocalReduceMotion.current) {
    val normalized = progress?.coerceIn(0f, 1f)
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalized?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalized?.let { "${(it * 100).toInt()} percent complete" } ?: "Loading"
    }
    Box(modifier.fillMaxWidth().then(semantics)) {
        if (normalized == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        else LinearProgressIndicator(progress = { normalized }, Modifier.fillMaxWidth())
    }
}

@Composable
fun RockCircularWavyProgressIndicator(progress: Float, modifier: Modifier = Modifier, reduceMotion: Boolean = LocalReduceMotion.current) {
    val normalized = progress.coerceIn(0f, 1f)
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = ProgressBarRangeInfo(normalized, 0f..1f)
        stateDescription = "${(normalized * 100).toInt()} percent complete"
    }
    Box(modifier.then(semantics), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { normalized }) }
}

enum class RockLoadingState { Loading, Progress, Success, Error, Empty, Retry }

@Composable
fun RockLoadingOverlay(state: RockLoadingState, modifier: Modifier = Modifier, progress: Float? = null, message: String? = null, onRetry: (() -> Unit)? = null) {
    when (state) {
        RockLoadingState.Loading -> AppLoadingIndicator(modifier, progress = null)
        RockLoadingState.Progress -> progress?.let { AppLoadingIndicator(modifier, progress = it) } ?: AppLoadingIndicator(modifier)
        RockLoadingState.Success -> Icon(Icons.Default.Check, "Success", tint = MaterialTheme.colorScheme.primary, modifier = modifier.size(28.dp))
        RockLoadingState.Error -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            onRetry?.let { TextButton(onClick = it) { Icon(Icons.Default.Refresh, null); Text("Retry") } }
        }
        RockLoadingState.Empty -> Text(message ?: "Nothing to show", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        RockLoadingState.Retry -> onRetry?.let { TextButton(onClick = it, modifier = modifier) { Icon(Icons.Default.Refresh, null); Text(message ?: "Retry") } }
    }
}

@Composable
private fun SmoothRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "rock-smooth-ring")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "rotation")
    Canvas(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation)) {
        val stroke = size.toPx() * .105f
        drawArc(primary.copy(alpha = .18f), -90f, 360f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawArc(primary, -90f, if (progress == null) 110f else 360f * progress, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun WavyRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "rock-wavy-ring")
    val phase by transition.animateFloat(0f, (2f * PI).toFloat(), infiniteRepeatable(tween(1250), RepeatMode.Restart), label = "phase")
    Canvas(Modifier.size(size)) {
        val pixels = size.toPx()
        val center = Offset(pixels / 2f, pixels / 2f)
        val baseRadius = pixels * .34f
        val amplitude = pixels * .045f
        val path = Path()
        for (i in 0..100) {
            val angle = -PI / 2.0 + i / 100.0 * 2.0 * PI
            val wavePhase = if (reduceMotion) 0.0 else phase.toDouble()
            val radius = baseRadius + amplitude * sin(angle * 7.0 + wavePhase).toFloat()
            val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, primary.copy(alpha = .18f), style = Stroke(width = pixels * .10f, cap = StrokeCap.Round))
        drawArc(primary, -90f, if (progress == null) 115f else 360f * progress, false, style = Stroke(width = pixels * .10f, cap = StrokeCap.Round))
    }
}

@Composable
private fun SegmentedRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "rock-segmented-ring")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "rotation")
    Canvas(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation)) {
        val stroke = size.toPx() * .12f
        val segments = 12
        val gap = 7f
        val completed = ((progress ?: 0f) * segments).coerceIn(0f, segments.toFloat())
        val activeSegment = (rotation / 30f).toInt() % segments
        repeat(segments) { index ->
            val start = -90f + index * (360f / segments) + gap / 2f
            val sweep = 360f / segments - gap
            val active = (progress != null && index < completed) || (progress == null && !reduceMotion && index == activeSegment)
            drawArc(if (active) primary else primary.copy(alpha = .16f), start, sweep, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun RockPentagon(progress: Float?, reduceMotion: Boolean, size: Dp, orbit: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "rock-pentagon")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(if (orbit) 2800 else 1800), RepeatMode.Restart), label = "rotation")
    val pulse by transition.animateFloat(.92f, 1.06f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val pixels = size.toPx()
            val center = Offset(pixels / 2f, pixels / 2f)
            val radius = pixels * .30f * if (reduceMotion) 1f else pulse
            val path = Path()
            PentagonAngles.forEachIndexed { index, angle ->
                val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            if (orbit && !reduceMotion) drawCircle(primary.copy(alpha = .10f), pixels * .47f, center, style = Stroke(width = pixels * .035f))
            drawPath(path, primary.copy(alpha = .20f), style = Stroke(width = pixels * .12f, cap = StrokeCap.Round))
            drawPath(path, primary, style = Stroke(width = pixels * .095f, cap = StrokeCap.Round))
            if (progress != null) drawArc(primary, -90f, 360f * progress, false, style = Stroke(width = pixels * .055f, cap = StrokeCap.Round))
        }
        if (orbit) Box(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation), contentAlignment = Alignment.TopCenter) {
            Surface(Modifier.size(5.dp), shape = RoundedCornerShape(50), color = primary) {}
        }
    }
}

@Composable
fun RockLoadingIndicator(modifier: Modifier = Modifier, progress: Float? = null) = AppLoadingIndicator(modifier = modifier, progress = progress, compact = true)
