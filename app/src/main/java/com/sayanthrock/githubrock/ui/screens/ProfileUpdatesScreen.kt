package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.ui.components.GlassCard

enum class ProfileUpdateSection(val route: String, val title: String, val subtitle: String) {
    WhatsNew("whats-new", "What's new", "Recent GitHub Rock improvements"),
    Announcements("announcements", "Notifications", "Your GitHub notifications");
    companion object { fun fromRoute(value: String?) = entries.firstOrNull { it.route.equals(value, true) } ?: WhatsNew }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUpdatesScreen(section: ProfileUpdateSection, onBack: () -> Unit) {
    if (section == ProfileUpdateSection.Announcements) {
        GitHubNotificationsScreen(onBack)
        return
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text(section.title, fontWeight = FontWeight.Black); Text(section.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        GlassCard(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column { Text("GitHub Rock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("Your app updates are shown here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GitHubNotificationsScreen(onBack: () -> Unit, viewModel: GitHubNotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("Notifications", fontWeight = FontWeight.Black); Text(if (state.unreadCount == 0) "All caught up" else "${state.unreadCount} unread", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = state.filter is NotificationsFilter.All, onClick = { viewModel.setFilter(NotificationsFilter.All) }, label = { Text("All") })
                FilterChip(selected = state.filter is NotificationsFilter.Unread, onClick = { viewModel.setFilter(NotificationsFilter.Unread) }, label = { Text("Unread ${state.unreadCount}") })
                if (state.unreadCount > 0) AssistChip(onClick = viewModel::markAllRead, label = { Text("Mark all read") })
            }
            state.error?.let { message -> Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) { Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            state.actionError?.let { message -> Text(message, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error) }
            if (state.loading) {
                Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
            } else if (state.visibleItems.isEmpty()) {
                Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("No notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("GitHub has nothing matching this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.visibleItems, key = { it.id }) { notification ->
                        NotificationCard(notification, onRead = { viewModel.markRead(notification.id) }, onOpen = { notification.subject.url?.let(uriHandler::openUri) ?: notification.url?.let(uriHandler::openUri) })
                    }
                    if (state.hasMore) item { LoadMoreRow(state.loadingMore, viewModel::loadMore) }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: GitHubNotification, onRead: () -> Unit, onOpen: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.size(10.dp), shape = MaterialTheme.shapes.small, color = if (notification.unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {}
                Column(Modifier.weight(1f)) {
                    Text(notification.subject.type.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(notification.subject.title, style = MaterialTheme.typography.titleMedium, fontWeight = if (notification.unread) FontWeight.Black else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (notification.unread) IconButton(onClick = onRead) { Icon(Icons.Default.Check, contentDescription = "Mark as read") }
            }
            Text(notification.repository.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(notification.reason.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else AssistChip(onClick = onClick, label = { Text("Load more") })
    }
}
