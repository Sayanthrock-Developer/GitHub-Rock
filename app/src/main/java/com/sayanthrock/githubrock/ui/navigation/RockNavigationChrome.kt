package com.sayanthrock.githubrock.ui.navigation

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sayanthrock.githubrock.data.settings.AnimationStyle
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurComponent
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSurface
import com.sayanthrock.githubrock.ui.motion.RockMotion

private val rockNavigationDestinations = listOf(
    TopDestinationV2.Home,
    TopDestinationV2.Repositories,
    TopDestinationV2.Builds,
    TopDestinationV2.Downloads,
    TopDestinationV2.Profile
)

@Composable
fun RockNavigationChrome(navController: NavHostController, style: NavigationBarStyle = NavigationBarStyle.FloatingCapsule, animationStyle: AnimationStyle = AnimationStyle.Spring, reduceMotion: Boolean = false, blurSettings: ApplicationBlurSettings = ApplicationBlurSettings(), modifier: Modifier = Modifier) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route
    if (rockNavigationDestinations.none { it.route == selectedRoute }) return
    BoxWithConstraints(modifier.fillMaxSize().zIndex(10f)) {
        RockBottomNavigation(selectedRoute, style, maxWidth < 360.dp, animationStyle, reduceMotion, blurSettings, { navigateToTopLevel(navController, it) }, Modifier.align(Alignment.BottomCenter).zIndex(10f))
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
private fun RockBottomNavigation(selectedRoute: String?, style: NavigationBarStyle, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier = Modifier) {
    when (style) {
        NavigationBarStyle.FloatingCapsule -> FloatingCapsuleNavigation(selectedRoute, compact, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
        NavigationBarStyle.Classic -> ClassicNavigation(selectedRoute, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
        NavigationBarStyle.Minimal -> MinimalNavigation(selectedRoute, compact, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
        NavigationBarStyle.Glass -> GlassNavigation(selectedRoute, compact, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
        NavigationBarStyle.Compact -> CompactNavigation(selectedRoute, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
        NavigationBarStyle.Ios -> FuturisticNavigation(selectedRoute, animationStyle, reduceMotion, blurSettings, onDestinationSelected, modifier)
    }
}

@Composable
private fun FuturisticNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainerHigh, 0.55f, 18.dp, 700.dp, blurSettings, onDestinationSelected) {
        NavigationRow(76.dp, 8.dp, 2.dp) {
            rockNavigationDestinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.05f else 1f,
                    animationSpec = if (reduceMotion) androidx.compose.animation.core.tween(RockMotion.duration(true, RockMotion.Navigation)) else spring(stiffness = 500f, dampingRatio = 0.78f),
                    label = "futuristic navigation scale"
                )
                Box(Modifier.weight(1f).height(64.dp), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {}
                    }
                    RockNavigationItem(
                        destination = destination,
                        selected = selected,
                        showLabel = true,
                        label = futuristicNavigationLabel(destination),
                        modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale),
                        selectedShape = 20.dp,
                        animationStyle = animationStyle,
                        reduceMotion = reduceMotion,
                        onClick = { onDestinationSelected(destination) },
                        iconSize = if (selected) 23.dp else 21.dp,
                        selectedContainerAlpha = 0f,
                        verticalLabelLayout = true
                    )
                }
            }
        }
    }
}

private fun futuristicNavigationLabel(destination: TopDestinationV2): String = when (destination) {
    TopDestinationV2.Repositories -> "Repos"
    else -> destination.accessibilityLabel
}

@Composable
private fun FloatingCapsuleNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainerHigh, 0.55f, 18.dp, 700.dp, blurSettings, onDestinationSelected) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, selectedRoute == destination.route && !compact, Modifier.weight(1f), 26.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun ClassicNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier.fillMaxWidth(), RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surfaceContainer, 0.5f, 12.dp, 700.dp, blurSettings, onDestinationSelected) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, true, Modifier.weight(1f), 18.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun MinimalNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    val view = LocalView.current
    val slideModifier = navigationSlideGesture(view, onDestinationSelected)
    Row(modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 620.dp).fillMaxWidth().height(if (compact) 54.dp else 60.dp).then(slideModifier), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        rockNavigationDestinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            RockNavigationItem(destination, selected, selected && !compact, Modifier.weight(1f).height(if (compact) 48.dp else 54.dp), 18.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }, if (selected) 22.dp else 21.dp, true, 0.18f)
        }
    }
}

@Composable
private fun GlassNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surface, 0.65f, 14.dp, 700.dp, blurSettings, onDestinationSelected) {
        NavigationRow(70.dp, 6.dp, 3.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, selectedRoute == destination.route && !compact, Modifier.weight(1f), 22.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun CompactNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(24.dp), MaterialTheme.colorScheme.surfaceContainerHigh, 0.5f, 12.dp, 500.dp, blurSettings, onDestinationSelected) {
        NavigationRow(54.dp, 4.dp, 2.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, false, Modifier.weight(1f), 22.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }, 22.dp) } }
    }
}

@Composable
private fun NavigationSurface(modifier: Modifier, shape: RoundedCornerShape, color: Color, borderAlpha: Float, shadow: Dp, maxWidth: Dp, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, content: @Composable () -> Unit) {
    val view = LocalView.current
    val blurProfile = blurSettings.profileFor(ApplicationBlurComponent.NavigationBar)
    val blurEnabled = blurSettings.mode.name != "Off" && blurProfile.enabled && blurProfile.intensity > 0f
    val slideModifier = navigationSlideGesture(view, onDestinationSelected)
    val surfaceModifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).widthIn(max = maxWidth).fillMaxWidth().then(slideModifier)
    if (blurEnabled) {
        ApplicationBlurSurface(settings = blurSettings, component = ApplicationBlurComponent.NavigationBar, modifier = surfaceModifier, shape = shape) {
            Surface(modifier = Modifier.fillMaxSize(), shape = shape, color = color, contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)), tonalElevation = 0.dp, shadowElevation = 0.dp) { content() }
        }
    } else {
        Surface(modifier = surfaceModifier, shape = shape, color = color, contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)), tonalElevation = 2.dp, shadowElevation = shadow) { content() }
    }
}

private fun navigationSlideGesture(view: View, onDestinationSelected: (TopDestinationV2) -> Unit): Modifier = Modifier.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
        onDrag = { change, dragAmount ->
            if (size.width <= 0) return@detectDragGesturesAfterLongPress
            val horizontalDistance = kotlin.math.abs(dragAmount.x)
            val verticalDistance = kotlin.math.abs(dragAmount.y)
            if (horizontalDistance <= verticalDistance) return@detectDragGesturesAfterLongPress
            change.consume()
            val index = (change.position.x / size.width * rockNavigationDestinations.size).toInt().coerceIn(0, rockNavigationDestinations.lastIndex)
            onDestinationSelected(rockNavigationDestinations[index])
        }
    )
}

@Composable
private fun NavigationRow(height: Dp, horizontalPadding: Dp, spacing: Dp, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = horizontalPadding, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun RowScope.RockNavigationItem(destination: TopDestinationV2, selected: Boolean, showLabel: Boolean, modifier: Modifier, selectedShape: Dp, animationStyle: AnimationStyle, reduceMotion: Boolean, onClick: () -> Unit, iconSize: Dp = if (selected) 24.dp else 22.dp, transparent: Boolean = false, selectedContainerAlpha: Float = 1f, label: String = destination.accessibilityLabel, verticalLabelLayout: Boolean = false) {
    val view = LocalView.current
    val duration = RockMotion.duration(reduceMotion, RockMotion.Navigation)
    val selectedContainer by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = selectedContainerAlpha) else Color.Transparent, animationSpec = if (reduceMotion) androidx.compose.animation.core.tween(duration) else spring(), label = "navigation indicator color")
    val iconTint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val labelTint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.combinedClickable(role = Role.Tab, onClick = onClick, onLongClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }).semantics { contentDescription = destination.accessibilityLabel; role = Role.Tab; this.selected = selected }, shape = RoundedCornerShape(selectedShape), color = selectedContainer, contentColor = labelTint) {
        if (showLabel && verticalLabelLayout) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = destination.accessibilityLabel, modifier = Modifier.size(iconSize), tint = iconTint)
                Text(label, modifier = Modifier.padding(top = 2.dp), maxLines = 1, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = labelTint)
            }
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = if (showLabel) 6.dp else 0.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = destination.accessibilityLabel, modifier = Modifier.size(iconSize), tint = iconTint)
                if (showLabel) Text(label, modifier = Modifier.padding(start = 5.dp), maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = labelTint)
            }
        }
    }
}
