package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.navigation.GitHubWebSection
import com.sayanthrock.githubrock.core.navigation.filterGitHubWebSections
import com.sayanthrock.githubrock.core.navigation.githubWebSections
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSettingsScreen(
    login: String?,
    onOpenAppSettings: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sections = remember(login) { githubWebSections(login) }
    val visibleSections = remember(sections, query) { filterGitHubWebSections(sections, query) }
    val totalDestinations = sections.sumOf { it.destinations.size }
    val visibleDestinations = visibleSections.sumOf { it.destinations.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("GitHub settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GitHubSettingsHero(totalDestinations)
            }

            item {
                StandardSectionHeader(
                    title = "GitHub Rock",
                    supporting = "On-device"
                )
            }
            item {
                StandardSettingsGroup {
                    StandardSettingsRow(
                        icon = Icons.Default.Palette,
                        title = "App appearance & interface",
                        subtitle = "Themes, colors, text, display size, loading, code, and logs",
                        onClick = onOpenAppSettings
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Find a GitHub setting or tool") },
                    placeholder = { Text("Security, notifications, tokens, billing…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    if (query.isBlank()) {
                        "$totalDestinations GitHub destinations available"
                    } else {
                        "$visibleDestinations of $totalDestinations destinations"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            visibleSections.forEach { section ->
                item(key = "header-${section.id}") {
                    StandardSectionHeader(section.title)
                }
                item(key = section.id) {
                    GitHubSettingsSection(
                        section = section,
                        onOpenGitHubUrl = onOpenGitHubUrl
                    )
                }
            }

            if (visibleSections.isEmpty()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("No matching GitHub setting", fontWeight = FontWeight.Bold)
                            Text(
                                "Try a shorter search such as security, apps, repositories, or billing.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubSettingsHero(totalDestinations: Int) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Everything in one place",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Customers can open account, repositories, security, apps, tokens, notifications, billing, accessibility, Copilot, organizations, and every other supported GitHub destination.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                "$totalDestinations official GitHub destinations",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Native tools stay inside GitHub Rock. Website-only account and payment pages open securely on GitHub.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GitHubSettingsSection(
    section: GitHubWebSection,
    onOpenGitHubUrl: (String) -> Unit
) {
    StandardSettingsGroup {
        section.destinations.forEachIndexed { index, destination ->
            StandardSettingsRow(
                icon = iconForSection(section.id),
                title = destination.title,
                subtitle = destination.description,
                onClick = { onOpenGitHubUrl(destination.url) },
                trailing = {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open ${destination.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            if (index < section.destinations.lastIndex) StandardSettingsDivider()
        }
    }
}

private fun iconForSection(sectionId: String): ImageVector = when (sectionId) {
    "security" -> Icons.Default.Security
    "create-code", "automate-extend" -> Icons.Default.Code
    "your-github", "plan-collaborate" -> Icons.Default.AccountCircle
    else -> Icons.Default.Settings
}
