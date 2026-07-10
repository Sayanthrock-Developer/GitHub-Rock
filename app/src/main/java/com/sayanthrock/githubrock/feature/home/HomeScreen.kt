package com.sayanthrock.githubrock.feature.home

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sayanthrock.githubrock.core.model.WorkflowVisualState
import com.sayanthrock.githubrock.demo.DemoData
import com.sayanthrock.githubrock.ui.theme.GlassCard

@Composable
fun HomeScreen(
    demoMode: Boolean,
    guestMode: Boolean,
    onRepositories: () -> Unit,
    onBuilds: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                when {
                    demoMode -> "Demo dashboard"
                    guestMode -> "Public GitHub dashboard"
                    else -> "Developer dashboard"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    demoMode -> "Sample data is isolated from connected accounts."
                    guestMode -> "Browse public repositories without signing in."
                    else -> "Repositories, builds, issues and releases in one place."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Quick actions", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("Repositories", Icons.Default.Folder, onRepositories)
                        ActionChip("Build APK", Icons.Default.Build, onBuilds)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("New repository", Icons.Default.Add) {
                            openUrl(context, "https://github.com/new")
                        }
                        ActionChip("Issues", Icons.Default.ErrorOutline) {
                            openUrl(context, "https://github.com/issues")
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Repository activity")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Recent", if (demoMode) "3" else "—", Modifier.weight(1f))
                MetricCard("Open issues", if (demoMode) "18" else "—", Modifier.weight(1f))
                MetricCard("Pull requests", if (demoMode) "7" else "—", Modifier.weight(1f))
            }
        }

        item { SectionTitle("Workflow status") }
        if (demoMode) {
            items(DemoData.workflows) { workflow ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(workflow.name, fontWeight = FontWeight.SemiBold)
                            Text(statusLabel(workflow.state), color = statusColor(workflow.state))
                        }
                        Text(workflow.repository, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(workflow.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Live workflow summary", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Workflow run listing and dispatch are scheduled for the next functional milestone. Build workflow generation is available now in Builds.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onBuilds) { Text("Open Builds") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) }
    )
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun statusColor(state: WorkflowVisualState) = when (state) {
    WorkflowVisualState.SUCCESS -> MaterialTheme.colorScheme.secondary
    WorkflowVisualState.FAILED -> MaterialTheme.colorScheme.error
    WorkflowVisualState.RUNNING -> MaterialTheme.colorScheme.primary
    WorkflowVisualState.QUEUED -> MaterialTheme.colorScheme.tertiary
    WorkflowVisualState.CANCELLED, WorkflowVisualState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun statusLabel(state: WorkflowVisualState): String = state.name.lowercase().replaceFirstChar(Char::uppercase)

private fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
}
