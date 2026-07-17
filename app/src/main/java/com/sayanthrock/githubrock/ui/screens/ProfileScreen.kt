package com.sayanthrock.githubrock.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.GITHUB_ACCOUNT_SECURITY_URL
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    onOpenRepositories: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val connectedLogin = if (mode == AppMode.Connected) {
        normalizedGitHubLogin(profile?.login)
    } else {
        null
    }
    val profileUrl = connectedLogin?.let { "https://github.com/$it" }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExport by remember(profile?.id) { mutableStateOf<String?>(null) }
    var exportMessage by remember(profile?.id) { mutableStateOf<String?>(null) }

    val profileExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExport
        pendingExport = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(content)
                    } ?: error("Unable to create the profile file")
                }
            }.onSuccess {
                exportMessage = "Profile downloaded successfully"
            }.onFailure { error ->
                exportMessage = error.message ?: "Unable to download this profile"
            }
        }
    }

    val downloadProfile: () -> Unit = {
        profile?.let { user ->
            pendingExport = ProfileExportFormatter.toJson(user)
            exportMessage = null
            profileExportLauncher.launch(ProfileExportFormatter.fileName(user))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Profile",
                subtitle = when (mode) {
                    AppMode.Connected -> "Your GitHub account and app preferences"
                    AppMode.Guest -> "Public browsing session"
                    AppMode.Demo -> "Isolated demonstration workspace"
                }
            )
        }

        item {
            ProfileHero(
                mode = mode,
                profile = profile,
                onOpenRepositories = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=repositories") }
                },
                onOpenFollowers = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=followers") }
                },
                onOpenFollowing = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=following") }
                }
            )
        }

        exportMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        message,
                        color = if (message.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (profileUrl != null) {
            item {
                Button(
                    onClick = { onOpenGitHubUrl(profileUrl) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("View on GitHub", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                }
            }
        }

        item { StandardSectionHeader("Library") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Repository library",
                    subtitle = "Browse projects connected to this profile",
                    onClick = onOpenRepositories
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "Artifacts, release files and APK inspection",
                    onClick = onOpenDownloads
                )
                if (profile != null) {
                    StandardSettingsDivider()
                    StandardSettingsRow(
                        icon = Icons.Default.Download,
                        title = "Download profile",
                        subtitle = "Save account details, links, and public statistics as JSON",
                        onClick = downloadProfile
                    )
                }
            }
        }

        item { StandardSectionHeader("Workspace") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Code,
                    title = "All GitHub services",
                    subtitle = "Notifications, Codespaces, settings, Marketplace and more",
                    onClick = onOpenFeatures
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Lock,
                    title = "GitHub security",
                    subtitle = "Passkeys, two-factor authentication and sessions",
                    onClick = { onOpenGitHubUrl(GITHUB_ACCOUNT_SECURITY_URL) }
                )
            }
        }

        item { StandardSectionHeader("App") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Theme style, images, accent, dynamic color and true black",
                    onClick = onOpenAppearance
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Info,
                    title = "About and feature status",
                    subtitle = "Native coverage, web tools and roadmap",
                    onClick = onOpenFeatures
                )
            }
        }

        item { StandardSectionHeader("Account") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Logout,
                    title = if (mode == AppMode.Connected) "Log out and delete token" else "Exit ${mode.name.lowercase()} mode",
                    subtitle = "Remove this session from the device",
                    destructive = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun ProfileHero(
    mode: AppMode,
    profile: GitHubUser?,
    onOpenRepositories: (() -> Unit)?,
    onOpenFollowers: (() -> Unit)?,
    onOpenFollowing: (() -> Unit)?
) {
    GlassCard(contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(profile)
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.name ?: when (mode) {
                            AppMode.Guest -> "Guest"
                            AppMode.Demo -> "Demo profile"
                            AppMode.Connected -> profile?.login.orEmpty()
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: "Public access only",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            profile?.bio?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (profile != null && (!profile.location.isNullOrBlank() || !profile.blog.isNullOrBlank())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    profile.location?.takeIf(String::isNotBlank)?.let {
                        ProfileFact("Location", it, Modifier.weight(1f))
                    }
                    profile.blog?.takeIf(String::isNotBlank)?.let {
                        ProfileFact("Website", it, Modifier.weight(1f))
                    }
                }
            }

            if (mode != AppMode.Guest && profile != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(profile.publicRepos, "Repositories", Modifier.weight(1f), onOpenRepositories)
                    StatDivider()
                    ProfileStat(profile.followers, "Followers", Modifier.weight(1f), onOpenFollowers)
                    StatDivider()
                    ProfileStat(profile.following, "Following", Modifier.weight(1f), onOpenFollowing)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: GitHubUser?) {
    val showImages = LocalRemoteImagesEnabled.current
    Surface(
        modifier = Modifier.size(80.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (showImages && !profile?.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = profile?.avatarUrl,
                contentDescription = "GitHub avatar",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    profile?.login?.take(2)?.uppercase() ?: "GR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onClick)
    }
    Column(
        modifier = modifier.then(interactionModifier).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun StatDivider() {
    Surface(
        modifier = Modifier.width(1.dp).height(38.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    ) {}
}

@Composable
private fun ProfileFact(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
