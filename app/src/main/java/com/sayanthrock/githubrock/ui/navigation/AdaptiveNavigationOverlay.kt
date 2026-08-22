package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.data.settings.NavigationStyle

private data class OverlayDestination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    OverlayDestination("home", "Home", Icons.Default.Home),
    OverlayDestination("repositories", "Repos", Icons.Default.Folder),
    OverlayDestination("builds", "Builds", Icons.Default.Build),
    OverlayDestination("downloads", "Downloads", Icons.Default.Download),
    OverlayDestination("profile", "Profile", Icons.Default.AccountCircle)
)

@Composable
fun AdaptiveNavigationOverlay(
    selectedRoute: String?,
    style: NavigationStyle,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        when (style) {
            NavigationStyle.Classic -> ClassicNavigation(selectedRoute, onDestinationSelected)
            NavigationStyle.Floating -> FloatingNavigation(selectedRoute, onDestinationSelected)
            NavigationStyle.Pill -> PillNavigation(selectedRoute, onDestinationSelected)
            NavigationStyle.Minimal -> MinimalNavigation(selectedRoute, onDestinationSelected)
        }
    }
}

@Composable
private fun ClassicNavigation(selectedRoute: String?, onDestinationSelected: (TopDestination) -> Unit) {
    NavigationBar(modifier = Modifier.widthIn(max = 620.dp), containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 4.dp) {
        destinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination.toTopDestination()) },
                icon = { Icon(destination.icon, destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun FloatingNavigation(selectedRoute: String?, onDestinationSelected: (TopDestination) -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Surface(
        modifier = Modifier.widthIn(max = 620.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            destinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(if (selected) 24.dp else 18.dp)).clickable { onDestinationSelected(destination.toTopDestination()) },
                    shape = RoundedCornerShape(if (selected) 24.dp else 18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = .35f)
                ) {
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(destination.icon, null, Modifier.size(if (selected) 24.dp else 22.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(destination.label, Modifier.padding(start = 5.dp), maxLines = 1, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PillNavigation(selectedRoute: String?, onDestinationSelected: (TopDestination) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(modifier = Modifier.widthIn(max = 520.dp), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .97f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            destinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(19.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = .2f)).clickable { onDestinationSelected(destination.toTopDestination()) }.padding(horizontal = 8.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(destination.icon, null, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(destination.label, maxLines = 1, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalNavigation(selectedRoute: String?, onDestinationSelected: (TopDestination) -> Unit) {
    Row(Modifier.widthIn(max = 560.dp).fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = .96f)).border(1.dp, MaterialTheme.colorScheme.outlineVariant), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        destinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            ColumnNavigationItem(destination, selected) { onDestinationSelected(destination.toTopDestination()) }
        }
    }
}

@Composable
private fun RowScope.ColumnNavigationItem(destination: OverlayDestination, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(destination.icon, destination.label, Modifier.size(21.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(destination.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun OverlayDestination.toTopDestination(): TopDestination = when (route) {
    "home" -> TopDestination.Home
    "repositories" -> TopDestination.Repositories
    "builds" -> TopDestination.Builds
    "downloads" -> TopDestination.Downloads
    else -> TopDestination.Profile
}
