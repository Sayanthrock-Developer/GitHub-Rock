package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSettingsScreen(
    profile: GitHubUser?,
    onOpenProfile: (String) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onBack: () -> Unit,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    var showBuildSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Account settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProfileHeader(profile = profile, onOpen = { profile?.login?.let(onOpenProfile) }) }

            item { StandardSectionHeader("Public Profile") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.Person, "Profile", profile?.let { "${it.name ?: it.login} · @${it.login}" } ?: "Sign in to load your GitHub profile") {
                        profile?.login?.let(onOpenProfile)
                    }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.AccountCircle, "Profile fields", profileSummary(profile)) {
                        profile?.htmlUrl?.takeIf(String::isNotBlank)?.let(onOpenGitHubUrl)
                    }
                }
            }

            item { StandardSectionHeader("Account") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.AccountCircle, "Username", profile?.let { "@${it.login}" } ?: "Not signed in") { }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.Person, "Email", profile?.email ?: "Not available from the current GitHub response") { }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.Settings, "Connected GitHub accounts", "Add, switch, and remove accounts") { onOpenAccounts() }
                }
            }

            item { StandardSectionHeader("Accounts") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.AccountCircle, "Multiple GitHub accounts", "Manage signed-in identities and active account") { onOpenAccounts() }
                }
            }

            item { StandardSectionHeader("Builds & Actions") }
            item {
                StandardSettingsGroup {
                    SettingsRow(
                        Icons.Default.Build,
                        "Builds & Actions",
                        "Workflow preview, job details, status colors, controls, and repository selection"
                    ) { showBuildSettings = true }
                }
            }

            item { StandardSectionHeader("Privacy") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.PrivacyTip, "GitHub privacy settings", "Open supported GitHub privacy controls") {
                        onOpenGitHubUrl("https://github.com/settings/profile")
                    }
                }
            }

            item { StandardSectionHeader("Notifications") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.Notifications, "GitHub notifications", "Open notifications and notification preferences") {
                        onOpenGitHubUrl("https://github.com/notifications")
                    }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.Notifications, "Notification preferences", "GitHub controls are managed by GitHub") {
                        onOpenGitHubUrl("https://github.com/settings/notifications")
                    }
                }
            }

            item { StandardSectionHeader("Security") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.Security, "Authentication & sessions", "Manage GitHub sessions and authorized access") {
                        onOpenGitHubUrl("https://github.com/settings/security")
                    }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.Lock, "Re-authentication", "Sensitive actions continue to use the app's secure authentication flow") { }
                }
            }

            item { StandardSectionHeader("Appearance") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.Palette, "Theme & interface", "Theme, navigation bar style, and display preferences") { onOpenAppearance() }
                }
            }

            item { StandardSectionHeader("Data & Storage") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.CloudDownload, "Downloads & library", "Downloaded releases, artifacts, and APKs") { onOpenDownloads() }
                    StandardSettingsDivider()
                    SettingsRow(Icons.Default.Storage, "Cache & local data", "Manage app-local cached data and storage") { onOpenDownloads() }
                }
            }

            item { StandardSectionHeader("About") }
            item {
                StandardSettingsGroup {
                    SettingsRow(Icons.Default.Info, "GitHub Rock", "Version, licenses, and open-source information") { onOpenAbout() }
                }
            }

            item { StandardSectionHeader("Sign Out") }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Selected account", fontWeight = FontWeight.Bold)
                        Text(profile?.let { "@${it.login}" } ?: "No authenticated account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Account sign-out is managed from the account switcher so other connected accounts can remain signed in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(4.dp))
                            Text("Open Accounts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showBuildSettings) {
        BuildSettingsDialog(
            workflowPreview = appearance.workflowPreview,
            workflowStepDetails = appearance.workflowStepDetails,
            statusColors = appearance.statusColors,
            actionsControls = appearance.actionsControls,
            repositoryManager = appearance.repositoryManager,
            fileTools = appearance.fileTools,
            compactCards = appearance.compactCards,
            onWorkflowPreview = appearanceViewModel::setWorkflowPreview,
            onWorkflowStepDetails = appearanceViewModel::setWorkflowStepDetails,
            onStatusColors = appearanceViewModel::setStatusColors,
            onActionsControls = appearanceViewModel::setActionsControls,
            onRepositoryManager = appearanceViewModel::setRepositoryManager,
            onFileTools = appearanceViewModel::setFileTools,
            onCompactCards = appearanceViewModel::setCompactCards,
            onDismiss = { showBuildSettings = false }
        )
    }
}

@Composable
private fun BuildSettingsDialog(
    workflowPreview: Boolean,
    workflowStepDetails: Boolean,
    statusColors: Boolean,
    actionsControls: Boolean,
    repositoryManager: Boolean,
    fileTools: Boolean,
    compactCards: Boolean,
    onWorkflowPreview: (Boolean) -> Unit,
    onWorkflowStepDetails: (Boolean) -> Unit,
    onStatusColors: (Boolean) -> Unit,
    onActionsControls: (Boolean) -> Unit,
    onRepositoryManager: (Boolean) -> Unit,
    onFileTools: (Boolean) -> Unit,
    onCompactCards: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Builds & Actions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BuildSettingSwitch("Workflow preview", "Show workflow source and health information", workflowPreview, onWorkflowPreview)
                BuildSettingSwitch("Job & step details", "Show jobs and expandable step information", workflowStepDetails, onWorkflowStepDetails)
                BuildSettingSwitch("Status colors", "Use success, failure, and running status accents", statusColors, onStatusColors)
                BuildSettingSwitch("Actions controls", "Allow refresh, dispatch, cancel, and re-run controls", actionsControls, onActionsControls)
                BuildSettingSwitch("Repository manager", "Allow changing the active Builds repository", repositoryManager, onRepositoryManager)
                BuildSettingSwitch("File tools", "Enable repository file-related build tools", fileTools, onFileTools)
                BuildSettingSwitch("Compact build cards", "Reduce spacing in build cards", compactCards, onCompactCards)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun BuildSettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileHeader(profile: GitHubUser?, onOpen: () -> Unit) {
    GlassCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile?.name ?: "GitHub account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(profile?.let { "@${it.login}" } ?: "Connect a GitHub account", color = MaterialTheme.colorScheme.primary)
                profile?.bio?.takeIf(String::isNotBlank)?.let {
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, "Open profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    StandardSettingsRow(icon = icon, title = title, subtitle = subtitle, onClick = onClick, trailing = {
        Icon(Icons.Default.ChevronRight, "Open $title", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    })
}

private fun profileSummary(profile: GitHubUser?): String = profile?.let {
    listOfNotNull(
        it.name?.takeIf(String::isNotBlank)?.let { value -> "Name: $value" },
        it.bio?.takeIf(String::isNotBlank)?.let { value -> "Bio: $value" },
        it.location?.takeIf(String::isNotBlank)?.let { value -> "Location: $value" },
        it.blog?.takeIf(String::isNotBlank)?.let { value -> "Website: $value" },
        it.twitterUsername?.takeIf(String::isNotBlank)?.let { value -> "X: @$value" }
    ).joinToString(" · ").ifBlank { "No optional profile fields returned" }
} ?: "Sign in to load real GitHub profile data"
