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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubOrganizationAccount
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.repository.NativeProfileRepository
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AccountSwitcherUiState(
    val organizations: List<GitHubOrganizationAccount> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountSwitcherViewModel @Inject constructor(
    private val repository: NativeProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountSwitcherUiState())
    val state: StateFlow<AccountSwitcherUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load(connected: Boolean) {
        loadJob?.cancel()
        if (!connected) {
            _state.value = AccountSwitcherUiState()
            return
        }
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatchingPreservingCancellation { repository.organizations() }
                .onSuccess { organizations ->
                    _state.value = AccountSwitcherUiState(organizations = organizations)
                }
                .onFailure { error ->
                    _state.value = AccountSwitcherUiState(error = error.accountMessage())
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherScreen(
    mode: AppMode,
    connectedProfile: GitHubUser?,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onReplaceConnectedAccount: () -> Unit,
    viewModel: AccountSwitcherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var otherLogin by rememberSaveable { mutableStateOf("") }
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
                            "Switch native profile context",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (connected) {
                        IconButton(onClick = { viewModel.load(true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh organizations")
                        }
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
                        badge = "CONNECTED",
                        onClick = { onOpenProfile(profile.login) }
                    )
                }
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Open another public account", fontWeight = FontWeight.Bold)
                                Text(
                                    "View any person or organization without leaving the app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = otherLogin,
                            onValueChange = { otherLogin = it.removePrefix("@").trimStart() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("GitHub username or organization") }
                        )
                        Button(
                            onClick = {
                                normalizedGitHubLogin(otherLogin)?.let(onOpenProfile)
                            },
                            enabled = normalizedGitHubLogin(otherLogin) != null,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open native profile")
                        }
                    }
                }
            }

            item {
                Text(
                    "Organizations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }

            when {
                state.loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(requireNotNull(state.error), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { viewModel.load(connected) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                !connected -> item {
                    GlassCard {
                        Text(
                            "Connect GitHub to load your organizations. Public account browsing still works above.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.organizations.isEmpty() -> item {
                    GlassCard {
                        Text(
                            "No organization memberships were returned for this account.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> items(state.organizations, key = { it.id }) { organization ->
                    AccountContextCard(
                        avatarUrl = organization.avatarUrl,
                        title = organization.login,
                        subtitle = organization.description?.takeIf(String::isNotBlank)
                            ?: "GitHub organization",
                        badge = "ORGANIZATION",
                        onClick = { onOpenProfile(organization.login) }
                    )
                }
            }

            if (connected) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Use another signed-in account", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "GitHub Rock currently stores one encrypted OAuth session. Replacing it signs out this account before the next login.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onReplaceConnectedAccount,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Replace connected account")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountContextCard(
    avatarUrl: String,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "$title avatar",
                    modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.extraLarge)
                )
            } else {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Business, contentDescription = null)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun Throwable.accountMessage(): String = when (this) {
    is retrofit2.HttpException -> when (code()) {
        401 -> "Your GitHub session expired. Sign in again."
        403 -> "GitHub denied organization access or the API rate limit was reached."
        else -> "Unable to load organizations (HTTP ${code()})."
    }
    is java.io.IOException -> "Network unavailable. Check your connection and retry."
    else -> message ?: "Unable to load account contexts."
}
