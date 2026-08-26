package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.data.local.DownloadEntity
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class DownloadListFilter(val label: String) {
    All("All"),
    Active("Active"),
    Applications("Apps"),
    Files("Files"),
    Completed("Completed")
}

internal fun filterDownloads(
    downloads: List<DownloadEntity>,
    filter: DownloadListFilter
): List<DownloadEntity> = when (filter) {
    DownloadListFilter.All -> downloads
    DownloadListFilter.Active -> downloads.filter {
        it.status in setOf("queued", "downloading", "retrying", "paused")
    }
    DownloadListFilter.Applications -> downloads.filter { it.isApkDownload() }
    DownloadListFilter.Files -> downloads.filterNot { it.isApkDownload() }
    DownloadListFilter.Completed -> downloads.filter { it.status == "completed" }
}

internal fun preferredApplicationName(fileName: String, extractedLabel: String?): String =
    extractedLabel?.trim()?.takeIf(String::isNotBlank)
        ?: fileName.substringBeforeLast('.').replace('-', ' ').replace('_', ' ').trim()
            .ifBlank { fileName }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsRedesignScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFilterName by rememberSaveable { mutableStateOf(DownloadListFilter.All.name) }
    val selectedFilter = DownloadListFilter.entries.firstOrNull { it.name == selectedFilterName }
        ?: DownloadListFilter.All

    var actionTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val actionTarget = actionTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    var cancelTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val cancelTarget = cancelTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteTarget = deleteTargetId?.let { id -> downloads.firstOrNull { it.id == id } }

    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DownloadsRedesignContent(
        downloads = downloads,
        selectedFilter = selectedFilter,
        onSelectFilter = { selectedFilterName = it.name },
        onPrimaryAction = { item ->
            when (item.status) {
                "downloading", "queued", "retrying" -> viewModel.pause(item)
                "paused" -> viewModel.resume(item)
                "failed", "cancelled" -> viewModel.retry(item)
                "completed" -> {
                    val result = if (item.isApkDownload()) {
                        openDownloadedApk(context, item)
                    } else {
                        shareDownloadedFile(context, item)
                    }
                    result.onFailure {
                        errorMessage = it.message ?: "Android could not open this download."
                    }
                }
                else -> actionTargetId = item.id
            }
        },
        onOpenActions = { actionTargetId = it.id }
    )

    actionTarget?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { actionTargetId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            DownloadActionsSheet(
                item = item,
                onPause = {
                    viewModel.pause(item)
                    actionTargetId = null
                },
                onResume = {
                    viewModel.resume(item)
                    actionTargetId = null
                },
                onRetry = {
                    viewModel.retry(item)
                    actionTargetId = null
                },
                onCancel = {
                    actionTargetId = null
                    cancelTargetId = item.id
                },
                onInstall = {
                    openDownloadedApk(context, item).onFailure {
                        errorMessage = it.message ?: "Android could not open this APK."
                    }
                    actionTargetId = null
                },
                onInspect = {
                    val file = item.localPath?.let(::File)?.takeIf(File::exists)
                    actionTargetId = null
                    if (file == null) {
                        errorMessage = "The downloaded APK file is no longer available."
                    } else {
                        inspectionLoading = true
                        viewModel.inspectApk(file) { result ->
                            inspectionLoading = false
                            inspection = result.getOrNull()
                            errorMessage = result.exceptionOrNull()?.message
                        }
                    }
                },
                onShare = {
                    shareDownloadedFile(context, item).onFailure {
                        errorMessage = it.message ?: "Android could not share this file."
                    }
                    actionTargetId = null
                },
                onDelete = {
                    actionTargetId = null
                    deleteTargetId = item.id
                }
            )
        }
    }

    if (inspectionLoading) {
        AlertDialog(
            onDismissRequest = {},
            icon = { CircularProgressIndicator(modifier = Modifier.size(28.dp)) },
            title = { Text("Inspecting APK") },
            text = { Text("Reading the application identity, SDK, signature, hash, and permissions.") },
            confirmButton = {}
        )
    }

    inspection?.let { apk ->
        ApkSummaryDialog(apk = apk, onDismiss = { inspection = null })
    }

    cancelTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { cancelTargetId = null },
            title = { Text("Cancel download?") },
            text = { Text("The partial file will be removed. You can restart this download later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancel(item)
                        cancelTargetId = null
                    }
                ) { Text("Cancel download") }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetId = null }) { Text("Keep downloading") }
            }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete download?") },
            text = { Text("This removes the local file and its download history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(item)
                        deleteTargetId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("Keep") }
            }
        )
    }

    errorMessage?.takeIf(String::isNotBlank)?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("Action unavailable") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("Close") }
            }
        )
    }
}

@Composable
internal fun DownloadsRedesignContent(
    downloads: List<DownloadEntity>,
    selectedFilter: DownloadListFilter,
    onSelectFilter: (DownloadListFilter) -> Unit,
    onPrimaryAction: (DownloadEntity) -> Unit,
    onOpenActions: (DownloadEntity) -> Unit
) {
    val visibleDownloads = remember(downloads, selectedFilter) {
        filterDownloads(downloads, selectedFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DownloadFilterRow(
                selected = selectedFilter,
                onSelect = onSelectFilter
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedFilter.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${visibleDownloads.size} item${if (visibleDownloads.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (visibleDownloads.isEmpty()) {
            item { EmptyDownloadsCard(selectedFilter) }
        }

        items(visibleDownloads, key = { it.id }) { item ->
            DownloadListCard(
                item = item,
                onPrimaryAction = { onPrimaryAction(item) },
                onOpenActions = { onOpenActions(item) }
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DownloadFilterRow(
    selected: DownloadListFilter,
    onSelect: (DownloadListFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DownloadListFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun EmptyDownloadsCard(filter: DownloadListFilter) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                if (filter == DownloadListFilter.All) "No downloads yet" else "No ${filter.label.lowercase()} downloads",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                if (filter == DownloadListFilter.All) {
                    "Repository files, release assets, APKs, and build artifacts will appear here automatically."
                } else {
                    "Downloads matching this filter will appear here automatically."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DownloadListCard(
    item: DownloadEntity,
    onPrimaryAction: () -> Unit,
    onOpenActions: () -> Unit
) {
    val progress = downloadProgressPercent(item)
    val accent = downloadStatusColor(item.status)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (item.isApkDownload()) Icons.Default.Android else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${downloadTypeLabel(item.fileName)} · ${item.fileName.substringAfterLast('.', "FILE").uppercase()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(downloadStatusLabel(item.status), accent)
            }

            if (item.status in setOf("queued", "downloading", "retrying", "paused") || item.downloadedBytes > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (item.totalBytes > 0) {
                            "${formatDownloadBytes(item.downloadedBytes)} of ${formatDownloadBytes(item.totalBytes)}"
                        } else {
                            formatDownloadBytes(item.downloadedBytes)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$progress%",
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = .14f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(primaryActionIcon(item), contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(primaryActionLabel(item), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onOpenActions) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More actions for ${item.fileName}")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Added ${formatDownloadTimestamp(item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, accent: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DownloadActionsSheet(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onInspect: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val active = item.status in setOf("queued", "downloading", "retrying")
    val localFileExists = item.localPath?.let(::File)?.exists() == true

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            item.fileName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (active) ActionRow(Icons.Default.Pause, "Pause download", onPause)
        if (item.status == "paused") ActionRow(Icons.Default.PlayArrow, "Resume download", onResume)
        if (item.status in setOf("failed", "cancelled")) ActionRow(Icons.Default.Refresh, "Retry download", onRetry)
        if (active || item.status == "paused") {
            ActionRow(Icons.Default.Cancel, "Cancel download", onCancel, destructive = true)
        }
        if (item.status == "completed" && item.isApkDownload() && localFileExists) {
            ActionRow(Icons.Default.InstallMobile, "Install application", onInstall)
            ActionRow(Icons.Default.Security, "Inspect APK", onInspect)
        }
        if (item.status == "completed" && localFileExists) {
            ActionRow(Icons.Default.Share, "Share file", onShare)
        }
        if (!active && item.status != "paused") {
            ActionRow(Icons.Default.Delete, "Delete file and history", onDelete, destructive = true)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = tint.copy(alpha = .08f),
        border = BorderStroke(1.dp, tint.copy(alpha = .20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(title, color = tint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ApkSummaryDialog(apk: ApkInspection, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(apk.appName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("Package", apk.packageName)
                SummaryRow("Version", "${apk.versionName} (${apk.versionCode})")
                SummaryRow("SDK", "API ${apk.minSdk}–${apk.targetSdk}")
                SummaryRow("Size", formatDownloadBytes(apk.fileSize))
                SummaryRow("Permissions", apk.permissions.size.toString())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun downloadStatusColor(status: String): Color = when (status) {
    "completed" -> MaterialTheme.colorScheme.primary
    "failed", "cancelled" -> MaterialTheme.colorScheme.error
    "paused" -> MaterialTheme.colorScheme.tertiary
    "queued", "retrying" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

private fun downloadStatusLabel(status: String): String = when (status) {
    "retrying" -> "Retrying"
    "downloading" -> "Downloading"
    "completed" -> "Completed"
    "cancelled" -> "Cancelled"
    "failed" -> "Failed"
    "paused" -> "Paused"
    "queued" -> "Queued"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun primaryActionLabel(item: DownloadEntity): String = when (item.status) {
    "downloading", "queued", "retrying" -> "Pause"
    "paused" -> "Resume"
    "failed", "cancelled" -> "Retry"
    "completed" -> if (item.isApkDownload()) "Install" else "Share"
    else -> "Actions"
}

private fun primaryActionIcon(item: DownloadEntity): ImageVector = when (item.status) {
    "downloading", "queued", "retrying" -> Icons.Default.Pause
    "paused" -> Icons.Default.PlayArrow
    "failed", "cancelled" -> Icons.Default.Refresh
    "completed" -> if (item.isApkDownload()) Icons.Default.InstallMobile else Icons.Default.Share
    else -> Icons.Default.MoreHoriz
}

private fun downloadProgressPercent(item: DownloadEntity): Int = when {
    item.status == "completed" -> 100
    item.totalBytes <= 0L -> 0
    else -> ((item.downloadedBytes.coerceAtLeast(0L) * 100L) / item.totalBytes.coerceAtLeast(1L))
        .coerceIn(0L, 100L).toInt()
}

private fun downloadTypeLabel(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "apk", "aab" -> "Application"
        "png", "jpg", "jpeg", "webp", "gif", "svg" -> "Image"
        "zip", "tar", "gz", "7z", "rar" -> "Archive"
        "pdf", "doc", "docx", "txt", "md" -> "Document"
        else -> "File"
    }

private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)

private fun formatDownloadBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    return when {
        safe >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", safe / (1024 * 1024 * 1024))
        safe >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", safe / (1024 * 1024))
        safe >= 1024 -> String.format(Locale.US, "%.1f KB", safe / 1024)
        else -> "${safe.toLong()} B"
    }
}

private fun formatDownloadTimestamp(value: Long): String = runCatching {
    DateTimeFormatter.ofPattern("dd MMM yyyy · h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}.getOrDefault("Recently")

private fun openDownloadedApk(context: Context, item: DownloadEntity): Result<Unit> = runCatching {
    require(item.isApkDownload()) { "This download is not an APK." }
    val file = item.localPath?.let(::File)?.takeIf(File::exists)
        ?: error("The downloaded APK file is no longer available.")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun shareDownloadedFile(context: Context, item: DownloadEntity): Result<Unit> = runCatching {
    val file = item.localPath?.let(::File)?.takeIf(File::exists)
        ?: error("The downloaded file is no longer available.")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val mime = if (item.isApkDownload()) {
        "application/vnd.android.package-archive"
    } else {
        "application/octet-stream"
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share ${item.fileName}"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
