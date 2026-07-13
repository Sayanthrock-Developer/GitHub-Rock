package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ApkInspector
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.ui.components.GlassCard
import java.io.File

/**
 * Displays the downloads list and provides controls for inspecting, sharing, cancelling, and deleting downloads.
 *
 * @param viewModel The view model that supplies downloads and handles download actions.
 */
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionFile by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var cancelTarget by remember { mutableStateOf<DownloadEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Downloads", style = MaterialTheme.typography.headlineSmall) }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Verified artifact pipeline", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Downloads continue through WorkManager, resume from partial files, calculate SHA-256 and expose APK inspection before Android's installer opens.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (downloads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxHeight(.62f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            null,
                            Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Queue an Actions artifact or release asset to see real progress here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        items(downloads, key = { it.id }) { item ->
            DownloadCard(
                item = item,
                onPause = { viewModel.pause(item) },
                onResume = { viewModel.resume(item) },
                onCancel = { cancelTarget = item },
                onRetry = { viewModel.retry(item) },
                onInspect = {
                    inspectionFile = item.localPath?.let(::File)
                    inspection = inspectionFile?.let { ApkInspector.inspect(context, it) }
                },
                onShare = { shareDownload(context, item) },
                onDelete = { deleteTarget = item }
            )
        }
    }

    inspection?.let { apk ->
        AlertDialog(
            onDismissRequest = { inspection = null },
            title = { Text(apk.appName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${apk.packageName} • ${apk.versionName} (${apk.versionCode})")
                    Text("${formatBytes(apk.fileSize)} • min SDK ${apk.minSdk} • target SDK ${apk.targetSdk}")
                    Text("SHA-256: ${apk.fileSha256}", style = MaterialTheme.typography.bodySmall)
                    Text("Signing: ${apk.signingSha256 ?: "Unavailable"}", style = MaterialTheme.typography.bodySmall)
                    Text("Installed signing match: ${apk.installedSignatureMatches?.toString() ?: "Not installed"}")
                    Text("Requested permissions: ${apk.permissions.size}")
                    apk.permissions.take(5).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (apk.permissions.size > 5) {
                        Text("+${apk.permissions.size - 5} more", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        inspectionFile?.let { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    }) { Text("Install") }
                    TextButton(onClick = { inspection = null; inspectionFile = null }) { Text("Close") }
                }
            }
        )
    }

    cancelTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel download?") },
            text = { Text("The partial file will be removed. The cancelled item remains in history and can be restarted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancel(item)
                    cancelTarget = null
                }) { Text("Cancel download") }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("Keep downloading") } }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete download?") },
            text = { Text("This removes the local file and its download history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } }
        )
    }
}

/**
 * Displays a download's status, progress, and available actions.
 *
 * @param item The download to display.
 * @param onPause Called when pausing the download.
 * @param onResume Called when resuming the download.
 * @param onCancel Called when canceling the download.
 * @param onRetry Called when retrying or restarting the download.
 * @param onInspect Called when inspecting a completed APK.
 * @param onShare Called when sharing the downloaded file.
 * @param onDelete Called when deleting the download.
 */
@Composable
private fun DownloadCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onInspect: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val controls = downloadControls(item.status)
    val active = item.status in setOf("queued", "downloading", "retrying")
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.status.replaceFirstChar { it.uppercase() },
                        color = downloadStatusColor(item.status),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (item.downloadedBytes > 0) {
                    Text(
                        if (item.totalBytes > 0) {
                            "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
                        } else {
                            formatBytes(item.downloadedBytes)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                item.totalBytes > 0 -> {
                    LinearProgressIndicator(
                        progress = {
                            (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                active -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (DownloadControl.Pause in controls) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Pause, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Pause")
                    }
                }
                if (DownloadControl.Resume in controls) {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Resume")
                    }
                }
                if (DownloadControl.Retry in controls) {
                    Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (item.status == "cancelled") "Restart" else "Retry")
                    }
                }
                if (DownloadControl.Cancel in controls) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Cancel, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Cancel")
                    }
                }
            }

            if (item.status == "completed") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.localPath?.endsWith(".apk", true) == true) {
                        TextButton(onClick = onInspect) { Text("Inspect APK") }
                    }
                    if (item.localPath != null) {
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Share")
                        }
                    }
                }
            }

            if (!active && item.status != "paused") {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * Selects the theme color associated with a download status.
 *
 * @param status The current download status.
 * @return The color to use when displaying the status.
 */
@Composable
private fun downloadStatusColor(status: String) = when (status) {
    "completed" -> MaterialTheme.colorScheme.tertiary
    "failed" -> MaterialTheme.colorScheme.error
    "cancelled", "paused" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

/**
 * Shares the downloaded file through the Android share sheet when it exists locally.
 *
 * @param context The context used to create the file URI and launch the share sheet.
 * @param item The download whose local file should be shared.
 */
private fun shareDownload(context: android.content.Context, item: DownloadEntity) {
    val file = item.localPath?.let(::File) ?: return
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            "Share ${item.fileName}"
        )
    )
}

/**
 * Formats a byte count using a human-readable unit.
 *
 * @param bytes The number of bytes to format.
 * @return The byte count expressed in gigabytes, megabytes, kilobytes, or bytes.
 */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
