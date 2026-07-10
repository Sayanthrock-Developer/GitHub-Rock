package com.sayanthrock.githubrock.feature.builds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.workflow.AndroidArtifactType
import com.sayanthrock.githubrock.core.workflow.AndroidWorkflowGenerator
import com.sayanthrock.githubrock.demo.DemoData
import com.sayanthrock.githubrock.ui.theme.GlassCard

@Composable
fun BuildsScreen(demoMode: Boolean) {
    var module by remember { mutableStateOf("app") }
    var type by remember { mutableStateOf(AndroidArtifactType.DEBUG_APK) }
    var workflow by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Android Builds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Generate a safe GitHub Actions workflow for cloud builds.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Build workflow generator", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = module,
                        onValueChange = { module = it },
                        label = { Text("Application module") },
                        supportingText = { Text("Examples: app or mobile:app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AndroidArtifactType.entries.forEach { candidate ->
                            FilterChip(
                                selected = type == candidate,
                                onClick = { type = candidate },
                                label = { Text(candidate.label) }
                            )
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            runCatching { AndroidWorkflowGenerator.generate(module, type) }
                                .onSuccess { workflow = it; error = null }
                                .onFailure { error = it.message }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Terminal, null)
                        Text(" Generate workflow YAML")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "GitHub Rock shows the complete workflow before any future commit operation. Signing values must come from GitHub repository or environment secrets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (demoMode) {
            item { Text("Recent demo runs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(DemoData.workflows.size) { index ->
                val item = DemoData.workflows[index]
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text(item.repository, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.state.name.lowercase()} · ${item.detail}")
                    }
                }
            }
        }
    }

    workflow?.let { yaml ->
        AlertDialog(
            onDismissRequest = { workflow = null },
            title = { Text("Generated android-build.yml") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(yaml, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { clipboard.setText(AnnotatedString(yaml)) }) {
                    Icon(Icons.Default.ContentCopy, null)
                    Text(" Copy")
                }
            },
            dismissButton = { TextButton(onClick = { workflow = null }) { Text("Close") } }
        )
    }
}
