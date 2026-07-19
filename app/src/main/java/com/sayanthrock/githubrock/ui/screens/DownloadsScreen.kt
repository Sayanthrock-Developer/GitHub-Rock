package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.ManualDownloadRequest
import com.sayanthrock.githubrock.core.model.ManualDownloadType
import com.sayanthrock.githubrock.core.model.validateManualDownload
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ApkInspector
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionFile by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var cancelTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var showAddDownload by rememberSaveable { mutableStateOf(false) }

    val active = downloads.count { it.status in setOf("queued", "downloading", "retrying") }
    val completed = downloads.count { it.status == "completed" }
    val failed = downloads.count { it.status == "failed" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StandardScreenHeader(
                    title = "Downloads",
                    subtitle = "Images, files, transfer status, and file safety",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = { showAddDownload = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Add")
                }
            }
        }
        item { DownloadOverviewCard(active, completed, failed) }

        if (downloads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text("No downloads yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Add a GitHub image or file, or queue an Actions artifact or release asset.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { showAddDownload = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add image or file")
                        }
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

    if (showAddDownload) {
        ManualDownloadDialog(
            onDismiss = { showAddDownload = false },
            onAdd = { request ->
                viewModel.enqueue(request.url, request.fileName)
                showAddDownload = false
            }
        )
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
                    apk.permissions.take(5).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    if (apk.permissions.size > 5) Text("+${apk.permissions.size - 5} more")
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
            text = { Text("The partial file will be removed. The item remains in history and can be restarted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancel(item); cancelTarget = null }) { Text("Cancel download") }
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
                TextButton(onClick = { viewModel.delete(item); deleteTarget = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep") } }
        )
    }
}

@Composable
private fun ManualDownloadDialog(
    onDismiss: () -> Unit,
    onAdd: (ManualDownloadRequest) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(ManualDownloadType.Image) }
    var url by rememberSaveable { mutableStateOf("") }
    var fileName by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download image or file") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose what you are saving. Only trusted GitHub HTTPS links are accepted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ManualDownloadType.Image,
                        onClick = { type = ManualDownloadType.Image; error = null },
                        label = { Text("Image") },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == ManualDownloadType.File,
                        onClick = { type = ManualDownloadType.File; error = null },
                        label = { Text("File") },
                        leadingIcon = {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    if (type == ManualDownloadType.Image) {
                        "Image · PNG, JPG, JPEG, WebP, or GIF from a GitHub link."
                    } else {
                        "File · APK, ZIP, PDF, source, or another published file."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("GitHub HTTPS download link") },
                    placeholder = { Text("https://github.com/…") },
                    singleLine = true,
                    isError = error != null
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it.take(120); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Save as filename") },
                    placeholder = { Text(if (type == ManualDownloadType.Image) "image.png" else "release.apk") },
                    singleLine = true,
                    supportingText = { Text("Optional — a safe name is created from the link.") }
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Saved privately inside the app. Completed items can be shared; APK files can also be inspected before installation.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val validation = validateManualDownload(type, url, fileName)
                validation.request?.let(onAdd) ?: run { error = validation.error }
            }) { Text("Add download") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DownloadOverviewCard(active: Int, completed: Int, failed: Int) {
    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Protected download queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Resumable transfers, SHA-256 verification, and APK inspection",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadMetric("Active", active, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                DownloadMetric("Completed", completed, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                DownloadMetric("Failed", failed, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DownloadMetric(label: String, value: Int, accent: Color, modifier: Modifier) {
    Surface(modifier, MaterialTheme.shapes.large, accent.copy(alpha = .09f)) {
        Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value.toString(),
                color = accent,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

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
    val isActive = item.status in setOf("queued", "downloading", "retrying")
    val progress = downloadProgressLevel(item.downloadedBytes, item.totalBytes, item.status) / 100f
    val accent = downloadStatusColor(item.status)
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(downloadStatusIcon(item.status), contentDescription = null, tint = accent)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(item.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${downloadTypeLabel(item.fileName)} · ${downloadFormatLabel(item.fileName)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = .12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .30f))
                ) {
                    Text(
                        item.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (item.downloadedBytes > 0) {
                Text(
                    if (item.totalBytes > 0) {
                        "${formatBytes(item.downloadedBytes)} of ${formatBytes(item.totalBytes)}"
                    } else {
                        formatBytes(item.downloadedBytes)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = accent
            )
            if (isActive && item.totalBytes <= 0) {
                Text(
                    "Waiting for the server to report file size",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    DownloadControl.Pause in controls -> OutlinedButton(onPause, Modifier.weight(1f)) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Pause")
                    }
                    DownloadControl.Resume in controls -> Button(onResume, Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Resume")
                    }
                    DownloadControl.Retry in controls -> Button(onRetry, Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (item.status == "cancelled") "Restart" else "Retry")
                    }
                    else -> Spacer(Modifier.weight(1f))
                }
                OutlinedButton({ expanded = !expanded }, Modifier.weight(1f)) {
                    Text(if (expanded) "Less" else "Options")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (DownloadControl.Cancel in controls) {
                    DownloadOption(Icons.Default.Cancel, "Cancel download", onCancel)
                }
                if (item.status == "completed" && item.localPath?.endsWith(".apk", true) == true) {
                    DownloadOption(Icons.Default.Security, "Inspect APK", onInspect)
                }
                if (item.status == "completed" && item.localPath != null) {
                    DownloadOption(
                        Icons.Default.Share,
                        if (isImageDownload(item.fileName)) "Share image" else "Share file",
                        onShare
                    )
                }
                if (!isActive && item.status != "paused") {
                    DownloadOption(Icons.Default.Delete, "Delete history and file", onDelete, destructive = true)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Added ${formatDownloadTime(item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    TextButton(onClick, Modifier.fillMaxWidth()) {
        val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint)
    }
}

@Composable
private fun downloadStatusColor(status: String): Color = when (status) {
    "completed" -> MaterialTheme.colorScheme.tertiary
    "failed" -> MaterialTheme.colorScheme.error
    "cancelled", "paused" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

private fun downloadStatusIcon(status: String): ImageVector = when (status) {
    "completed" -> Icons.Default.CheckCircle
    "failed" -> Icons.Default.ErrorOutline
    "paused" -> Icons.Default.Pause
    "cancelled" -> Icons.Default.Cancel
    "queued", "downloading", "retrying" -> Icons.Default.HourglassTop
    else -> Icons.Default.Download
}

internal fun downloadProgressLevel(downloadedBytes: Long, totalBytes: Long, status: String): Int = when {
    status == "completed" -> 100
    totalBytes <= 0 -> 0
    else -> (downloadedBytes.coerceAtLeast(0) * 100 / totalBytes)
        .toInt()
        .coerceIn(0, 100)
}

private fun isImageDownload(fileName: String): Boolean =
    fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US) in
        setOf("png", "jpg", "jpeg", "webp", "gif")

private fun downloadTypeLabel(fileName: String): String = if (isImageDownload(fileName)) "Image" else "File"

private fun downloadFormatLabel(fileName: String): String = fileName
    .substringAfterLast('.', missingDelimiterValue = "file")
    .ifBlank { "file" }
    .uppercase(Locale.US)

private fun downloadMimeType(fileName: String): String = when (
    fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)
) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "apk" -> "application/vnd.android.package-archive"
    "pdf" -> "application/pdf"
    "zip" -> "application/zip"
    "json" -> "application/json"
    "txt", "md", "kt", "java", "xml", "yaml", "yml" -> "text/plain"
    else -> "application/octet-stream"
}

private fun shareDownload(context: android.content.Context, item: DownloadEntity) {
    val file = item.localPath?.let(::File) ?: return
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = downloadMimeType(item.fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            "Share ${item.fileName}"
        )
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private val downloadTimeFormatter = DateTimeFormatter
    .ofPattern("dd MMM yyyy • hh:mm:ss a", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun formatDownloadTime(epochMillis: Long): String = runCatching {
    downloadTimeFormatter.format(Instant.ofEpochMilli(epochMillis))
}.getOrDefault("Time unavailable")
