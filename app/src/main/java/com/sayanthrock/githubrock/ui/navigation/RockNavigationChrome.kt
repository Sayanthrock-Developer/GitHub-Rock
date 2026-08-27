package com.sayanthrock.githubrock.ui.navigation

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
 * Fixed navigation chrome. Phones use the original full-width bottom bar;
 * large screens use the compact rail. Content is responsible for reserving
 * the bottom bar's measured area so the bar never obscures the page.
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
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 3.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        rockNavigationDestinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.accessibilityLabel, maxLines = 1) },
                alwaysShowLabel = true,
                modifier = Modifier.semantics {
                    contentDescription = destination.accessibilityLabel
                    role = Role.Tab
                    this.selected = selectedRoute == destination.route
                },
                colors = NavigationBarItemDefaults.colors()
            )
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
            .padding(start = 14.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .width(78.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rockNavigationDestinations.forEach { destination ->
                RockRailItem(destination, selectedRoute == destination.route) {
                    onDestinationSelected(destination)
                }
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
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f)
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(58.dp)
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(destination.icon, null, Modifier.size(if (selected) 24.dp else 22.dp))
        }
    }
}
