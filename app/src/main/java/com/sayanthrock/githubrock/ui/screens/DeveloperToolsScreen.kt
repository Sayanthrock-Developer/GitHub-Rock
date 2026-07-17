package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val safeOwner = owner.trim().takeIf { it.matches(Regex("^[A-Za-z0-9-]+$")) }
    val safeRepository = repository.trim().takeIf { it.matches(Regex("^[A-Za-z0-9._-]+$")) }
    val safePullRequest = pullRequest.trim().toIntOrNull()?.takeIf { it > 0 }
    val repositoryFlag = if (safeOwner != null && safeRepository != null) " --repo $safeOwner/$safeRepository" else ""
    val checkoutCommand = safePullRequest?.let { "gh pr checkout $it$repositoryFlag" }.orEmpty()
    val viewCommand = safePullRequest?.let { "gh pr view $it$repositoryFlag --web" }.orEmpty()
    val apiCommand = if (safeOwner != null && safeRepository != null && safePullRequest != null) {
        "gh api repos/$safeOwner/$safeRepository/pulls/$safePullRequest"
    } else {
        ""
    }
    val cloneCommand = if (safeOwner != null && safeRepository != null) "gh repo clone $safeOwner/$safeRepository" else ""

    fun copy(label: String, value: String) {
        if (value.isBlank()) {
            scope.launch { snackbar.showSnackbar("Enter a valid owner, repository and pull request number") }
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        scope.launch { snackbar.showSnackbar("$label copied") }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Mobile command workspace", fontWeight = FontWeight.Bold)
                            Text(
                                "Build GitHub CLI commands safely, then copy them to Termux or another Android terminal.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "GitHub Rock does not expose your OAuth token to the clipboard or execute unrestricted shell commands inside the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it },
                label = { Text("Repository owner") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it },
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

            CommandCard("Checkout pull request", checkoutCommand) { copy("Checkout command", checkoutCommand) }
            CommandCard("View pull request", viewCommand) { copy("View command", viewCommand) }
            CommandCard("GitHub API request", apiCommand) { copy("API command", apiCommand) }
            CommandCard("Clone repository", cloneCommand) { copy("Clone command", cloneCommand) }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("API access", fontWeight = FontWeight.Bold)
                            Text(
                                if (mode == AppMode.Connected) {
                                    "Connected mode already uses GitHub OAuth. No separate paid API key is required."
                                } else {
                                    "Sign in to use authenticated GitHub API operations and higher account-based limits."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Personal access tokens are secrets. Do not paste them into screenshots, public issues, source code, or command history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
                        if (launchIntent == null) {
                            scope.launch { snackbar.showSnackbar("Termux is not installed") }
                        } else {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Termux")
                }
                FilledTonalButton(
                    onClick = { copy("Checkout command", checkoutCommand) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy checkout")
                }
            }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("What works natively", fontWeight = FontWeight.Bold)
                    }
                    Text("Repositories · code viewing/editing · issues · pull requests · reviews · Actions · releases · downloads · APK inspection")
                    Text(
                        "A full Linux shell, arbitrary compilers and desktop binaries require a dedicated Android terminal environment.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandCard(title: String, command: String, onCopy: () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                command.ifBlank { "Complete the fields above" },
                fontFamily = FontFamily.Monospace,
                color = if (command.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
            OutlinedButton(onClick = onCopy, enabled = command.isNotBlank()) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy")
            }
        }
    }
}
