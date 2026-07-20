package com.sayanthrock.githubrock.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.GITHUB_ACCOUNT_SECURITY_URL
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val details = explorerState.snapshot?.details
    val connectedLogin = normalizedGitHubLogin(displayedProfile?.login)
    val ownLogin = normalizedGitHubLogin(profile?.login)
    val profileUrl = connectedLogin?.let { "https://github.com/$it" }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var requestedOwnProfile by rememberSaveable(ownLogin) { mutableStateOf(false) }
    var exportMessage by remember(displayedProfile?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(mode, ownLogin) {
        if (!requestedOwnProfile && ownLogin != null) {
            requestedOwnProfile = true
            onInspectProfile(ownLogin)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val user = displayedProfile
        if (uri == null || user == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(ProfileExportFormatter.toJson(user))
                    } ?: error("Unable to create the profile file")
                }
            }.onSuccess {
                exportMessage = "Profile downloaded successfully"
            }.onFailure { error ->
                exportMessage = error.message ?: "Unable to download this profile"
            }
        }
    }

    val openExternal: (String) -> Unit = { url ->
        if (url.startsWith("https://github.com/") || url.startsWith("https://gist.github.com/")) {
            onOpenGitHubUrl(url)
        } else {
            runCatching { uriHandler.openUri(url) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Profile",
                subtitle = when (mode) {
                    AppMode.Connected -> "Identity, activity, controls, and account"
                    AppMode.Guest -> "Public browsing session"
                    AppMode.Demo -> "Isolated demonstration workspace"
                }
            )
        }

        if (explorerState.loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                }
            }
        }

        explorerState.error?.let { message ->
            item { GlassCard { Text(message, color = MaterialTheme.colorScheme.error) } }
        }

        item {
            ProfileCommandHero(
                mode = mode,
                profile = displayedProfile,
                onOpenProfile = profileUrl?.let { url -> { onOpenGitHubUrl(url) } },
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

        if (displayedProfile != null) {
            item {
                IdentityCard(
                    profile = displayedProfile,
                    details = details,
                    onOpenLink = openExternal
                )
            }
        }

        details?.contributionsLastYear?.let { total ->
            item { ActivityCard(total = total, details = details, profileUrl = profileUrl, onOpenGitHubUrl = onOpenGitHubUrl) }
        }

        item { StandardSectionHeader("Control deck") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ControlTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Tune,
                        title = "Settings",
                        subtitle = "Theme, feature switches, density, motion",
                        badge = "ALL CONTROLS",
                        onClick = onOpenSettings
                    )
                    ControlTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        title = "Downloads",
                        subtitle = "Artifacts, releases, APK inspection",
                        badge = "FILES",
                        onClick = onOpenDownloads
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ControlTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Info,
                        title = "App & SDK",
                        subtitle = "Version, Android APIs, installation",
                        badge = "SYSTEM",
                        onClick = onOpenAppInfo
                    )
                    ControlTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Code,
                        title = "GitHub services",
                        subtitle = "Profile, Gists, organizations, plans",
                        badge = "SERVICES",
                        onClick = onOpenFeatures
                    )
                }
            }
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

        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Advanced tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Security, export, and feature status",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide" else "Show")
                        }
                    }

                    if (showAdvanced) {
                        StandardSettingsGroup {
                            StandardSettingsRow(
                                icon = Icons.Default.Lock,
                                title = "GitHub security",
                                subtitle = "Passkeys, two-factor authentication, and sessions",
                                onClick = { onOpenGitHubUrl(GITHUB_ACCOUNT_SECURITY_URL) }
                            )
                            if (displayedProfile != null) {
                                StandardSettingsDivider()
                                StandardSettingsRow(
                                    icon = Icons.Default.Download,
                                    title = "Download profile",
                                    subtitle = "Save public account details as JSON",
                                    onClick = {
                                        exportMessage = null
                                        exportLauncher.launch(ProfileExportFormatter.fileName(displayedProfile))
                                    }
                                )
                            }
                            StandardSettingsDivider()
                            StandardSettingsRow(
                                icon = Icons.Default.Info,
                                title = "Feature status",
                                subtitle = "Native coverage and roadmap",
                                onClick = onOpenFeatures
                            )
                        }
                    }
                }
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
private fun ProfileCommandHero(
    mode: AppMode,
    profile: GitHubUser?,
    onOpenProfile: (() -> Unit)?,
    onOpenRepositories: (() -> Unit)?,
    onOpenFollowers: (() -> Unit)?,
    onOpenFollowing: (() -> Unit)?
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "Profile avatar",
                        modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.extraLarge)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(76.dp),
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
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.name ?: profile?.login ?: "GitHub Rock",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: mode.name,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    profile?.bio?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill("Repositories", profile?.publicRepos ?: 0, onOpenRepositories, Modifier.weight(1f))
                MetricPill("Followers", profile?.followers ?: 0, onOpenFollowers, Modifier.weight(1f))
                MetricPill("Following", profile?.following ?: 0, onOpenFollowing, Modifier.weight(1f))
            }

            if (onOpenProfile != null) {
                Button(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("View profile on GitHub", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.heightIn(min = 74.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IdentityCard(
    profile: GitHubUser,
    details: GitHubProfileDetails?,
    onOpenLink: (String) -> Unit
) {
    val localTime = remember {
        ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a · z", Locale.getDefault()))
    }
    val fields = buildList {
        profile.company?.takeIf { it.isNotBlank() }?.let { add("Company" to it) }
        profile.location?.takeIf { it.isNotBlank() }?.let { add("Location" to it) }
        details?.pronouns?.takeIf { it.isNotBlank() }?.let { add("Pronouns" to it) }
        profile.email?.takeIf { it.isNotBlank() }?.let { add("Email" to it) }
        add("Local time" to localTime)
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            fields.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            profile.blog?.takeIf { it.isNotBlank() }?.let { blog ->
                TextButton(onClick = { onOpenLink(if (blog.startsWith("http")) blog else "https://$blog") }) {
                    Text(blog, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            details?.orcid?.let { orcid ->
                OutlinedButton(onClick = { onOpenLink(orcid.url) }, modifier = Modifier.fillMaxWidth()) {
                    Text("ORCID · ${orcid.displayName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    total: Int,
    details: GitHubProfileDetails,
    profileUrl: String?,
    onOpenGitHubUrl: (String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$total contributions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "Activity recorded during the last year",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (details.organizationCount > 0) {
                Text("${details.organizationCount} organizations", fontWeight = FontWeight.SemiBold)
            }
            if (details.highlights.isNotEmpty()) {
                Text(details.highlights.joinToString(" · "), color = MaterialTheme.colorScheme.primary)
                profileUrl?.let { url ->
                    TextButton(onClick = { onOpenGitHubUrl(url) }) {
                        Text("View achievements on GitHub")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 142.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
