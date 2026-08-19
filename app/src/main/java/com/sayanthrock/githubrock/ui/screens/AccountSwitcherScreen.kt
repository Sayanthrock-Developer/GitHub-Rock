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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.GitHubOrganizationAccount
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.security.StoredAccount
import com.sayanthrock.githubrock.core.security.StoredTokens
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.auth.DeviceFlowAuthRepository
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import com.sayanthrock.githubrock.data.repository.NativeProfileRepository
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountSwitcherUiState(
    val accounts: List<StoredAccount> = emptyList(),
    val organizations: List<GitHubOrganizationAccount> = emptyList(),
    val activeAccountId: String? = null,
    val activeOrganization: String? = null,
    val loading: Boolean = false,
    val auth: AccountAuthUiState = AccountAuthUiState(),
    val error: String? = null
)

data class AccountAuthUiState(
    val code: DeviceCodeResponse? = null,
    val status: String? = null,
    val error: String? = null
)

@HiltViewModel
class AccountSwitcherViewModel @Inject constructor(
    private val authRepository: DeviceFlowAuthRepository,
    private val githubRepository: GitHubRepository,
    private val nativeProfileRepository: NativeProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountSwitcherUiState())
    val state: StateFlow<AccountSwitcherUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun load(connected: Boolean) {
        if (!connected) {
            _state.value = AccountSwitcherUiState()
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatchingPreservingCancellation {
                nativeProfileRepository.organizations()
            }.onSuccess { organizations ->
                _state.value = AccountSwitcherUiState(
                    accounts = authRepository.accounts,
                    organizations = organizations,
                    activeAccountId = authRepository.activeAccountId,
                    activeOrganization = authRepository.activeOrganization
                )
            }.onFailure { error ->
                _state.value = AccountSwitcherUiState(
                    accounts = authRepository.accounts,
                    activeAccountId = authRepository.activeAccountId,
                    activeOrganization = authRepository.activeOrganization,
                    error = error.accountMessage()
                )
            }
        }
    }

    fun switchAccount(accountId: String, onChanged: () -> Unit) {
        if (!authRepository.switchAccount(accountId)) {
            _state.update { it.copy(error = "Unable to switch to that account.") }
            return
        }
        _state.update {
            it.copy(
                activeAccountId = accountId,
                activeOrganization = null,
                accounts = authRepository.accounts
            )
        }
        onChanged()
    }

    fun removeAccount(accountId: String, onChanged: () -> Unit) {
        if (authRepository.accounts.size <= 1) {
            _state.update { it.copy(error = "Keep at least one signed-in account. Use Log out to remove the final account.") }
            return
        }
        if (!authRepository.removeAccount(accountId)) {
            _state.update { it.copy(error = "Unable to remove that account.") }
            return
        }
        _state.update {
            it.copy(
                accounts = authRepository.accounts,
                activeAccountId = authRepository.activeAccountId,
                activeOrganization = authRepository.activeOrganization
            )
        }
        onChanged()
    }

    fun selectOrganization(login: String?, onChanged: () -> Unit) {
        authRepository.setOrganization(login)
        _state.update { it.copy(activeOrganization = authRepository.activeOrganization) }
        onChanged()
    }

    fun startAddAccount() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(auth = AccountAuthUiState(status = "Requesting a device code…"), error = null) }
            try {
                val code = authRepository.begin()
                _state.update { it.copy(auth = AccountAuthUiState(code = code, status = "Approve this account on GitHub.")) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(auth = AccountAuthUiState(error = error.accountMessage())) }
            }
        }
    }

    fun checkAddAccount(onChanged: () -> Unit) {
        val code = _state.value.auth.code ?: return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(auth = it.auth.copy(status = "Checking GitHub authorization…", error = null)) }
            try {
                val tokens = authRepository.poll(code, onStatus = { status ->
                    _state.update { it.copy(auth = it.auth.copy(status = status, error = null)) }
                }, save = false)
                authRepository.addAccount(tokens)
                val dashboard = githubRepository.dashboard()
                authRepository.updateActiveAccount(
                    login = dashboard.profile.login,
                    name = dashboard.profile.name,
                    avatarUrl = dashboard.profile.avatarUrl
                )
                _state.update {
                    it.copy(
                        accounts = authRepository.accounts,
                        activeAccountId = authRepository.activeAccountId,
                        activeOrganization = null,
                        auth = AccountAuthUiState()
                    )
                }
                onChanged()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(auth = it.auth.copy(error = error.accountMessage())) }
            }
        }
    }

    fun clearAuth() = _state.update { it.copy(auth = AccountAuthUiState()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherScreen(
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
    var otherLogin by rememberSaveable { mutableStateOf("") }
    var accountToRemove by remember { mutableStateOf<StoredAccount?>(null) }
    val connected = mode == AppMode.Connected

    LaunchedEffect(connected) { viewModel.load(connected) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Accounts & organizations", fontWeight = FontWeight.Bold)
                        Text(
                            "Switch users, accounts and organization context",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (connected) IconButton(onClick = { viewModel.load(true) }) {
                        Icon(Icons.Default.Refresh, "Refresh accounts")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            connectedProfile?.let { profile ->
                item {
                    AccountContextCard(
                        avatarUrl = profile.avatarUrl,
                        title = profile.name ?: profile.login,
                        subtitle = "@${profile.login} · Personal account",
                        badge = "ACTIVE",
                        onClick = { onOpenProfile(profile.login) }
                    )
                }
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Login, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("GitHub accounts", fontWeight = FontWeight.Bold)
                                Text(
                                    "Keep multiple accounts signed in and switch instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(state.accounts.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                        Button(onClick = viewModel::startAddAccount, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add another account")
                        }
                    }
                }
            }

            if (state.accounts.isNotEmpty()) {
                item { Text("Saved accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
                items(state.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        active = account.id == state.activeAccountId,
                        onSwitch = { viewModel.switchAccount(account.id, onContextChanged) },
                        onRemove = { accountToRemove = account }
                    )
                }
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Organization context", fontWeight = FontWeight.Bold)
                                Text(
                                    state.activeOrganization?.let { "Showing repositories for @$it" } ?: "Personal account context",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.selectOrganization(null, onContextChanged) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Personal account") }
                    }
                }
            }

            item { Text("Organizations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(requireNotNull(state.error), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { viewModel.load(connected) }) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                state.organizations.isEmpty() -> item {
                    GlassCard { Text("No organization memberships were returned for this account.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                else -> items(state.organizations, key = { it.id }) { organization ->
                    OrganizationRow(
                        organization = organization,
                        active = organization.login.equals(state.activeOrganization, ignoreCase = true),
                        onSelect = { viewModel.selectOrganization(organization.login, onContextChanged) },
                        onOpen = { onOpenProfile(organization.login) }
                    )
                }
            }

            if (connected) {
                item {
                    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Log out of all accounts")
                    }
                }
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonSearch, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Open another public account", fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = otherLogin,
                            onValueChange = { otherLogin = it.removePrefix("@").trimStart() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("GitHub username or organization") }
                        )
                        Button(
                            onClick = { onOpenProfile(otherLogin.removePrefix("@").trim()) },
                            enabled = otherLogin.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.PersonSearch, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open public profile")
                        }
                    }
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
            confirmButton = {
                Button(onClick = { onOpenGitHubUrl(code.verificationUri) }) { Text("Open GitHub") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.checkAddAccount(onContextChanged) }) { Text("Check") }
            }
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
private fun AccountRow(account: StoredAccount, active: Boolean, onSwitch: () -> Unit, onRemove: () -> Unit) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(account.avatarUrl, account.login ?: account.id)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name ?: account.login ?: "GitHub account", fontWeight = FontWeight.Bold)
                Text("@${account.login ?: "unknown"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (active) {
                Icon(Icons.Default.CheckCircle, "Active", tint = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = onSwitch) { Text("Switch") }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, "Remove account") }
        }
    }
}

@Composable
private fun OrganizationRow(
    organization: GitHubOrganizationAccount,
    active: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    GlassCard(onClick = onSelect) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(organization.avatarUrl, organization.login)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(organization.login, fontWeight = FontWeight.Bold)
                Text(
                    organization.description?.takeIf(String::isNotBlank) ?: "GitHub organization",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (active) Icon(Icons.Default.CheckCircle, "Active organization", tint = MaterialTheme.colorScheme.primary)
            else TextButton(onClick = onSelect) { Text("Use") }
            TextButton(onClick = onOpen) { Text("Open") }
        }
    }
}

@Composable
private fun AccountContextCard(avatarUrl: String, title: String, subtitle: String, badge: String, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(avatarUrl, title)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun Avatar(url: String?, fallback: String) {
    if (!url.isNullOrBlank()) {
        coil.compose.AsyncImage(
            model = url,
            contentDescription = "$fallback avatar",
            modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.extraLarge)
        )
    } else {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null) }
        }
    }
}

private fun Throwable.accountMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "This GitHub session expired. Sign in again."
        403 -> "GitHub denied organization access or the API rate limit was reached."
        else -> "GitHub request failed (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Unable to manage GitHub accounts."
}
