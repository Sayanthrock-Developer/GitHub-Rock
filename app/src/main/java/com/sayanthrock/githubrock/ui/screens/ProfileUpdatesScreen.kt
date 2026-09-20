package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.core.translation.GoogleTranslationService
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.icons.RockIcon

enum class ProfileUpdateSection(val route: String, val title: String, val subtitle: String) {
    WhatsNew("whats-new", "What's new", "Recent GitHub Rock improvements"),
    Announcements("announcements", "Notifications", "Your GitHub notifications");
    companion object { fun fromRoute(value: String?) = entries.firstOrNull { it.route.equals(value, true) } ?: WhatsNew }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUpdatesScreen(
    section: ProfileUpdateSection,
    onBack: () -> Unit,
    translationViewModel: ProfileUpdatesTranslationViewModel = hiltViewModel()
) {
    if (section == ProfileUpdateSection.Announcements) {
        GitHubNotificationsScreen(onBack)
        return
    }

    val translationState by translationViewModel.state.collectAsState()
    val sourceTitle = "What's new"
    val sourceSubtitle = "Recent GitHub Rock improvements"
    val sourceDescription = "Your app updates are shown here."
    val selectedLanguage = translationState.targetLanguage ?: "en"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            translationState.translatedTitle ?: sourceTitle,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            translationState.translatedSubtitle ?: sourceSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(RockIcon.Back.vector(), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WhatsNewTranslationCard(
                    selectedLanguage = selectedLanguage,
                    loading = translationState.loading,
                    error = translationState.error,
                    onSelectLanguage = { language ->
                        translationViewModel.translate(
                            sourceTitle,
                            sourceSubtitle,
                            sourceDescription,
                            language
                        )
                    },
                    onTranslate = {
                        translationViewModel.translate(
                            sourceTitle,
                            sourceSubtitle,
                            sourceDescription,
                            selectedLanguage
                        )
                    }
                )
            }

            item {
                GlassCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            RockIcon.AutoAwesome.vector(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                translationState.translatedTitle ?: "GitHub Rock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                translationState.translatedDescription ?: sourceDescription,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsNewTranslationCard(
    selectedLanguage: String,
    loading: Boolean,
    error: String?,
    onSelectLanguage: (String) -> Unit,
    onTranslate: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val language = GoogleTranslationService.supportedLanguages.firstOrNull { it.code == selectedLanguage }
        ?: GoogleTranslationService.supportedLanguages.first()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        RockIcon.Public.vector(),
                        contentDescription = "Translate",
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text("Translate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Render this page in another language.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "Target language",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(RockIcon.Public.vector(), contentDescription = null)
                        Text(
                            language.label,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(RockIcon.ArrowDropDown.vector(), contentDescription = "Choose language")
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    GoogleTranslationService.supportedLanguages.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                expanded = false
                                onSelectLanguage(option.code)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onTranslate,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(RockIcon.Public.vector(), contentDescription = null)
                }
                Text(
                    if (loading) "Translating…" else "Translate to " + language.label,
                    modifier = Modifier.padding(start = 10.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            error?.let {
                Text(
                    "Translation unavailable: " + it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), contentDescription = "Back") } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(RockIcon.Refresh.vector(), contentDescription = "Refresh") } },
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
                if (notification.unread) IconButton(onClick = onRead) { Icon(RockIcon.Check.vector(), contentDescription = "Mark as read") }
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
