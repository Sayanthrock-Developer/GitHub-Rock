package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * The single modern navigation chrome for GitHub Rock.
 * The mobile dock is a floating iPhone-style capsule with animated icon/label states.
 * MainNavigation's legacy dock remains underneath only as a compatibility layer and is
 * completely covered by the background mask below; users see one navigation bar only.
 */
@Composable
fun ModernNavigationChrome(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route
    if (modernTopDestinations.none { it.route == selectedRoute }) return

    Box(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth.value >= 600f) {
                ModernNavigationRail(
                    selectedRoute = selectedRoute,
                    onDestinationSelected = { navigate(navController, it) },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            } else {
                // Fully cover the legacy dock so only the modern dock is visible.
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(112.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {}

                ModernNavigationBottomBar(
                    selectedRoute = selectedRoute,
                    onDestinationSelected = { navigate(navController, it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

private fun navigate(navController: NavHostController, destination: TopDestination) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
internal fun ModernNavigationBottomBar(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp),
        shape = IphoneFloatingDockShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modernTopDestinations.forEach { destination ->
                ModernBottomItem(
                    destination = destination,
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModernBottomItem(
    destination: TopDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(220),
        label = "navigation indicator color"
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 25.dp else 23.dp,
        animationSpec = tween(180),
        label = "navigation icon size"
    )
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
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
        shape = RoundedCornerShape(29.dp),
        color = indicatorColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(160)) + expandHorizontally(tween(180)),
                exit = fadeOut(tween(100)) + shrinkHorizontally(tween(140))
            ) {
                Text(
                    text = destination.accessibilityLabel,
                    modifier = Modifier.padding(start = 5.dp),
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** Lifted capsule silhouette with raised/curved ends, matching the iPhone-style reference. */
private val IphoneFloatingDockShape: Shape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val lift = (height * 0.30f).coerceAtLeast(10f)
    val curve = (width * 0.035f).coerceIn(14f, 28f)

    moveTo(0f, height / 2f)
    cubicTo(0f, lift, curve, 0f, curve * 2.2f, 0f)
    lineTo(width - curve * 2.2f, 0f)
    cubicTo(width - curve, 0f, width, lift, width, height / 2f)
    cubicTo(width, height - lift, width - curve, height, width - curve * 2.2f, height)
    lineTo(curve * 2.2f, height)
    cubicTo(curve, height, 0f, height - lift, 0f, height / 2f)
    close()
}

@Composable
private fun ModernNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "GR",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
            modernTopDestinations.forEach { destination ->
                ModernRailItem(
                    destination = destination,
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination) }
                )
            }
        }
    }
}

@Composable
private fun ModernRailItem(
    destination: TopDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(220),
        label = "rail indicator color"
    )

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
        color = indicatorColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                destination.icon,
                null,
                Modifier.size(23.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selected) {
                Text(
                    destination.accessibilityLabel,
                    maxLines = 1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
