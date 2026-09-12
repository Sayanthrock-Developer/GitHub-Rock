package com.sayanthrock.githubrock.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.vector

/** Native capability hub. It deliberately reports unsupported/permission-gated work instead of exposing fake actions. */
@Composable
fun GitHubToolsScreen(
    onBack: () -> Unit,
    onOpenRepositories: () -> Unit,
    onOpenBuilds: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenProfile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(RockIcon.Back.vector(), contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text("GitHub Tools", style = MaterialTheme.typography.headlineMedium)
                    Text("Real capabilities available in GitHub Rock", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capability status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Available means a native user path exists. Permission required, Partial, and Unsupported states are shown explicitly instead of behaving like fake buttons.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        GitHubCapability.Group.entries.forEach { group ->
            item {
                Text(group.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            }
            items(GitHubCapabilityRegistry.all.filter { it.group == group }, key = { it.name }) { capability ->
                CapabilityRow(capability)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Open existing native workspaces", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenRepositories) { Text("Repositories") }
                    TextButton(onClick = onOpenBuilds) { Text("Builds / Actions") }
                    TextButton(onClick = onOpenAccounts) { Text("Accounts & organizations") }
                    TextButton(onClick = onOpenProfile) { Text("Profile") }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(capability: GitHubCapability) {
    val stateLabel = when (capability.state) {
        GitHubCapabilityState.Available -> "Available"
        GitHubCapabilityState.PermissionRequired -> "Permission required"
        GitHubCapabilityState.Partial -> "Partial"
        GitHubCapabilityState.Unsupported -> "Not supported"
        GitHubCapabilityState.Unauthenticated -> "Sign in required"
        GitHubCapabilityState.Error -> "Unavailable"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(capability.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(stateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            capability.reason?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
