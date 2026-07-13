package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    followers: List<Owner>,
    following: List<Owner>,
    socialLoading: Boolean,
    socialError: String?,
    onRetrySocial: () -> Unit,
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
                    Column {
                        Text(
                            profile?.name ?: when (mode) {
                                AppMode.Guest -> "Guest"
                                AppMode.Demo -> "Demo profile"
                                AppMode.Connected -> profile?.login.orEmpty()
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            profile?.login?.let { "@$it" } ?: "Public access only",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("GitHub community", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStat("Public repos", profile?.publicRepos ?: 0, Icons.Default.Folder, Modifier.weight(1f))
                        ProfileStat("Followers", profile?.followers ?: 0, Icons.Default.People, Modifier.weight(1f))
                        ProfileStat("Following", profile?.following ?: 0, Icons.Default.PersonAdd, Modifier.weight(1f))
                    }

                    when {
                        mode == AppMode.Guest -> Text(
                            "Connect a GitHub account to view followers and following.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        followers.isEmpty() && following.isEmpty() && socialLoading -> {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(
                                "Loading GitHub followers…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        followers.isEmpty() && following.isEmpty() && socialError != null -> {
                            Text(socialError, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onRetrySocial) { Text("Retry followers") }
                        }
                        else -> {
                            SocialAccounts("Followers", followers)
                            SocialAccounts("Following", following)
                            if (socialLoading) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            } else if (socialError != null) {
                                Text(socialError, color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = onRetrySocial) { Text("Retry followers") }
                            }
                        }
                    }
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
                Spacer(Modifier.width(8.dp))
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

@Composable
private fun ProfileStat(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun SocialAccounts(title: String, users: List<Owner>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        if (users.isEmpty()) {
            Text(
                "No accounts to show.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(users.take(SOCIAL_PREVIEW_LIMIT), key = { it.login }) { user ->
                    Column(
                        modifier = Modifier.width(72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                        ) {
                            if (user.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "@${user.login}",
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    Text(user.login.take(2).uppercase())
                                }
                            }
                        }
                        Text(
                            "@${user.login}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private const val SOCIAL_PREVIEW_LIMIT = 12
