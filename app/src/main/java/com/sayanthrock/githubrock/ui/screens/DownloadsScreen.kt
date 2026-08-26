package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sayanthrock.githubrock.core.model.DownloadMirror
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val selectedMirror by viewModel.selectedMirror.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var actionTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cancelTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showMirrors by rememberSaveable { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val actionTarget = actionTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    val deleteTarget = deleteTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    val cancelTarget = cancelTargetId?.let { id -> downloads.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StandardScreenHeader("Downloads", "Transfers, completed files, and download history") }
        item { DownloadCommandBar(selectedMirror, onChangeMirror = { showMirrors = true }) }
        if (downloads.isEmpty()) item { EmptyDownloadsCard() }
        items(downloads, key = { it.id }) { item ->
            DownloadCard(
                item = item,
                onPause = { viewModel.pause(item) },
                onResume = { viewModel.resume(item) },
                onRetry = { viewModel.retry(item) },
                onOpenActions = { actionTargetId = item.id }
            )
        }
    }

    if (showMirrors) {
        DownloadMirrorDialog(
            selected = selectedMirror,
            onSelect = viewModel::selectMirror,
            onDismiss = { showMirrors = false }
        )
    }

    actionTarget?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { actionTargetId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            DownloadActionsSheet(
                item = item,
                onPause = { viewModel.pause(item); actionTargetId = null },
                onResume = { viewModel.resume(item); actionTargetId = null },
                onRetry = { viewModel.retry(item); actionTargetId = null },
                onCancel = { actionTargetId = null; cancelTargetId = item.id },
                onShare = {
                    shareDownload(context, item).onFailure { actionError = it.message ?: "Android could not share this file." }
                    actionTargetId = null
                },
                onDelete = { actionTargetId = null; deleteTargetId = item.id }
            )
        }
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            icon = { Icon(Icons.Default.ErrorOutline, null) },
            title = { Text("File action unavailable") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("Close") } }
        )
    }

    cancelTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { cancelTargetId = null },
            title = { Text("Cancel download?") },
            text = { Text("The partial file will be removed. The item remains in history and can be restarted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancel(item); cancelTargetId = null }) { Text("Cancel download") }
            },
            dismissButton = { TextButton(onClick = { cancelTargetId = null }) { Text("Keep downloading") } }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete download?") },
            text = { Text("This removes the local file and its download history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(item); deleteTargetId = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTargetId = null }) { Text("Keep") } }
        )
    }
}

@Composable
internal fun DownloadCommandBar(selectedMirror: DownloadMirror, onChangeMirror: () -> Unit) {
    Surface(
        onClick = onChangeMirror,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Download source", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedMirror.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun EmptyDownloadsCard() {
    GlassCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Text("No downloads yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Files downloaded from repositories, releases, builds, and other GitHub actions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DownloadCard(item: DownloadEntity, onPause: () -> Unit, onResume: () -> Unit, onRetry: () -> Unit, onOpenActions: () -> Unit) {
    val controls = downloadControls(item.status)
    val progress = downloadProgressLevel(item.downloadedBytes, item.totalBytes, item.status)
    val accent = downloadStatusColor(item.status)
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(downloadStatusIcon(item.status), null, tint = accent)
                Column(Modifier.weight(1f)) {
                    Text(item.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${downloadTypeLabel(item.fileName)} · ${downloadFormatLabel(item.fileName)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DownloadStatusPill(item.status, accent)
            }
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = accent, trackColor = accent.copy(alpha = .14f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    DownloadControl.Pause in controls -> OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause") }
                    DownloadControl.Resume in controls -> Button(onClick = onResume, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume") }
                    DownloadControl.Retry in controls -> Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text(if (item.status == "cancelled") "Restart" else "Retry") }
                    else -> Spacer(Modifier.weight(1f))
                }
                OutlinedButton(onClick = onOpenActions, modifier = Modifier.weight(1f)) { Icon(Icons.Default.MoreHoriz, null); Spacer(Modifier.width(6.dp)); Text("Actions") }
            }
            Text("Added ${formatDownloadTime(item.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DownloadStatusPill(status: String, accent: Color) {
    Surface(shape = MaterialTheme.shapes.large, color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .28f))) {
        Text(status.replaceFirstChar { it.uppercase() }, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DownloadActionsSheet(item: DownloadEntity, onPause: () -> Unit, onResume: () -> Unit, onRetry: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val controls = downloadControls(item.status)
    val active = item.status in setOf("queued", "downloading", "retrying", "paused")
    val hasFile = item.localPath?.let(::File)?.exists() == true
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(item.fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        HorizontalDivider()
        if (DownloadControl.Pause in controls) SheetAction(Icons.Default.Pause, "Pause download", "Keep progress and resume later", onPause)
        if (DownloadControl.Resume in controls) SheetAction(Icons.Default.PlayArrow, "Resume download", "Continue from saved progress", onResume)
        if (DownloadControl.Retry in controls) SheetAction(Icons.Default.Refresh, if (item.status == "cancelled") "Restart download" else "Retry download", "Start this transfer again", onRetry)
        if (DownloadControl.Cancel in controls) SheetAction(Icons.Default.Cancel, "Cancel download", "Remove the current partial file", onCancel, true)
        if (hasFile && item.status == "completed") SheetAction(Icons.Default.Share, "Share file", "Send the completed file using Android share", onShare)
        if (!active) SheetAction(Icons.Default.Delete, "Delete history and file", "Permanently remove the local file and record", onDelete, true)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SheetAction(icon: ImageVector, title: String, detail: String, onClick: () -> Unit, destructive: Boolean = false) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = tint.copy(alpha = .08f), border = BorderStroke(1.dp, tint.copy(alpha = .22f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = tint)
            Column(Modifier.weight(1f)) { Text(title, color = tint, fontWeight = FontWeight.Bold); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.ChevronRight, null, tint = tint)
        }
    }
}

@Composable
private fun DownloadMirrorDialog(selected: DownloadMirror, onSelect: (DownloadMirror) -> Unit, onDismiss: () -> Unit) {
    var pending by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download source") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Official GitHub is the default. Community endpoints are third-party services.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DownloadMirror.entries.forEach { mirror ->
                    Surface(onClick = { pending = mirror }, color = Color.Transparent) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(mirror.label, fontWeight = FontWeight.SemiBold); Text(if (mirror.community) "Community endpoint" else "Official GitHub endpoint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            RadioButton(selected = pending == mirror, onClick = { pending = mirror })
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSelect(pending); onDismiss() }) { Text("Use selected") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private enum class DownloadControl { Pause, Resume, Retry, Cancel }

private fun downloadControls(status: String): Set<DownloadControl> = when (status) {
    "queued", "downloading", "retrying" -> setOf(DownloadControl.Pause, DownloadControl.Cancel)
    "paused" -> setOf(DownloadControl.Resume, DownloadControl.Cancel)
    "failed", "cancelled" -> setOf(DownloadControl.Retry)
    else -> emptySet()
}

private fun downloadStatusColor(status: String): Color = when (status) {
    "completed" -> Color(0xFF2E7D32)
    "failed" -> Color(0xFFC62828)
    "cancelled", "paused" -> Color.Gray
    else -> Color(0xFF1565C0)
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
    else -> (downloadedBytes.coerceAtLeast(0) * 100 / totalBytes).toInt().coerceIn(0, 100)
}

private fun isImageDownload(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase(Locale.US) in setOf("png", "jpg", "jpeg", "webp", "gif")
private fun downloadTypeLabel(fileName: String): String = if (isImageDownload(fileName)) "Image" else "File"
private fun downloadFormatLabel(fileName: String): String = fileName.substringAfterLast('.', "file").ifBlank { "file" }.uppercase(Locale.US)

private fun downloadMimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "apk" -> "application/vnd.android.package-archive"
    "pdf" -> "application/pdf"
    "zip" -> "application/zip"
    else -> "application/octet-stream"
}

private fun shareDownload(context: Context, item: DownloadEntity): Result<Unit> = runCatching {
    val file = requireNotNull(item.localPath?.let(::File)) { "The downloaded file is no longer available." }
    require(file.exists()) { "The downloaded file is no longer available." }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = downloadMimeType(item.fileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }, "Share ${item.fileName}"))
}

private val downloadTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()).withZone(ZoneId.systemDefault())
private fun formatDownloadTime(epochMillis: Long): String = runCatching { downloadTimeFormatter.format(Instant.ofEpochMilli(epochMillis)) }.getOrDefault("Time unavailable")
