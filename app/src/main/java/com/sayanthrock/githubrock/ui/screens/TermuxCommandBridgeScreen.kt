package com.sayanthrock.githubrock.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.navigation.TermuxCommandBridge
import com.sayanthrock.githubrock.core.navigation.TermuxBridgeSettings
import com.sayanthrock.githubrock.core.util.TermuxCommand
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel

private data class SuggestedCommand(val label: String, val command: String)

private val suggestions = listOf(
    SuggestedCommand("Git status", "git status"),
    SuggestedCommand("Git pull", "git pull"),
    SuggestedCommand("Git log", "git log --oneline -10"),
    SuggestedCommand("Build", "./gradlew assembleDebug"),
    SuggestedCommand("Test", "./gradlew test"),
    SuggestedCommand("Lint", "./gradlew lint")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxCommandBridgeScreen(
    repository: GitHubRepositoryModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember(context) { TermuxBridgeSettings(context) }
    var bridgeEnabled by remember { mutableStateOf(settings.enabled) }
    var directory by rememberSaveable(repository?.fullName) {
        mutableStateOf(settings.workingDirectory.ifBlank { repository?.name?.let { "$HOME/$it" }.orEmpty() })
    }
    var command by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf(TermuxCommandBridge.State.Ready) }
    var output by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<TermuxCommandBridge.Execution?>(null) }
    var confirmation by remember { mutableStateOf<TermuxCommandBridge.Execution?>(null) }
    var destructiveConfirmation by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(TermuxCommandBridge.status(context)) }
    var history by remember { mutableStateOf(settings.history()) }
    var testMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = TermuxCommandBridge.status(context)
        testMessage = if (granted) "Run commands permission granted." else "Run commands permission was not granted."
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != TermuxCommandBridge.RESULT_ACTION) return
                val id = intent.getStringExtra("execution_id") ?: return
                if (pending?.id != id) return
                val result = TermuxCommandBridge.CommandResult(
                    id = id,
                    exitCode = intent.getIntExtra("exit_code", -1),
                    stdout = intent.getStringExtra("stdout").orEmpty(),
                    stderr = intent.getStringExtra("stderr").orEmpty()
                )
                output = (result.stdout + if (result.stderr.isNotBlank()) "\n" + result.stderr else "").trim()
                state = if (result.success) TermuxCommandBridge.State.Success else TermuxCommandBridge.State.Failed
                pending?.let { settings.updateLatest(it.command, it.workingDirectory, result.success) }
                history = settings.history()
                pending = null
            }
        }
        context.registerReceiver(receiver, IntentFilter(TermuxCommandBridge.RESULT_ACTION), Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun refreshStatus() {
        status = TermuxCommandBridge.status(context)
        state = status.state
    }

    fun prepareRun() {
        val validated = TermuxCommandBridge.validate(command, directory).getOrElse {
            output = it.message.orEmpty()
            state = TermuxCommandBridge.State.Failed
            return
        }
        if (isDestructive(validated.command)) {
            confirmation = validated
            destructiveConfirmation = true
        } else {
            confirmation = validated
            destructiveConfirmation = false
        }
    }

    fun execute(execution: TermuxCommandBridge.Execution) {
        confirmation = null
        state = TermuxCommandBridge.State.Executing
        output = "$ ${execution.command}\n\nStarting in ${execution.workingDirectory}…"
        pending = execution
        settings.workingDirectory = execution.workingDirectory
        settings.addHistory(execution.command, execution.workingDirectory, null)
        history = settings.history()
        TermuxCommandBridge.runCommand(context, execution).onFailure {
            pending = null
            state = TermuxCommandBridge.State.Failed
            output = TermuxCommandBridge.userFacingError(it)
            settings.updateLatest(execution.command, execution.workingDirectory, false)
            history = settings.history()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Command Bridge") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { refreshStatus(); testMessage = statusLabel(status) }) {
                        Icon(Icons.Default.Refresh, "Test connection")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.padding(top = 2.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Termux", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(statusLabel(status), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = bridgeEnabled, onCheckedChange = {
                                bridgeEnabled = it
                                settings.enabled = it
                            })
                        }
                        Text(
                            "Optional command execution through Termux. GitHub Rock never runs commands silently.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!status.installed) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/")))
                            }) {
                                Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Install Termux")
                            }
                        } else if (!status.permissionGranted) {
                            OutlinedButton(onClick = { permissionLauncher.launch(TermuxCommandBridge.RUN_COMMAND_PERMISSION) }) {
                                Text("Grant Run commands permission")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { refreshStatus(); testMessage = statusLabel(status) }) {
                                Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text("Test Connection")
                            }
                            OutlinedButton(onClick = { TermuxCommandBridge.open(context) }) { Text("Open Termux") }
                        }
                        testMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Repository / project context", fontWeight = FontWeight.Bold)
                        Text(
                            repository?.fullName ?: "No repository selected",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = directory,
                            onValueChange = { directory = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Termux working directory") },
                            placeholder = { Text("$HOME/project") }
                        )
                        Text(
                            "This is a Termux filesystem path. GitHub Rock does not assume access to its private app storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Text("Suggested commands", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = command == suggestion.command,
                            onClick = { command = suggestion.command },
                            label = { Text(suggestion.label) }
                        )
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            label = { Text("Command") },
                            placeholder = { Text("git status") }
                        )
                        Button(
                            onClick = { prepareRun() },
                            enabled = bridgeEnabled && status.state == TermuxCommandBridge.State.Ready && pending == null && command.isNotBlank()
                        ) { Icon(Icons.Default.Terminal, null); Spacer(Modifier.width(8.dp)); Text("Run Command") }
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Terminal, null)
                            Text("Terminal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(state.name, color = MaterialTheme.colorScheme.primary)
                        }
                        if (state == TermuxCommandBridge.State.Executing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            output.ifBlank { "Ready. Command output will appear here when Termux returns it." },
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state == TermuxCommandBridge.State.Executing) {
                            TextButton(onClick = { state = TermuxCommandBridge.State.Cancelled; pending = null; output += "\n\nCancellation requested. Termux does not expose a safe kill primitive through RUN_COMMAND; the current session may continue." }) {
                                Icon(Icons.Default.Clear, null); Spacer(Modifier.width(6.dp)); Text("Cancel")
                            }
                        }
                    }
                }
            }
            item { Text("Command history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(history, key = { "${it.command}|${it.directory}|${it.success}" }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.History, null, modifier = Modifier.height(18.dp))
                            Text(item.command, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }
                        Text(item.directory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            when (item.success) { true -> "Completed"; false -> "Failed"; null -> "Running" },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    confirmation?.let { execution ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            icon = { Icon(if (destructiveConfirmation) Icons.Default.Warning else Icons.Default.Terminal, null) },
            title = { Text(if (destructiveConfirmation) "Confirm destructive command" else "Run command?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(execution.command, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("This command will execute through Termux in:")
                    Text(execution.workingDirectory, fontFamily = FontFamily.Monospace)
                    if (destructiveConfirmation) Text("This command may delete, reset, overwrite, or otherwise destroy data. Review it carefully before continuing.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { Button(onClick = { execute(execution) }) { Text("Run") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
        )
    }
}

private fun statusLabel(status: TermuxCommandBridge.Status): String = when {
    !status.installed -> "Not installed"
    !status.permissionGranted -> "Connection unavailable · permission required"
    else -> "Available · ready"
}

private fun isDestructive(command: String): Boolean = Regex("(^|[;&|]\\s*)(rm(\\s|$)|git\\s+reset\\s+--hard|git\\s+clean\\s+-[a-zA-Z]*f|truncate\\s|mkfs\\s|dd\\s+if=)", RegexOption.IGNORE_CASE).containsMatchIn(command) ||
    Regex("(^|[;&|]\\s*)rm\\s+-[a-zA-Z]*r", RegexOption.IGNORE_CASE).containsMatchIn(command)
