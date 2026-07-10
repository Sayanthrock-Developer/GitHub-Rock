package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun ProfileScreen(mode: AppMode, profile: GitHubUser?, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(18.dp).padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(profile?.avatarUrl, contentDescription = "GitHub avatar", Modifier.size(72.dp))
                    } else {
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Text(profile?.login?.take(2)?.uppercase() ?: "GR") }
                    }
                }
                Column {
                    Text(profile?.name ?: when (mode) { AppMode.Guest -> "Guest"; AppMode.Demo -> "Demo profile"; AppMode.Connected -> profile?.login.orEmpty() }, style = MaterialTheme.typography.titleLarge)
                    Text(profile?.login?.let { "@$it" } ?: "Public access only", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.tertiary)
                Column {
                    Text("Security", style = MaterialTheme.typography.titleMedium)
                    Text("Tokens are encrypted with an Android Keystore-backed master key and excluded from backups.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text(if (mode == AppMode.Connected) "Log out and delete token" else "Exit ${mode.name.lowercase()} mode")
        }
    }
}

