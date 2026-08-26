package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private val rockNavigationDestinations = listOf(
    TopDestinationV2.Home,
    TopDestinationV2.Repositories,
    TopDestinationV2.Builds,
    TopDestinationV2.Downloads,
    TopDestinationV2.Profile
)

/**
 * The single navigation chrome for GitHub Rock.
 *
 * Phone: floating pill navigation.
 * Large screens: compact navigation rail.
 * The navigation chrome owns only top-level navigation; nested screens remain
 * inside the existing NavHost and are intentionally not shown here.
 */
@Composable
fun RockNavigationChrome(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route
    if (rockNavigationDestinations.none { it.route == selectedRoute }) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            RockBottomNavigation(
                selectedRoute = selectedRoute,
                onDestinationSelected = { navigateToTopLevel(navController, it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            RockNavigationRail(
                selectedRoute = selectedRoute,
                onDestinationSelected = { navigateToTopLevel(navController, it) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

private fun navigateToTopLevel(
    navController: NavHostController,
    destination: TopDestinationV2
) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RockBottomNavigation(
    selectedRoute: String?,
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .widthIn(max = 680.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rockNavigationDestinations.forEach { destination ->
                RockBottomItem(
                    destination = destination,
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.RockBottomItem(
    destination: TopDestinationV2,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = destination.accessibilityLabel
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(destination.icon, null, Modifier.size(if (selected) 24.dp else 23.dp))
            AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = destination.accessibilityLabel,
                    modifier = Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun RockNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("R", style = MaterialTheme.typography.titleMedium)
                }
            }

            rockNavigationDestinations.forEach { destination ->
                RockRailItem(
                    destination = destination,
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }
    }
}

@Composable
private fun RockRailItem(
    destination: TopDestinationV2,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = destination.accessibilityLabel
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(destination.icon, null, Modifier.size(23.dp))
            AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = destination.accessibilityLabel,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
