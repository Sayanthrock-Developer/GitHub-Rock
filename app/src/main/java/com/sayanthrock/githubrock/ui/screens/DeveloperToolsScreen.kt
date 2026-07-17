package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.navigation.TermuxBridge
import com.sayanthrock.githubrock.core.util.DeveloperCommandBuilder
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import kotlinx.coroutines.launch

private data class PendingTermuxCommand(val label: String, val command: String)
private data class ApiProviderOption(val label: String, val variable: String)

private val apiProviders = listOf(
    ApiProviderOption("OpenAI", "OPENAI_API_KEY"),
    ApiProviderOption("Anthropic", "ANTHROPIC_API_KEY"),
    ApiProviderOption("Gemini", "GEMINI_API_KEY"),
    ApiProviderOption("GitHub token", "GH_TOKEN")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolsScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    onBack: () -> Unit
) {
    val firstRepository = repositories.firstOrNull()
    var owner by remember(firstRepository?.fullName) { mutableStateOf(firstRepository?.owner?.login.orEmpty()) }
    var repository by remember(firstRepository?.fullName) { mutableStateOf(firstRepository?.name.orEmpty()) }
    var pullRequest by remember { mutableStateOf("91") }
    var apiVariable by remember { mutableStateOf("OPENAI_API_KEY") }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var termuxInstalled by remember { mutableStateOf(TermuxBridge.isInstalled(context)) }
    var pendingCommand by remember { mutableStateOf<PendingTermuxCommand?>(null) }

    val checkoutCommand = DeveloperCommandBuilder.checkout(owner, repository, pullRequest)
    val viewCommand = DeveloperCommandBuilder.viewPullRequest(owner, repository, pullRequest)
    val apiCommand = DeveloperCommandBuilder.pullRequestApi(owner, repository, pullRequest)
    val cloneCommand = DeveloperCommandBuilder.clone(owner, repository)
    val safeApiVariable = DeveloperCommandBuilder.environmentVariable(apiVariable)
    val sessionKeyCommand = DeveloperCommandBuilder.sessionApiKey(apiVariable)
    val persistentKeyCommand = DeveloperCommandBuilder.persistentApiKey(apiVariable)
    val loadKeyCommand = DeveloperCommandBuilder.loadPersistentApiKey(apiVariable)

    fun copy(label: String, value: String) {
        if (value.isBlank()) {
            scope.launch { snackbar.showSnackbar("Complete the required fields first") }
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        scope.launch { snackbar.showSnackbar("$label copied") }
    }

    fun requestTermux(label: String, command: String) {
        termuxInstalled = TermuxBridge.isInstalled(context)
        if (!termuxInstalled) {
            scope.launch { snackbar.showSnackbar("Termux is not installed") }
        } else if (command.isBlank()) {
            scope.launch { snackbar.showSnackbar("Complete the required fields first") }
        } else {
            pendingCommand = PendingTermuxCommand(label, command)
        }
    }

    pendingCommand?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
            title = { Text("Send to Termux?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GitHub Rock will ask Termux to open a visible terminal session and run this command:")
                    Text(
                        pending.command,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Nothing runs silently. Termux must allow external apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCommand = null
                        TermuxBridge.runCommand(context, pending.command)
                            .onSuccess {
                                scope.launch { snackbar.showSnackbar("${pending.label} sent to Termux") }
                            }
                            .onFailure { error ->
                                scope.launch { snackbar.showSnackbar(TermuxBridge.userFacingError(error)) }
                            }
                    }
                ) { Text("Open and run") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCommand = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Developer Tools") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("GitHub ↔ Termux bridge", fontWeight = FontWeight.Bold)
                            Text(
                                if (termuxInstalled) "Termux detected on this device" else "Termux is not installed or cannot be detected",
                                color = if (termuxInstalled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { termuxInstalled = TermuxBridge.isInstalled(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Check Termux again")
                        }
                    }
                    Text(
                        "GitHub Rock never transfers its OAuth token. Termux connects separately to the same GitHub account through GitHub CLI browser authorization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                TermuxBridge.open(context)
                                    .onFailure { error ->
                                        scope.launch { snackbar.showSnackbar(TermuxBridge.userFacingError(error)) }
                                    }
                            },
                            enabled = termuxInstalled,
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Termux")
                        }
                        FilledTonalButton(
                            onClick = {
                                copy("Termux bridge setup", DeveloperCommandBuilder.ENABLE_TERMUX_BRIDGE)
                            },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy bridge setup")
                        }
                    }
                }
            }

            CommandCard(
                title = "1. Enable GitHub Rock bridge",
                description = "Run this once inside Termux, then restart Termux. It enables Termux's official external-command service.",
                command = DeveloperCommandBuilder.ENABLE_TERMUX_BRIDGE,
                onCopy = { copy("Termux bridge setup", DeveloperCommandBuilder.ENABLE_TERMUX_BRIDGE) },
                onSend = null
            )
            CommandCard(
                title = "2. Install coding tools",
                description = "Installs Git, GitHub CLI and OpenSSH from Termux packages.",
                command = DeveloperCommandBuilder.INSTALL_TOOLCHAIN,
                onCopy = { copy("Toolchain setup", DeveloperCommandBuilder.INSTALL_TOOLCHAIN) },
                onSend = { requestTermux("Toolchain setup", DeveloperCommandBuilder.INSTALL_TOOLCHAIN) }
            )
            CommandCard(
                title = "3. Connect GitHub account",
                description = "GitHub CLI opens secure browser authorization. Your password and app OAuth token are not shared.",
                command = DeveloperCommandBuilder.GITHUB_LOGIN,
                onCopy = { copy("GitHub login", DeveloperCommandBuilder.GITHUB_LOGIN) },
                onSend = { requestTermux("GitHub login", DeveloperCommandBuilder.GITHUB_LOGIN) }
            )
            CommandCard(
                title = "4. Configure Git authentication",
                description = "Connects Git operations to GitHub CLI and verifies the active account.",
                command = DeveloperCommandBuilder.GITHUB_SETUP_GIT,
                onCopy = { copy("Git setup", DeveloperCommandBuilder.GITHUB_SETUP_GIT) },
                onSend = { requestTermux("Git setup", DeveloperCommandBuilder.GITHUB_SETUP_GIT) }
            )

            Button(
                onClick = { requestTermux("Complete GitHub setup", DeveloperCommandBuilder.fullGitHubSetup()) },
                enabled = termuxInstalled,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Connect GitHub to Termux")
            }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Connection status", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (mode == AppMode.Connected) {
                            "GitHub Rock is connected. Termux still uses its own gh auth login so credentials remain isolated between apps."
                        } else {
                            "GitHub Rock is in ${mode.name.lowercase()} mode. Termux can still connect independently with gh auth login."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it.take(39) },
                label = { Text("Repository owner") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it.take(100) },
                label = { Text("Repository name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pullRequest,
                onValueChange = { pullRequest = it.filter(Char::isDigit).take(10) },
                label = { Text("Pull request number") },
                supportingText = { Text("Example: 91") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CommandCard(
                "Checkout pull request",
                "Checks out the selected PR with GitHub CLI.",
                checkoutCommand,
                { copy("Checkout command", checkoutCommand) },
                { requestTermux("Checkout command", checkoutCommand) }
            )
            CommandCard(
                "View pull request",
                "Opens the pull request in the browser from Termux.",
                viewCommand,
                { copy("View command", viewCommand) },
                { requestTermux("View command", viewCommand) }
            )
            CommandCard(
                "GitHub API request",
                "Uses the authenticated gh session without embedding an API token.",
                apiCommand,
                { copy("API command", apiCommand) },
                { requestTermux("API command", apiCommand) }
            )
            CommandCard(
                "Clone repository",
                "Clones the selected repository into Termux home storage.",
                cloneCommand,
                { copy("Clone command", cloneCommand) },
                { requestTermux("Clone command", cloneCommand) }
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Mobile coding API keys", fontWeight = FontWeight.Bold)
                            Text(
                                "Choose a provider or enter a valid environment-variable name. The key is typed only in Termux's hidden prompt.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        apiProviders.take(2).forEach { provider ->
                            FilterChip(
                                selected = apiVariable == provider.variable,
                                onClick = { apiVariable = provider.variable },
                                label = { Text(provider.label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        apiProviders.drop(2).forEach { provider ->
                            FilterChip(
                                selected = apiVariable == provider.variable,
                                onClick = { apiVariable = provider.variable },
                                label = { Text(provider.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = apiVariable,
                        onValueChange = { apiVariable = it.uppercase().filter { character -> character.isLetterOrDigit() || character == '_' }.take(64) },
                        label = { Text("API-key variable") },
                        supportingText = {
                            Text(if (safeApiVariable == null) "Use letters, numbers and underscores" else "Key value is never entered in this app")
                        },
                        isError = safeApiVariable == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Persistent mode stores the key as plaintext protected by file mode 600 inside Termux. Device compromise or root access can still expose it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            CommandCard(
                "Use API key for this session",
                "Prompts invisibly, exports the variable, and keeps it only in the current shell.",
                sessionKeyCommand,
                { copy("Session API-key command", sessionKeyCommand) },
                { requestTermux("Session API-key setup", sessionKeyCommand) }
            )
            CommandCard(
                "Save API key with mode 600",
                "Prompts inside Termux and saves a protected environment file without putting the key in command history.",
                persistentKeyCommand,
                { copy("Persistent API-key command", persistentKeyCommand) },
                { requestTermux("Persistent API-key setup", persistentKeyCommand) }
            )
            CommandCard(
                "Load saved API key",
                "Loads the saved provider key into a future Termux shell.",
                loadKeyCommand,
                { copy("Load API-key command", loadKeyCommand) },
                { requestTermux("Load API-key command", loadKeyCommand) }
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Mobile coding environment", fontWeight = FontWeight.Bold)
                    }
                    Text("Git · GitHub CLI · SSH · repository cloning · pull-request checkout · gh api · provider API-key environment variables")
                    Text(
                        "Compilers, language runtimes and editors can then be installed through Termux packages. GitHub Rock keeps native repository, issue, PR, Actions, release, download and APK tools separate from the terminal sandbox.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandCard(
    title: String,
    description: String,
    command: String,
    onCopy: () -> Unit,
    onSend: (() -> Unit)?
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                command.ifBlank { "Complete the fields above" },
                fontFamily = FontFamily.Monospace,
                color = if (command.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCopy, enabled = command.isNotBlank()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
                if (onSend != null) {
                    FilledTonalButton(onClick = onSend, enabled = command.isNotBlank()) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Termux")
                    }
                }
            }
        }
    }
}