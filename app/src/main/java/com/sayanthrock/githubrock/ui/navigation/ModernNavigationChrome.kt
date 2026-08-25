package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
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

@Composable
fun ModernNavigationChrome(navController: NavHostController, modifier: Modifier = Modifier) {
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

/** The single mobile navigation component; the previous glass dock is not retained. */
@Composable
internal fun ModernNavigationBottomBar(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = IphoneFloatingDockShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
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
    val activeColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(220),
        label = "navigation active color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "navigation content color"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .animateContentSize(animationSpec = tween(220))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = destination.accessibilityLabel
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.animateContentSize(animationSpec = tween(220)),
            shape = RoundedCornerShape(22.dp),
            color = activeColor
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (selected) 13.dp else 12.dp,
                    vertical = 9.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = contentColor
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(160)) + expandHorizontally(tween(180)),
                    exit = fadeOut(tween(100)) + shrinkHorizontally(tween(160))
                ) {
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Floating capsule whose left and right ends lift smoothly toward the center,
 * producing the requested iPhone-style raised/curved silhouette.
 */
private val IphoneFloatingDockShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val lift = (h * 0.30f).coerceAtLeast(10f)
    val curve = (w * 0.035f).coerceIn(14f, 28f)

    moveTo(0f, h / 2f)
    cubicTo(0f, lift, curve, 0f, curve * 2.2f, 0f)
    lineTo(w - curve * 2.2f, 0f)
    cubicTo(w - curve, 0f, w, lift, w, h / 2f)
    cubicTo(w, h - lift, w - curve, h, w - curve * 2.2f, h)
    lineTo(curve * 2.2f, h)
    cubicTo(curve, h, 0f, h - lift, 0f, h / 2f)
    close()
}

@Composable
private fun ModernNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(92.dp).windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(Modifier.size(40.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text("GR", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
            modernTopDestinations.forEach { destination ->
                ModernRailItem(destination, selectedRoute == destination.route) { onDestinationSelected(destination) }
            }
        }
    }
}

@Composable
private fun ModernRailItem(destination: TopDestination, selected: Boolean, onClick: () -> Unit) {
    val indicatorColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        tween(220),
        label = "rail indicator color"
    )
    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp).clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = destination.accessibilityLabel; role = Role.Tab; this.selected = selected },
        shape = RoundedCornerShape(18.dp), color = indicatorColor
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(destination.icon, null, Modifier.size(23.dp), tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected) Text(destination.label, maxLines = 1, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
