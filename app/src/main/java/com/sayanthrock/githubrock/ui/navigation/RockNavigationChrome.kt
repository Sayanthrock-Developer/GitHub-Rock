package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sayanthrock.githubrock.data.settings.AnimationStyle
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle

private val rockNavigationDestinations = listOf(
    TopDestinationV2.Home,
    TopDestinationV2.Repositories,
    TopDestinationV2.Builds,
    TopDestinationV2.Downloads,
    TopDestinationV2.Profile
)

private val MotionEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun RockNavigationChrome(
    navController: NavHostController,
    style: NavigationBarStyle = NavigationBarStyle.FloatingCapsule,
    animationStyle: AnimationStyle = AnimationStyle.Spring,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route
    if (rockNavigationDestinations.none { it.route == selectedRoute }) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        RockBottomNavigation(
            selectedRoute = selectedRoute,
            style = style,
            compact = maxWidth < 360.dp,
            animationStyle = animationStyle,
            reduceMotion = reduceMotion,
            onDestinationSelected = { navigateToTopLevel(navController, it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun navigateToTopLevel(navController: NavHostController, destination: TopDestinationV2) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RockBottomNavigation(
    selectedRoute: String?,
    style: NavigationBarStyle,
    compact: Boolean,
    animationStyle: AnimationStyle,
    reduceMotion: Boolean,
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    when (style) {
        NavigationBarStyle.FloatingCapsule -> FloatingCapsuleNavigation(selectedRoute, compact, animationStyle, reduceMotion, onDestinationSelected, modifier)
        NavigationBarStyle.Classic -> ClassicNavigation(selectedRoute, animationStyle, reduceMotion, onDestinationSelected, modifier)
        NavigationBarStyle.Minimal -> MinimalNavigation(selectedRoute, compact, animationStyle, reduceMotion, onDestinationSelected, modifier)
        NavigationBarStyle.Glass -> GlassNavigation(selectedRoute, compact, animationStyle, reduceMotion, onDestinationSelected, modifier)
        NavigationBarStyle.Compact -> CompactNavigation(selectedRoute, animationStyle, reduceMotion, onDestinationSelected, modifier)
    }
}

@Composable
private fun FloatingCapsuleNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f), 0.55f, 18.dp, 700.dp) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = selectedRoute == destination.route && !compact, modifier = Modifier.weight(1f), selectedShape = 26.dp, animationStyle = animationStyle, reduceMotion = reduceMotion, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun ClassicNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        NavigationRow(76.dp, 6.dp, 0.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = true, modifier = Modifier.weight(1f), selectedShape = 16.dp, animationStyle = animationStyle, reduceMotion = reduceMotion, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun MinimalNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp).widthIn(max = 700.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) { rockNavigationDestinations.forEach { destination ->
        RockNavigationItem(destination, selectedRoute == destination.route, showLabel = !compact && selectedRoute == destination.route, modifier = Modifier.widthIn(min = 54.dp).height(52.dp), selectedShape = 18.dp, animationStyle = animationStyle, reduceMotion = reduceMotion, onClick = { onDestinationSelected(destination) }, transparent = true)
    } }
}

@Composable
private fun GlassNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surface.copy(alpha = 0.58f), 0.65f, 14.dp, 700.dp) {
        NavigationRow(70.dp, 6.dp, 3.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = selectedRoute == destination.route && !compact, modifier = Modifier.weight(1f), selectedShape = 22.dp, animationStyle = animationStyle, reduceMotion = reduceMotion, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun CompactNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(24.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f), 0.5f, 12.dp, 500.dp) {
        NavigationRow(54.dp, 4.dp, 2.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = false, modifier = Modifier.weight(1f), selectedShape = 19.dp, iconSize = 22.dp, animationStyle = animationStyle, reduceMotion = reduceMotion, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun NavigationSurface(modifier: Modifier, shape: RoundedCornerShape, color: Color, borderAlpha: Float, shadow: Dp, maxWidth: Dp, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).widthIn(max = maxWidth),
        shape = shape,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)),
        tonalElevation = 2.dp,
        shadowElevation = shadow
    ) { content() }
}

@Composable
private fun NavigationRow(height: Dp, horizontalPadding: Dp, spacing: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = horizontalPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

private data class MotionProfile(
    val duration: Int,
    val selectedScale: Float,
    val pressedScale: Float,
    val iconLift: Float
)

private fun motionProfile(style: AnimationStyle, reduceMotion: Boolean): MotionProfile = when {
    reduceMotion -> MotionProfile(0, 1f, 1f, 0f)
    style == AnimationStyle.Liquid -> MotionProfile(280, 1.015f, 0.97f, -0.5f)
    style == AnimationStyle.Spring -> MotionProfile(220, 1.025f, 0.965f, -1.0f)
    style == AnimationStyle.Cinematic -> MotionProfile(360, 1.02f, 0.97f, -0.75f)
    style == AnimationStyle.Magnetic -> MotionProfile(200, 1.035f, 0.96f, -1.25f)
    else -> MotionProfile(230, 1.025f, 0.965f, -1.0f)
}

@Composable
private fun RowScope.RockNavigationItem(
    destination: TopDestinationV2,
    selected: Boolean,
    showLabel: Boolean,
    modifier: Modifier,
    selectedShape: Dp,
    animationStyle: AnimationStyle,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = if (selected) 24.dp else 22.dp,
    transparent: Boolean = false
) {
    val profile = motionProfile(animationStyle, reduceMotion)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        pressed -> profile.pressedScale
        selected -> profile.selectedScale
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (reduceMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = when (animationStyle) {
                AnimationStyle.Magnetic -> Spring.StiffnessMedium
                AnimationStyle.Cinematic -> Spring.StiffnessLow
                else -> Spring.StiffnessMediumLow
            }
        ),
        label = "navigation item scale"
    )
    val iconLift by animateFloatAsState(
        targetValue = if (selected) profile.iconLift else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(profile.duration, easing = MotionEase),
        label = "navigation icon lift"
    )
    val selectedContainer by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = if (reduceMotion) tween(0) else tween(profile.duration, easing = MotionEase),
        label = "navigation indicator color"
    )
    val labelVisible = showLabel

    Surface(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = destination.accessibilityLabel
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(selectedShape),
        color = if (transparent) selectedContainer else selectedContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = iconLift
                }
                .padding(horizontal = if (labelVisible) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(destination.icon, contentDescription = null, modifier = Modifier.size(iconSize))
            AnimatedVisibility(
                visible = labelVisible,
                enter = fadeIn(tween(profile.duration, easing = MotionEase)) + expandHorizontally(tween(profile.duration, easing = MotionEase)),
                exit = fadeOut(tween(profile.duration, easing = MotionEase)) + shrinkHorizontally(tween(profile.duration, easing = MotionEase))
            ) {
                Text(destination.accessibilityLabel, modifier = Modifier.padding(start = 6.dp), maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
