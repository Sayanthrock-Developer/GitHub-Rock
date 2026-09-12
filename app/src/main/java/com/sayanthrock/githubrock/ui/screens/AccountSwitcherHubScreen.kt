package com.sayanthrock.githubrock.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubOrganizationAccount
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.security.StoredAccount
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.vector

private enum class AccountHubAction {
    Switch,
    Add,
    Organizations,
    Access,
    Remove
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherHubScreen(
    mode: AppMode,
    connectedProfile: GitHubUser?,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onContextChanged: () -> Unit,
    onLogout: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    viewModel: AccountSwitcherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedAction by remember { mutableStateOf<AccountHubAction?>(null) }
    var accountToRemove by remember { mutableStateOf<StoredAccount?>(null) }
    val connected = mode == AppMode.Connected
    val showAll = selectedAction == null || selectedAction == AccountHubAction.Switch && false

    LaunchedEffect(connected) { viewModel.load(connected) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Accounts & organizations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), "Back") }
                },
                actions = {
                    if (connected) {
                        IconButton(onClick = { viewModel.load(true) }) {
                            Icon(RockIcon.Refresh.vector(), "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                connectedProfile?.let { profile ->
                    CurrentAccountHero(
                        profile = profile,
                        organization = state.activeOrganization,
                        onOpenProfile = { onOpenProfile(profile.login) }
                    )
                } ?: EmptyAccountHero()
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("What do you want to do?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "Choose one task, or manage everything from one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ActionGrid(
                    selected = selectedAction,
                    onSelect = { selectedAction = if (selectedAction == it) null else it },
                    onManageEverything = { selectedAction = null }
                )
            }

            if (selectedAction == null) {
                item {
                    GlassCard(onClick = { selectedAction = null }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(RockIcon.Tune.vector(), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Manage everything", fontWeight = FontWeight.Bold)
                                Text(
                                    "Accounts, organizations, access and sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(RockIcon.ChevronRight.vector(), null)
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = selectedAction != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ActionPanel(
                        action = selectedAction,
                        state = state,
                        connected = connected,
                        onSwitch = { id -> viewModel.switchAccount(id, onContextChanged) },
                        onAdd = viewModel::startAddAccount,
                        onSelectOrganization = { login -> viewModel.selectOrganization(login, onContextChanged) },
                        onOpenProfile = onOpenProfile,
                        onRemove = { accountToRemove = it },
                        onLogout = onLogout,
                        onOpenGitHubUrl = onOpenGitHubUrl,
                        onRetry = { viewModel.load(connected) }
                    )
                }
            }
        }
    }

    state.auth.code?.let { code ->
        AlertDialog(
            onDismissRequest = viewModel::clearAuth,
            title = { Text("Add GitHub account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Open GitHub, enter the device code, approve the account, then return here.")
                    Text(code.userCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(code.verificationUri, color = MaterialTheme.colorScheme.primary)
                    state.auth.status?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    state.auth.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { Button(onClick = { onOpenGitHubUrl(code.verificationUri) }) { Text("Open GitHub") } },
            dismissButton = { TextButton(onClick = { viewModel.checkAddAccount(onContextChanged) }) { Text("Check") } }
        )
    }

    state.auth.error?.takeIf { state.auth.code == null }?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearAuth,
            title = { Text("Account login failed") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::clearAuth) { Text("Close") } }
        )
    }

    accountToRemove?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text("Remove @${account.login ?: account.id}?") },
            text = { Text("This removes the encrypted session from GitHub Rock. It does not revoke the GitHub authorization itself.") },
            confirmButton = {
                Button(onClick = {
                    accountToRemove = null
                    viewModel.removeAccount(account.id, onContextChanged)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { accountToRemove = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ActionGrid(
    selected: AccountHubAction?,
    onSelect: (AccountHubAction) -> Unit,
    onManageEverything: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccountActionCard(
                modifier = Modifier.weight(1f),
                icon = RockIcon.Person,
                title = "Switch account",
                subtitle = "Change workspace",
                selected = selected == AccountHubAction.Switch,
                onClick = { onSelect(AccountHubAction.Switch) }
            )
            AccountActionCard(
                modifier = Modifier.weight(1f),
                icon = RockIcon.PersonAdd,
                title = "Add account",
                subtitle = "Sign in another account",
                selected = selected == AccountHubAction.Add,
                onClick = { onSelect(AccountHubAction.Add) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccountActionCard(
                modifier = Modifier.weight(1f),
                icon = RockIcon.Repositories,
                title = "Organizations",
                subtitle = "Choose context",
                selected = selected == AccountHubAction.Organizations,
                onClick = { onSelect(AccountHubAction.Organizations) }
            )
            AccountActionCard(
                modifier = Modifier.weight(1f),
                icon = RockIcon.Security,
                title = "Access",
                subtitle = "Permissions & security",
                selected = selected == AccountHubAction.Access,
                onClick = { onSelect(AccountHubAction.Access) }
            )
        }
        AccountActionCard(
            modifier = Modifier.fillMaxWidth(),
            icon = RockIcon.PersonRemove,
            title = "Remove account",
            subtitle = "Remove a saved session",
            selected = selected == AccountHubAction.Remove,
            onClick = { onSelect(AccountHubAction.Remove) }
        )
    }
}

@Composable
private fun AccountActionCard(
    modifier: Modifier,
    icon: RockIcon,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.large,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon.vector(selected),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionPanel(
    action: AccountHubAction?,
    state: AccountSwitcherUiState,
    connected: Boolean,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onSelectOrganization: (String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onRemove: (StoredAccount) -> Unit,
    onLogout: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (action) {
        AccountHubAction.Switch -> AccountSwitchPanel(state, onSwitch)
        AccountHubAction.Add -> AddAccountPanel(onAdd, state)
        AccountHubAction.Organizations -> OrganizationPanel(state, onSelectOrganization, onOpenProfile, onRetry)
        AccountHubAction.Access -> AccessPanel(onOpenGitHubUrl)
        AccountHubAction.Remove -> RemoveAccountPanel(state, connected, onRemove, onLogout)
        null -> Unit
    }
}

@Composable
private fun AccountSwitchPanel(state: AccountSwitcherUiState, onSwitch: (String) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelTitle(RockIcon.Person, "Switch account", "Tap an account to make it the active workspace.")
            if (state.accounts.isEmpty()) {
                Text("No saved accounts are available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.accounts.forEach { account ->
                    AccountManagementRow(
                        avatarUrl = account.avatarUrl,
                        title = account.name ?: account.login ?: "GitHub account",
                        subtitle = "@${account.login ?: "unknown"}",
                        active = account.id == state.activeAccountId,
                        actionLabel = if (account.id == state.activeAccountId) "Active" else "Switch",
                        onClick = { onSwitch(account.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAccountPanel(onAdd: () -> Unit, state: AccountSwitcherUiState) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelTitle(RockIcon.PersonAdd, "Add another account", "Sign in without replacing your current GitHub session.")
            Text(
                "GitHub Rock uses the existing Device Flow. Your current account remains available while the new session is added.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(RockIcon.PersonAdd.vector(), null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.auth.status == null) "Continue with GitHub" else "Authorization in progress")
            }
        }
    }
}

@Composable
private fun OrganizationPanel(
    state: AccountSwitcherUiState,
    onSelect: (String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onRetry: () -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelTitle(RockIcon.Repositories, "Manage organizations", "Choose which GitHub workspace Repositories and Builds should use.")
            AccountManagementRow(
                avatarUrl = null,
                title = "Personal account",
                subtitle = "Use your own repositories",
                active = state.activeOrganization == null,
                actionLabel = if (state.activeOrganization == null) "Active" else "Use",
                onClick = { onSelect(null) }
            )
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            } else if (state.organizations.isEmpty()) {
                Text("No organization memberships were returned for this account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.organizations.forEach { organization ->
                    AccountManagementRow(
                        avatarUrl = organization.avatarUrl,
                        title = organization.login,
                        subtitle = organization.description?.takeIf(String::isNotBlank) ?: "GitHub organization",
                        active = organization.login.equals(state.activeOrganization, ignoreCase = true),
                        actionLabel = if (organization.login.equals(state.activeOrganization, true)) "Active" else "Use",
                        onClick = { onSelect(organization.login) },
                        onOpen = { onOpenProfile(organization.login) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessPanel(onOpenGitHubUrl: (String) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelTitle(RockIcon.Security, "Access & permissions", "Review GitHub-side authorization and security controls.")
            Text(
                "GitHub Rock does not invent or mirror GitHub permission controls. Use GitHub's own security settings for authorization and access management.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { onOpenGitHubUrl("https://github.com/settings/security") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(RockIcon.OpenInNew.vector(), null)
                Spacer(Modifier.width(8.dp))
                Text("Open GitHub security settings")
            }
        }
    }
}

@Composable
private fun RemoveAccountPanel(
    state: AccountSwitcherUiState,
    connected: Boolean,
    onRemove: (StoredAccount) -> Unit,
    onLogout: () -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelTitle(RockIcon.PersonRemove, "Remove a saved account", "Remove only the local encrypted session from GitHub Rock.")
            if (state.accounts.isEmpty()) {
                Text("No saved accounts are available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.accounts.forEach { account ->
                    AccountManagementRow(
                        avatarUrl = account.avatarUrl,
                        title = account.name ?: account.login ?: "GitHub account",
                        subtitle = "@${account.login ?: "unknown"}",
                        active = account.id == state.activeAccountId,
                        actionLabel = "Remove",
                        onClick = { onRemove(account) }
                    )
                }
            }
            if (connected) {
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Icon(RockIcon.Logout.vector(), null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log out of all accounts")
                }
            }
        }
    }
}

@Composable
private fun PanelTitle(icon: RockIcon, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon.vector(), null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountManagementRow(
    avatarUrl: String?,
    title: String,
    subtitle: String,
    active: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    onOpen: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountHubAvatar(avatarUrl, title)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (onOpen != null) TextButton(onClick = onOpen) { Text("Open") }
        TextButton(onClick = onClick) { Text(actionLabel) }
    }
}

@Composable
private fun CurrentAccountHero(profile: GitHubUser, organization: String?, onOpenProfile: () -> Unit) {
    GlassCard(onClick = onOpenProfile) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AccountHubAvatar(profile.avatarUrl, profile.login, size = 68.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.name ?: profile.login, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("@${profile.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    organization?.let { "Organization: @$it" } ?: "Personal account · Current workspace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(RockIcon.ChevronRight.vector(), null)
        }
    }
}

@Composable
private fun EmptyAccountHero() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No GitHub account connected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Choose Add account below to connect GitHub Rock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountHubAvatar(url: String?, fallback: String, size: androidx.compose.ui.unit.Dp = 48.dp) {
    if (!url.isNullOrBlank()) {
        coil.compose.AsyncImage(
            model = url,
            contentDescription = "$fallback avatar",
            modifier = Modifier.size(size).clip(MaterialTheme.shapes.extraLarge)
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(RockIcon.AccountCircle.vector(), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
