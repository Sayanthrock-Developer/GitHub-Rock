package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.components.LogoutConfirmationSheet
import java.util.Locale

data class ConnectedProfileDashboardUiState(
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

private data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState = ProfileExplorerState(),
    onInspectProfile: (String) -> Unit = {},
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAccounts: () -> Unit = onOpenFeatures,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenGitHubUrl: (String) -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit = {},
    onLogout: () -> Unit,
    onOpenRepositories: () -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    dashboardStateOverride: ConnectedProfileDashboardUiState? = null
) {
    var activeLibraryRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var activeUpdateRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var showLogoutSheet by rememberSaveable { mutableStateOf(false) }

    activeLibraryRoute?.let { route ->
        ProfileLibraryScreen(
            section = ProfileLibrarySection.fromRoute(route),
            onBack = { activeLibraryRoute = null },
            onOpenRepository = onOpenRepository
        )
        return
    }

    activeUpdateRoute?.let { route ->
        ProfileUpdatesScreen(
            section = ProfileUpdateSection.fromRoute(route),
            onBack = { activeUpdateRoute = null }
        )
        return
    }

    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val login = normalizedGitHubLogin(displayedProfile?.login)

    LaunchedEffect(mode, login) {
        login?.let(onInspectProfile)
    }

    val openFullProfile = login?.let { normalizedLogin ->
        { onInspectProfile(normalizedLogin) }
    }

    val libraryItems = listOf(
        ProfileMenuItem(Icons.Default.Star, "Stars", "Your starred repositories from GitHub", { activeLibraryRoute = ProfileLibrarySection.Stars.route }),
        ProfileMenuItem(Icons.Default.Favorite, "Favourites", "Repositories pinned inside GitHub Rock", { activeLibraryRoute = ProfileLibrarySection.Favourites.route }),
        ProfileMenuItem(Icons.Default.History, "Recently viewed", "Repositories you have opened on this device", { activeLibraryRoute = ProfileLibrarySection.RecentlyViewed.route })
    )
    val updateItems = listOf(
        ProfileMenuItem(Icons.Default.AutoAwesome, "What's new", "Highlights from recent GitHub Rock updates", { activeUpdateRoute = ProfileUpdateSection.WhatsNew.route }),
        ProfileMenuItem(Icons.Default.Announcement, "Announcements", "Security, account, and important app notices", { activeUpdateRoute = ProfileUpdateSection.Announcements.route })
    )
    val appItems = listOf(
        ProfileMenuItem(Icons.Default.Tune, "Tweaks", "App settings, theme, network, and display", onOpenSettings),
        ProfileMenuItem(Icons.Default.Settings, "GitHub settings", "Account, security, notifications, and applications", onOpenSettings),
        ProfileMenuItem(Icons.Default.AutoAwesome, "GitHub features", "Native, connected, and roadmap capabilities", onOpenFeatures),
        ProfileMenuItem(Icons.Default.Download, "Downloads", "Applications, artifacts, files, and APK safety", onOpenDownloads),
        ProfileMenuItem(Icons.Default.Info, "About", "Version, Android capabilities, community, and legal", onOpenAppInfo)
    )
    val accountItems = listOf(
        ProfileMenuItem(Icons.Default.AccountCircle, "Accounts & organizations", "Connected account, organizations, and public profiles", onOpenAccounts),
        ProfileMenuItem(
            Icons.Default.Logout,
            if (mode == AppMode.Connected) "Logout" else "Exit ${mode.name.lowercase()}",
            if (mode == AppMode.Connected) "Sign out safely from this device" else "Close the current ${mode.name.lowercase()} session",
            { showLogoutSheet = true },
            destructive = true
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
        item { CompactProfileCard(displayedProfile, mode, openFullProfile, onOpenRepositories, onOpenFollowers, onOpenFollowing) }
        if (explorerState.loading) {
            item { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) } }
        }
        explorerState.error?.let { message ->
            item { Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) { Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
        }
        item { ProfileMenuGroup("Library", libraryItems) }
        item { ProfileMenuGroup("Updates", updateItems) }
        item { ProfileMenuGroup("App", appItems) }
        item { ProfileMenuGroup("Account", accountItems) }
    }

    if (showLogoutSheet) {
        LogoutConfirmationSheet(
            displayName = displayedProfile?.name,
            login = displayedProfile?.login,
            avatarUrl = displayedProfile?.avatarUrl,
            connected = mode == AppMode.Connected,
            onDismiss = { showLogoutSheet = false },
            onConfirm = {
                showLogoutSheet = false
                onLogout()
            }
        )
    }
}

@Composable
private fun CompactProfileCard(
    profile: GitHubUser?,
    mode: AppMode,
    onClick: (() -> Unit)?,
    onOpenRepositories: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(model = profile?.avatarUrl, contentDescription = "${profile?.login} avatar", modifier = Modifier.size(82.dp).clip(MaterialTheme.shapes.extraLarge))
                } else {
                    Surface(Modifier.size(82.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(profile?.login?.take(2)?.uppercase(Locale.getDefault()) ?: "GR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(profile?.name ?: profile?.login ?: "GitHub Rock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile?.login?.let { "@$it" } ?: mode.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    profile?.bio?.takeIf(String::isNotBlank)?.let { bio -> Spacer(Modifier.height(4.dp)); Text(bio, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompactProfileMetric(profile?.publicRepos ?: 0, "Repos", Modifier.weight(1f), onOpenRepositories)
                MetricDivider()
                CompactProfileMetric(profile?.followers ?: 0, "Followers", Modifier.weight(1f), onOpenFollowers)
                MetricDivider()
                CompactProfileMetric(profile?.following ?: 0, "Following", Modifier.weight(1f), onOpenFollowing)
            }
        }
    }
}

@Composable
private fun CompactProfileMetric(value: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricDivider() {
    Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun ProfileMenuGroup(title: String, items: List<ProfileMenuItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                items.forEachIndexed { index, item ->
                    ProfileMenuRow(item)
                    if (index < items.lastIndex) HorizontalDivider(Modifier.padding(start = 74.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(item: ProfileMenuItem) {
    val accent = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconTint = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBackground = if (item.destructive) MaterialTheme.colorScheme.error.copy(alpha = .08f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(horizontal = 16.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.extraLarge, color = iconBackground) { Box(contentAlignment = Alignment.Center) { Icon(item.icon, contentDescription = null, tint = iconTint) } }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, color = accent, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!item.destructive) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
