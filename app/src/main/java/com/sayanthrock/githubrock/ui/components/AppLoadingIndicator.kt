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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

private val PentagonAngles = List(5) { index -> -PI / 2.0 + (index * 2.0 * PI / 5.0) }

/** Central production loading/progress API used by GitHub Rock. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    compact: Boolean = false,
    style: LoadingStyle = LocalLoadingStyle.current,
    reduceMotion: Boolean = LocalReduceMotion.current
) {
    val normalizedProgress = progress?.coerceIn(0f, 1f)
    val indicatorSize = if (compact) 28.dp else 44.dp
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalizedProgress?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalizedProgress?.let { "${(it * 100).toInt()} percent loaded" } ?: "Loading"
        contentDescription = normalizedProgress?.let { "Loading progress ${(it * 100).toInt()} percent" } ?: "Loading"
    }
    Box(modifier = modifier.height(if (compact) 36.dp else 52.dp).then(semantics), contentAlignment = Alignment.Center) {
        when (style) {
            LoadingStyle.SystemDefault, LoadingStyle.MaterialExpressive -> {
                if (normalizedProgress == null && !reduceMotion) LoadingIndicator(Modifier.size(indicatorSize))
                else CircularProgressIndicator(progress = { normalizedProgress ?: 0.72f }, Modifier.size(if (compact) 24.dp else 32.dp), strokeWidth = if (compact) 2.5.dp else 3.dp)
            }
            LoadingStyle.SmoothRing -> SmoothRing(normalizedProgress, reduceMotion, indicatorSize)
            LoadingStyle.WavyRing -> WavyRing(normalizedProgress, reduceMotion, indicatorSize)
            LoadingStyle.SegmentedRing -> SegmentedRing(normalizedProgress, reduceMotion, indicatorSize)
            LoadingStyle.RockPentagon -> RockPentagon(normalizedProgress, reduceMotion, indicatorSize, false)
            LoadingStyle.PentagonOrbit -> RockPentagon(normalizedProgress, reduceMotion, indicatorSize, true)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RockContainedLoadingIndicator(modifier: Modifier = Modifier, reduceMotion: Boolean = LocalReduceMotion.current) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            if (reduceMotion) CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp) else LoadingIndicator(Modifier.size(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RockLinearProgressIndicator(modifier: Modifier = Modifier, progress: Float? = null, reduceMotion: Boolean = LocalReduceMotion.current) {
    val normalized = progress?.coerceIn(0f, 1f)
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalized?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalized?.let { "${(it * 100).toInt()} percent complete" } ?: "Loading"
    }
    Box(modifier.fillMaxWidth().then(semantics)) {
        if (reduceMotion) {
            if (normalized == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator(progress = { normalized }, Modifier.fillMaxWidth())
        } else androidx.compose.material3.LinearWavyProgressIndicator(progress = normalized?.let { { it } }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RockCircularWavyProgressIndicator(progress: Float?, modifier: Modifier = Modifier, reduceMotion: Boolean = LocalReduceMotion.current) {
    val normalized = progress?.coerceIn(0f, 1f)
    val semantics = Modifier.clearAndSetSemantics {
        progressBarRangeInfo = normalized?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        stateDescription = normalized?.let { "${(it * 100).toInt()} percent complete" } ?: "Loading"
    }
    Box(modifier.then(semantics), contentAlignment = Alignment.Center) {
        if (reduceMotion) CircularProgressIndicator(progress = { normalized ?: 0.5f })
        else androidx.compose.material3.CircularWavyProgressIndicator(progress = normalized?.let { { it } })
    }
}

enum class RockLoadingState { Loading, Progress, Success, Error, Empty, Retry }

@Composable
fun RockLoadingOverlay(state: RockLoadingState, modifier: Modifier = Modifier, progress: Float? = null, message: String? = null, onRetry: (() -> Unit)? = null) {
    when (state) {
        RockLoadingState.Loading -> AppLoadingIndicator(modifier, progress = null)
        RockLoadingState.Progress -> AppLoadingIndicator(modifier, progress = progress)
        RockLoadingState.Success -> Icon(Icons.Default.Check, "Success", tint = MaterialTheme.colorScheme.primary, modifier = modifier.size(28.dp))
        RockLoadingState.Error -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            message?.let { androidx.compose.material3.Text(it, color = MaterialTheme.colorScheme.error) }
            onRetry?.let { androidx.compose.material3.TextButton(onClick = it) { Icon(Icons.Default.Refresh, null); androidx.compose.material3.Text("Retry") } }
        }
        RockLoadingState.Empty -> androidx.compose.material3.Text(message ?: "Nothing to show", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        RockLoadingState.Retry -> onRetry?.let { androidx.compose.material3.TextButton(onClick = it, modifier = modifier) { Icon(Icons.Default.Refresh, null); androidx.compose.material3.Text(message ?: "Retry") } }
    }
}

@Composable
fun AppLoadingIndicator(style: LoadingStyle, reduceMotion: Boolean) = AppLoadingIndicator(style = style, reduceMotion = reduceMotion)

@Composable
private fun SmoothRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "rock-smooth-ring")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "rotation")
    Canvas(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation)) {
        val stroke = size.toPx() * .105f
        drawArc(MaterialTheme.colorScheme.primary.copy(alpha = .18f), -90f, 360f, false, style = Stroke(stroke, StrokeCap.Round))
        drawArc(MaterialTheme.colorScheme.primary, -90f, if (progress == null) 110f else 360f * progress, false, style = Stroke(stroke, StrokeCap.Round))
    }
}

@Composable
private fun WavyRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "rock-wavy-ring")
    val phase by transition.animateFloat(0f, (2f * PI).toFloat(), infiniteRepeatable(tween(1250), RepeatMode.Restart), label = "wave-phase")
    Canvas(Modifier.size(size)) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val baseRadius = size.toPx() * .34f
        val amplitude = size.toPx() * .045f
        val path = Path()
        for (i in 0..100) {
            val angle = -PI / 2.0 + (i.toDouble() / 100.0) * 2.0 * PI
            val radius = baseRadius + amplitude * sin(angle * 7.0 + if (reduceMotion) 0f else phase.toDouble()).toFloat()
            val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, MaterialTheme.colorScheme.primary.copy(alpha = .18f), style = Stroke(size.toPx() * .10f, StrokeCap.Round))
        drawArc(MaterialTheme.colorScheme.primary, -90f, if (progress == null) 115f else 360f * progress, false, style = Stroke(size.toPx() * .10f, StrokeCap.Round))
    }
}

@Composable
private fun SegmentedRing(progress: Float?, reduceMotion: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "rock-segmented-ring")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "rotation")
    Canvas(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation)) {
        val stroke = size.toPx() * .12f
        val segments = 12
        val gap = 7f
        val completed = ((progress ?: 0.35f) * segments).coerceIn(0f, segments.toFloat())
        repeat(segments) { index ->
            val start = -90f + index * (360f / segments) + gap / 2f
            val sweep = 360f / segments - gap
            val active = progress != null && index < completed
            val animatedActive = progress == null && index == ((rotation / 30f).toInt() % segments)
            drawArc(if (active || animatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .16f), start, sweep, false, style = Stroke(stroke, StrokeCap.Round))
        }
    }
}

@Composable
private fun RockPentagon(progress: Float?, reduceMotion: Boolean, size: Dp, orbit: Boolean) {
    val transition = rememberInfiniteTransition(label = "rock-pentagon")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(if (orbit) 2800 else 1800), RepeatMode.Restart), label = "rotation")
    val pulse by transition.animateFloat(.92f, 1.06f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = size.toPx() * .30f * if (reduceMotion) 1f else pulse
            val path = Path()
            PentagonAngles.forEachIndexed { index, angle ->
                val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            if (orbit && !reduceMotion) drawCircle(MaterialTheme.colorScheme.primary.copy(alpha = .10f), size.toPx() * .47f, center, style = Stroke(size.toPx() * .035f))
            drawPath(path, MaterialTheme.colorScheme.primary.copy(alpha = .20f), style = Stroke(size.toPx() * .12f, StrokeCap.Round))
            drawPath(path, MaterialTheme.colorScheme.primary, style = Stroke(size.toPx() * .095f, StrokeCap.Round))
            if (progress != null) drawArc(MaterialTheme.colorScheme.primary, -90f, 360f * progress, false, style = Stroke(size.toPx() * .055f, StrokeCap.Round))
        }
        if (orbit) Box(Modifier.size(size).rotate(if (reduceMotion) 0f else rotation), contentAlignment = Alignment.TopCenter) {
            Surface(Modifier.size(5.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {}
        }
    }
}

@Composable
fun RockLoadingIndicator(modifier: Modifier = Modifier, progress: Float? = null) = AppLoadingIndicator(modifier, progress, compact = true)
