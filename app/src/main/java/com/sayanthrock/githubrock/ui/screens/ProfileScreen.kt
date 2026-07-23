package com.sayanthrock.githubrock.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.SaveAlt
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Retained for source compatibility with existing previews and tests. */
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
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenTweaks: () -> Unit = {},
    onOpenLibrary: (ProfileLibrarySection) -> Unit = {},
    onOpenUpdates: (ProfileUpdateSection) -> Unit = {},
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit,
    dashboardStateOverride: ConnectedProfileDashboardUiState? = null
) {
    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val login = normalizedGitHubLogin(displayedProfile?.login)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember(displayedProfile?.id) { mutableStateOf<String?>(null) }
    var pendingExportProfile by remember { mutableStateOf<GitHubUser?>(null) }

    LaunchedEffect(mode, login) {
        login?.let(onInspectProfile)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val target = pendingExportProfile
        pendingExportProfile = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(ProfileExportFormatter.toJson(target))
                    } ?: error("Unable to create the profile file")
                }
            }.onSuccess {
                exportMessage = "Profile downloaded successfully"
            }.onFailure { problem ->
                exportMessage = problem.message ?: "Unable to download this profile"
            }
        }
    }

    val openFullProfile = login?.let { normalizedLogin ->
        { onOpenGitHubUrl("https://github.com/$normalizedLogin?tab=repositories") }
    }

    val libraryItems = listOf(
        ProfileMenuItem(
            icon = Icons.Default.Star,
            title = "Stars",
            subtitle = "Your starred repositories from GitHub",
            onClick = { onOpenLibrary(ProfileLibrarySection.Stars) }
        ),
        ProfileMenuItem(
            icon = Icons.Default.Favorite,
            title = "Favourites",
            subtitle = "Repositories pinned inside GitHub Rock",
            onClick = { onOpenLibrary(ProfileLibrarySection.Favourites) }
        ),
        ProfileMenuItem(
            icon = Icons.Default.History,
            title = "Recently viewed",
            subtitle = "Repositories you have opened on this device",
            onClick = { onOpenLibrary(ProfileLibrarySection.RecentlyViewed) }
        )
    )

    val updateItems = listOf(
        ProfileMenuItem(
            icon = Icons.Default.AutoAwesome,
            title = "What's new",
            subtitle = "Highlights from recent GitHub Rock updates",
            onClick = { onOpenUpdates(ProfileUpdateSection.WhatsNew) }
        ),
        ProfileMenuItem(
            icon = Icons.Default.Announcement,
            title = "Announcements",
            subtitle = "Security, account, and important app notices",
            onClick = { onOpenUpdates(ProfileUpdateSection.Announcements) }
        )
    )

    val appItems = listOf(
        ProfileMenuItem(
            icon = Icons.Default.Tune,
            title = "Tweaks",
            subtitle = "Appearance, theme, display, and app behaviour",
            onClick = onOpenTweaks
        ),
        ProfileMenuItem(
            icon = Icons.Default.Settings,
            title = "GitHub settings",
            subtitle = "Account, security, notifications, and applications",
            onClick = onOpenSettings
        ),
        ProfileMenuItem(
            icon = Icons.Default.Download,
            title = "Downloads",
            subtitle = "Applications, artifacts, files, and APK safety",
            onClick = onOpenDownloads
        ),
        ProfileMenuItem(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "Version, Android capabilities, community, and legal",
            onClick = onOpenAppInfo
        )
    )

    val accountItems = buildList {
        add(
            ProfileMenuItem(
                icon = Icons.Default.AccountCircle,
                title = "Accounts & organizations",
                subtitle = "Connected account, organizations, and public profiles",
                onClick = onOpenFeatures
            )
        )
        displayedProfile?.let { target ->
            add(
                ProfileMenuItem(
                    icon = Icons.Default.SaveAlt,
                    title = "Download profile",
                    subtitle = "Save this public profile as a JSON file",
                    onClick = {
                        exportMessage = null
                        pendingExportProfile = target
                        exportLauncher.launch(ProfileExportFormatter.fileName(target))
                    }
                )
            )
        }
        add(
            ProfileMenuItem(
                icon = Icons.Default.Logout,
                title = if (mode == AppMode.Connected) "Logout" else "Exit ${mode.name.lowercase()} mode",
                subtitle = if (mode == AppMode.Connected) {
                    "Remove the connected GitHub session from this device"
                } else {
                    "Close the current ${mode.name.lowercase()} session"
                },
                onClick = onLogout,
                destructive = true
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        item {
            CompactProfileCard(
                profile = displayedProfile,
                mode = mode,
                onClick = openFullProfile
            )
        }

        if (explorerState.loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }

        explorerState.error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item { ProfileMenuGroup(title = "Library", items = libraryItems) }
        item { ProfileMenuGroup(title = "Updates", items = updateItems) }
        item { ProfileMenuGroup(title = "App", items = appItems) }
        item { ProfileMenuGroup(title = "Account", items = accountItems) }

        exportMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = if (message.contains("success", ignoreCase = true)) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = if (message.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactProfileCard(
    profile: GitHubUser?,
    mode: AppMode,
    onClick: (() -> Unit)?
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "${profile?.login} avatar",
                        modifier = Modifier.size(82.dp).clip(MaterialTheme.shapes.extraLarge)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(82.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                profile?.login?.take(2)?.uppercase(Locale.getDefault()) ?: "GR",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        profile?.name ?: profile?.login ?: "GitHub Rock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: mode.name,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    profile?.bio?.takeIf(String::isNotBlank)?.let { bio ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            bio,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactProfileMetric(
                    value = profile?.publicRepos ?: 0,
                    label = "Repos",
                    modifier = Modifier.weight(1f)
                )
                MetricDivider()
                CompactProfileMetric(
                    value = profile?.followers ?: 0,
                    label = "Followers",
                    modifier = Modifier.weight(1f)
                )
                MetricDivider()
                CompactProfileMetric(
                    value = profile?.following ?: 0,
                    label = "Following",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactProfileMetric(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun ProfileMenuGroup(title: String, items: List<ProfileMenuItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    ProfileMenuRow(item)
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(item: ProfileMenuItem) {
    val accent = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconTint = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBackground = if (item.destructive) {
        MaterialTheme.colorScheme.error.copy(alpha = .08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = iconTint)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                item.title,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!item.destructive) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
