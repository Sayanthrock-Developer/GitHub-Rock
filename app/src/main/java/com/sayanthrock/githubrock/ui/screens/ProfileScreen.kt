package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    onOpenRepositories: () -> Unit,
    onOpenFeatures: () -> Unit,
    onLogout: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val profileUrl = profile?.login?.let { "https://github.com/$it" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item { ProfileHero(mode = mode, profile = profile) }

        if (profileUrl != null) {
            item {
                Button(
                    onClick = { uriHandler.openUri(profileUrl) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Follow on GitHub", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }

        item { ProfileSectionTitle("Library") }
        item {
            ProfileMenuGroup {
                ProfileMenuItem(
                    icon = Icons.Default.Folder,
                    title = "Repository library",
                    subtitle = "Browse projects connected to this profile",
                    onClick = onOpenRepositories
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Code,
                    title = "Developer workspace",
                    subtitle = "Builds, releases and repository tools",
                    onClick = onOpenFeatures
                )
            }
        }

        item { ProfileSectionTitle("Developer") }
        item {
            ProfileMenuGroup {
                ProfileMenuItem(
                    icon = Icons.Default.Code,
                    title = "All GitHub services",
                    subtitle = "Notifications, Codespaces, settings, Marketplace and more",
                    onClick = onOpenFeatures
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Security",
                    subtitle = "Keystore-encrypted account credentials",
                    onClick = {}
                )
            }
        }

        item { ProfileSectionTitle("Account") }
        item {
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                ProfileMenuItem(
                    icon = Icons.Default.Logout,
                    title = if (mode == AppMode.Connected) "Log out and delete token" else "Exit ${mode.name.lowercase()} mode",
                    subtitle = "Remove this session from the device",
                    accent = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun ProfileHero(mode: AppMode, profile: GitHubUser?) {
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
                    ProfileStat(profile.publicRepos, "Repositories", Modifier.weight(1f))
                    StatDivider()
                    ProfileStat(profile.followers, "Followers", Modifier.weight(1f))
                    StatDivider()
                    ProfileStat(profile.following, "Following", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: GitHubUser?) {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (!profile?.avatarUrl.isNullOrBlank()) {
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
private fun ProfileStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
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
private fun ProfileSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun ProfileMenuGroup(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column(content = content)
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.large,
            color = tint.copy(alpha = .12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (accent) tint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
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
