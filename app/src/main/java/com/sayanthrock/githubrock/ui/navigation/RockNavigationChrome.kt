package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle

private val rockNavigationDestinations = listOf(
    TopDestinationV2.Home,
    TopDestinationV2.Repositories,
    TopDestinationV2.Builds,
    TopDestinationV2.Downloads,
    TopDestinationV2.Profile
)

/** Shared top-level navigation chrome. The style changes only presentation; routes and behavior stay shared. */
@Composable
fun RockNavigationChrome(
    navController: NavHostController,
    style: NavigationBarStyle = NavigationBarStyle.FloatingCapsule,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route
    if (rockNavigationDestinations.none { it.route == selectedRoute }) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            RockBottomNavigation(
                selectedRoute = selectedRoute,
                style = style,
                compact = maxWidth < 360.dp,
                onDestinationSelected = { navigateToTopLevel(navController, it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            RockNavigationRail(
                selectedRoute = selectedRoute,
                style = style,
                onDestinationSelected = { navigateToTopLevel(navController, it) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
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
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    when (style) {
        NavigationBarStyle.FloatingCapsule -> FloatingCapsuleNavigation(selectedRoute, compact, onDestinationSelected, modifier)
        NavigationBarStyle.Classic -> ClassicNavigation(selectedRoute, onDestinationSelected, modifier)
        NavigationBarStyle.Minimal -> MinimalNavigation(selectedRoute, compact, onDestinationSelected, modifier)
        NavigationBarStyle.Glass -> GlassNavigation(selectedRoute, compact, onDestinationSelected, modifier)
        NavigationBarStyle.Compact -> CompactNavigation(selectedRoute, onDestinationSelected, modifier)
    }
}

@Composable
private fun FloatingCapsuleNavigation(selectedRoute: String?, compact: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f), 0.55f, 18.dp, 620.dp) {
        NavigationRow(72.dp, 7.dp, 4.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = selectedRoute == destination.route && !compact, compact = compact, modifier = Modifier.weight(1f), selectedShape = 26.dp, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun ClassicNavigation(selectedRoute: String?, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        NavigationRow(76.dp, 6.dp, 0.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = true, compact = false, modifier = Modifier.weight(1f), selectedShape = 16.dp, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun MinimalNavigation(selectedRoute: String?, compact: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp).widthIn(max = 620.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) { rockNavigationDestinations.forEach { destination ->
        RockNavigationItem(destination, selectedRoute == destination.route, showLabel = !compact && selectedRoute == destination.route, compact = compact, modifier = Modifier.widthIn(min = 54.dp).height(56.dp), selectedShape = 18.dp, onClick = { onDestinationSelected(destination) }, transparent = true)
    } }
}

@Composable
private fun GlassNavigation(selectedRoute: String?, compact: Boolean, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(28.dp), MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), 0.45f, 14.dp, 620.dp) {
        NavigationRow(70.dp, 6.dp, 3.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = selectedRoute == destination.route && !compact, compact = compact, modifier = Modifier.weight(1f), selectedShape = 22.dp, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun CompactNavigation(selectedRoute: String?, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    NavigationSurface(modifier, RoundedCornerShape(24.dp), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f), 0.5f, 12.dp, 430.dp) {
        NavigationRow(54.dp, 4.dp, 2.dp) { rockNavigationDestinations.forEach { destination ->
            RockNavigationItem(destination, selectedRoute == destination.route, showLabel = false, compact = true, modifier = Modifier.weight(1f), selectedShape = 19.dp, iconSize = 22.dp, onClick = { onDestinationSelected(destination) })
        } }
    }
}

@Composable
private fun NavigationSurface(modifier: Modifier, shape: RoundedCornerShape, color: Color, borderAlpha: Float, shadow: androidx.compose.ui.unit.Dp, maxWidth: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp).widthIn(max = maxWidth),
        shape = shape,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)),
        tonalElevation = 2.dp,
        shadowElevation = shadow
    ) { content() }
}

@Composable
private fun NavigationRow(height: androidx.compose.ui.unit.Dp, horizontalPadding: androidx.compose.ui.unit.Dp, spacing: androidx.compose.ui.unit.Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = horizontalPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RowScope.RockNavigationItem(
    destination: TopDestinationV2,
    selected: Boolean,
    showLabel: Boolean,
    compact: Boolean,
    modifier: Modifier,
    selectedShape: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = if (selected) 24.dp else 22.dp,
    transparent: Boolean = false
) {
    val selectedContainer by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "navigation indicator color"
    )
    Surface(
        modifier = modifier.clickable(role = Role.Tab, onClick = onClick).semantics {
            contentDescription = destination.accessibilityLabel
            role = Role.Tab
            this.selected = selected
        },
        shape = RoundedCornerShape(selectedShape),
        color = if (transparent) selectedContainer else selectedContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = if (showLabel) 8.dp else 0.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(destination.icon, contentDescription = null, modifier = Modifier.size(iconSize))
            if (showLabel) Text(destination.accessibilityLabel, modifier = Modifier.padding(start = 6.dp), maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RockNavigationRail(selectedRoute: String?, style: NavigationBarStyle, onDestinationSelected: (TopDestinationV2) -> Unit, modifier: Modifier) {
    val railColor = if (style == NavigationBarStyle.Glass) MaterialTheme.colorScheme.surface.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    Surface(
        modifier = modifier.padding(start = 14.dp).windowInsetsPadding(WindowInsets.safeDrawing).width(78.dp),
        shape = RoundedCornerShape(30.dp), color = railColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)), tonalElevation = 2.dp, shadowElevation = 18.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rockNavigationDestinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (style == NavigationBarStyle.Compact) 48.dp else 58.dp).clickable(role = Role.Tab) { onDestinationSelected(destination) }.semantics { contentDescription = destination.accessibilityLabel; role = Role.Tab; this.selected = selected },
                    shape = RoundedCornerShape(if (style == NavigationBarStyle.Minimal) 16.dp else 24.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(destination.icon, contentDescription = null, Modifier.size(if (selected) 24.dp else 22.dp))
                        if (selected && style != NavigationBarStyle.Compact) Text(destination.accessibilityLabel, maxLines = 1, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
