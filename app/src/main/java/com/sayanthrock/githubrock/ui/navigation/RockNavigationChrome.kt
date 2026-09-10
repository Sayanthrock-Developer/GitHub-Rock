package com.sayanthrock.githubrock.ui.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    BoxWithConstraints(modifier.fillMaxSize()) {
        RockBottomNavigation(selectedRoute, style, maxWidth < 360.dp, animationStyle, reduceMotion, blurSettings, { navigateToTopLevel(navController, it) }, Modifier.align(Alignment.BottomCenter))
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
    }
}

@Composable
private fun FloatingCapsuleNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f), 0.55f, 18.dp, 700.dp, blurSettings) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, selectedRoute == destination.route && !compact, Modifier.weight(1f), 26.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun ClassicNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier.fillMaxWidth(), RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), 0.5f, 12.dp, 700.dp, blurSettings) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, true, Modifier.weight(1f), 18.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun MinimalNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    Row(modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 620.dp).fillMaxWidth().height(if (compact) 54.dp else 60.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        rockNavigationDestinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            RockNavigationItem(destination, selected, selected && !compact, Modifier.weight(1f).height(if (compact) 48.dp else 54.dp), 18.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }, if (selected) 22.dp else 21.dp, true, 0.12f)
        }
    }
}

@Composable
private fun GlassNavigation(selectedRoute: String?, compact: Boolean, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surface.copy(alpha = 0.58f), 0.65f, 14.dp, 700.dp, blurSettings) {
        NavigationRow(70.dp, 6.dp, 3.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, selectedRoute == destination.route && !compact, Modifier.weight(1f), 22.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }) } }
    }
}

@Composable
private fun CompactNavigation(selectedRoute: String?, animationStyle: AnimationStyle, reduceMotion: Boolean, blurSettings: ApplicationBlurSettings, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(24.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f), 0.5f, 12.dp, 500.dp, blurSettings) {
        NavigationRow(54.dp, 4.dp, 2.dp) { rockNavigationDestinations.forEach { destination -> RockNavigationItem(destination, selectedRoute == destination.route, false, Modifier.weight(1f), 22.dp, animationStyle, reduceMotion, { onDestinationSelected(destination) }, 22.dp) } }
    }
}

@Composable
private fun NavigationSurface(modifier: Modifier, shape: RoundedCornerShape, color: Color, borderAlpha: Float, shadow: Dp, maxWidth: Dp, blurSettings: ApplicationBlurSettings, content: @Composable () -> Unit) {
    val blurProfile = blurSettings.profileFor(ApplicationBlurComponent.NavigationBar)
    val blurEnabled = blurSettings.mode.name != "Off" && blurProfile.enabled && blurProfile.intensity > 0f
    if (blurEnabled) {
        ApplicationBlurSurface(settings = blurSettings, component = ApplicationBlurComponent.NavigationBar, modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).widthIn(max = maxWidth), shape = shape) {
            Surface(modifier = Modifier.fillMaxSize(), shape = shape, color = color.copy(alpha = 0f), contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)), tonalElevation = 0.dp, shadowElevation = 0.dp) { content() }
        }
    } else {
        Surface(modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).widthIn(max = maxWidth), shape = shape, color = color, contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)), tonalElevation = 2.dp, shadowElevation = shadow) { content() }
    }
}

@Composable
private fun NavigationRow(height: Dp, horizontalPadding: Dp, spacing: Dp, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = horizontalPadding, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun RowScope.RockNavigationItem(destination: TopDestinationV2, selected: Boolean, showLabel: Boolean, modifier: Modifier, selectedShape: Dp, animationStyle: AnimationStyle, reduceMotion: Boolean, onClick: () -> Unit, iconSize: Dp = if (selected) 24.dp else 22.dp, transparent: Boolean = false, selectedContainerAlpha: Float = 1f) {
    val view = LocalView.current
    val duration = RockMotion.duration(reduceMotion, RockMotion.Navigation)
    val selectedContainer by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = selectedContainerAlpha) else Color.Transparent, animationSpec = tween(durationMillis = duration), label = "navigation indicator color")
    val pressScale by animateFloatAsState(targetValue = 1f, animationSpec = tween(durationMillis = 120), label = "navigation press scale")
    Surface(modifier = modifier.combinedClickable(role = Role.Tab, onClick = onClick, onLongClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }).semantics { contentDescription = destination.accessibilityLabel; role = Role.Tab; this.selected = selected }, shape = RoundedCornerShape(selectedShape), color = selectedContainer, contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
        Row(Modifier.fillMaxSize().padding(horizontal = if (showLabel) 8.dp else 0.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = destination.accessibilityLabel, modifier = Modifier.size(iconSize))
            if (showLabel) Text(destination.accessibilityLabel, modifier = Modifier.padding(start = 6.dp), maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
