package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun DownloadsScreen() {
    Column(
        Modifier.fillMaxSize().padding(18.dp).padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Verified artifact pipeline", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Artifacts downloaded from a repository are recovered by WorkManager, hashed with SHA-256 and inspected before Android's system installer opens.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Download, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                Text("Download an Actions artifact or release APK to see it here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

