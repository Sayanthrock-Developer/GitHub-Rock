package com.sayanthrock.githubrock.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onOpenFeatures: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Profile", style = MaterialTheme.typography.headlineSmall) }
        item {
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileAvatar(profile)
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile?.name ?: when (mode) {
                                AppMode.Guest -> "Guest"
                                AppMode.Demo -> "Demo profile"
                                AppMode.Connected -> profile?.login.orEmpty()
                            },
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            profile?.login?.let { "@$it" } ?: "Public access only",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        profile?.bio?.takeIf(String::isNotBlank)?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        if (mode != AppMode.Guest) {
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp).size(24.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Public repositories", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Repositories visible on this GitHub profile",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            (profile?.publicRepos ?: 0).toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenFeatures
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Explore all GitHub features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Preview repositories, code, collaboration, Actions, Android builds, releases, security, and the product roadmap.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Open GitHub features preview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.tertiary)
                    Column {
                        Text("Security", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tokens are encrypted with an Android Keystore-backed master key and excluded from backups.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.size(8.dp))
                Text(if (mode == AppMode.Connected) "Log out and delete token" else "Exit ${mode.name.lowercase()} mode")
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: GitHubUser?) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
    ) {
        if (!profile?.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = profile?.avatarUrl,
                contentDescription = "GitHub avatar",
                modifier = Modifier.size(72.dp)
            )
        } else {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Text(profile?.login?.take(2)?.uppercase() ?: "GR")
            }
        }
    }
}
