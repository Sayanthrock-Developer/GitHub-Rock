package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.ui.theme.RockSurfaceRole
import com.sayanthrock.githubrock.ui.theme.rockContentColor
import com.sayanthrock.githubrock.ui.theme.rockSurfaceBorder
import com.sayanthrock.githubrock.ui.theme.rockSurfaceColor

@Composable
internal fun TvNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (TopDestinationV2) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(start = 28.dp, top = 28.dp, bottom = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.width(184.dp).fillMaxHeight().padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = rockSurfaceColor(RockSurfaceRole.Navigation),
            contentColor = rockContentColor(RockSurfaceRole.Navigation),
            border = rockSurfaceBorder(),
            tonalElevation = 0.dp,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GITHUB ROCK",
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                rockNavigationDestinations.forEach { destination ->
                    val selected = selectedRoute == destination.route
                    var focused by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .onFocusChanged { focused = it.isFocused }
                            .focusable()
                            .combinedClickable(role = Role.Tab, onClick = { onDestinationSelected(destination) })
                            .semantics {
                                contentDescription = destination.accessibilityLabel
                                role = Role.Tab
                                this.selected = selected
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.01f),
                        border = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, androidx.compose.ui.graphics.Color.Transparent),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selected) destination.selectedIcon else destination.icon,
                                contentDescription = destination.accessibilityLabel,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                destination.accessibilityLabel,
                                maxLines = 1,
                                fontSize = 16.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
