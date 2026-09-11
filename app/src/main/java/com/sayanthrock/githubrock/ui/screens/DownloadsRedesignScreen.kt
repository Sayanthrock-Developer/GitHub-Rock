package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
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
import com.sayanthrock.githubrock.core.util.InstalledApkStateResolver
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.DownloadState
import com.sayanthrock.githubrock.data.local.state
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class DownloadListFilter(val label: String) {
    All("All"), Active("Active"), Completed("Completed"), Failed("Failed"), Paused("Paused"), Queued("Queued"), Applications("Apps"), Files("Files")
}

internal fun filterDownloads(downloads: List<DownloadEntity>, filter: DownloadListFilter): List<DownloadEntity> = when (filter) {
    DownloadListFilter.All -> downloads
    DownloadListFilter.Active -> downloads.filter { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.RETRYING }
    DownloadListFilter.Completed -> downloads.filter { it.state == DownloadState.COMPLETED || it.state == DownloadState.INSTALLABLE }
    DownloadListFilter.Failed -> downloads.filter { it.state == DownloadState.FAILED || it.state == DownloadState.CANCELLED }
    DownloadListFilter.Paused -> downloads.filter { it.state == DownloadState.PAUSED }
    DownloadListFilter.Queued -> downloads.filter { it.state == DownloadState.QUEUED }
    DownloadListFilter.Applications -> downloads.filter { it.isApkDownload() }
    DownloadListFilter.Files -> downloads.filterNot { it.isApkDownload() }
}

internal fun preferredApplicationName(fileName: String, extractedLabel: String?): String =
    extractedLabel?.trim()?.takeIf(String::isNotBlank)
        ?: fileName.substringBeforeLast('.').replace('-', ' ').replace('_', ' ').trim().ifBlank { fileName }

internal fun downloadRepositoryOwner(repositoryFullName: String): String? =
    repositoryFullName.substringBefore('/', missingDelimiterValue = "").trim().takeIf { it.isNotBlank() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsRedesignScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onOpenProfile: (String) -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedFilterName by rememberSaveable { mutableStateOf(DownloadListFilter.All.name) }
    val selectedFilter = DownloadListFilter.entries.firstOrNull { it.name == selectedFilterName } ?: DownloadListFilter.All
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
            when (item.state) {
                DownloadState.DOWNLOADING, DownloadState.QUEUED, DownloadState.RETRYING -> viewModel.pause(item)
                DownloadState.PAUSED -> viewModel.resume(item)
                DownloadState.FAILED, DownloadState.CANCELLED -> viewModel.retry(item)
                DownloadState.INSTALLABLE -> openDownloadedApk(context, item).onFailure { errorMessage = it.message }
                DownloadState.COMPLETED -> if (item.isApkDownload()) openInstalledApplication(context, item).onFailure { errorMessage = it.message } else shareDownloadedFile(context, item).onFailure { errorMessage = it.message }
                DownloadState.VERIFYING -> Unit
            }
        },
        onOpenActions = { actionTargetId = it.id },
        onOpenProfile = onOpenProfile
    )

    actionTarget?.let { item ->
        ModalBottomSheet(onDismissRequest = { actionTargetId = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            DownloadActionsSheet(
                item = item,
                onPause = { viewModel.pause(item); actionTargetId = null },
                onResume = { viewModel.resume(item); actionTargetId = null },
                onRetry = { viewModel.retry(item); actionTargetId = null },
                onCancel = { actionTargetId = null; cancelTargetId = item.id },
                onInstall = { openDownloadedApk(context, item).onFailure { errorMessage = it.message }; actionTargetId = null },
                onOpen = { openInstalledApplication(context, item).onFailure { errorMessage = it.message }; actionTargetId = null },
                onInspect = {
                    val file = item.localPath?.let(::File)?.takeIf(File::exists)
                    actionTargetId = null
                    if (file == null) errorMessage = "The downloaded APK file is no longer available. Download it again."
                    else {
                        inspectionLoading = true
                        viewModel.inspectApk(file) { result ->
                            inspectionLoading = false
                            inspection = result.getOrNull()
                            errorMessage = result.exceptionOrNull()?.message
                        }
                    }
                },
                onShare = { shareDownloadedFile(context, item).onFailure { errorMessage = it.message }; actionTargetId = null },
                onViewRelease = { viewRelease(context, item).onFailure { errorMessage = it.message }; actionTargetId = null },
                onDelete = { actionTargetId = null; deleteTargetId = item.id }
            )
        }
    }
    if (inspectionLoading) AlertDialog(onDismissRequest = {}, icon = { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }, title = { Text("Inspecting APK") }, text = { Text("Reading the application identity, SDK, signature, hash, and permissions.") }, confirmButton = {})
    inspection?.let { apk -> ApkSummaryDialog(apk = apk, onDismiss = { inspection = null }) }
    cancelTarget?.let { item ->
        AlertDialog(onDismissRequest = { cancelTargetId = null }, title = { Text("Cancel download?") }, text = { Text("The partial file will be removed. You can restart this download later.") }, confirmButton = { TextButton(onClick = { viewModel.cancel(item); cancelTargetId = null }) { Text("Cancel download") } }, dismissButton = { TextButton(onClick = { cancelTargetId = null }) { Text("Keep downloading") } })
    }
    deleteTarget?.let { item ->
        AlertDialog(onDismissRequest = { deleteTargetId = null }, title = { Text("Delete download?") }, text = { Text("This removes the local file and its download history. This cannot be undone.") }, confirmButton = { TextButton(onClick = { viewModel.delete(item); deleteTargetId = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteTargetId = null }) { Text("Keep") } })
    }
    errorMessage?.takeIf(String::isNotBlank)?.let { message ->
        AlertDialog(onDismissRequest = { errorMessage = null }, icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) }, title = { Text("Action unavailable") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("Close") } })
    }
}

@Composable
internal fun DownloadsRedesignContent(
    downloads: List<DownloadEntity>, selectedFilter: DownloadListFilter, onSelectFilter: (DownloadListFilter) -> Unit,
    onPrimaryAction: (DownloadEntity) -> Unit, onOpenActions: (DownloadEntity) -> Unit, onOpenProfile: (String) -> Unit
) {
    val visibleDownloads = remember(downloads, selectedFilter) { filterDownloads(downloads, selectedFilter) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 44.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { DownloadFilterRow(selected = selectedFilter, downloads = downloads, onSelect = onSelectFilter) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(selectedFilter.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("${visibleDownloads.size} item${if (visibleDownloads.size == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        if (visibleDownloads.isEmpty()) item { EmptyDownloadsCard(selectedFilter) }
        items(visibleDownloads, key = { it.id }) { item -> DownloadListCard(item, { onPrimaryAction(item) }, { onOpenActions(item) }, onOpenProfile) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DownloadFilterRow(selected: DownloadListFilter, downloads: List<DownloadEntity>, onSelect: (DownloadListFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(DownloadListFilter.entries, key = { it.name }) { filter -> FilterChip(selected = filter == selected, onClick = { onSelect(filter) }, label = { Text("${filter.label} · ${filterDownloads(downloads, filter).size}") }) } }
}

@Composable
private fun EmptyDownloadsCard(filter: DownloadListFilter) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(64.dp), MaterialTheme.shapes.extraLarge, MaterialTheme.colorScheme.primary.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) } }
            Text(if (filter == DownloadListFilter.All) "No downloads yet" else "No ${filter.label.lowercase()} downloads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(if (filter == DownloadListFilter.All) "Repository files, release assets, APKs, and build artifacts will appear here automatically." else "Downloads matching this filter will appear here automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DownloadListCard(item: DownloadEntity, onPrimaryAction: () -> Unit, onOpenActions: () -> Unit, onOpenProfile: (String) -> Unit) {
    val state = item.state
    val progress = downloadProgressPercent(item)
    val accent = downloadStatusColor(state)
    val isTerminal = state == DownloadState.COMPLETED || state == DownloadState.INSTALLABLE
    val localFileExists = item.localPath?.let(::File)?.isFile == true
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(56.dp), MaterialTheme.shapes.extraLarge, MaterialTheme.colorScheme.surfaceContainerHigh) { Box(contentAlignment = Alignment.Center) { Icon(if (item.isApkDownload()) Icons.Default.Android else Icons.Default.InsertDriveFile, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary) } }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.repositoryFullName?.takeIf { it.isNotBlank() }?.let { fullName ->
                        val owner = downloadRepositoryOwner(fullName)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (owner != null) {
                                Text(owner, modifier = Modifier.clickable { onOpenProfile(owner) }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fullName.removePrefix(owner), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else Text(fullName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    val release = item.releaseName?.takeIf { it.isNotBlank() }
                    Text(listOfNotNull(release, "${downloadTypeLabel(item.fileName)} · ${item.fileName.substringAfterLast('.', "FILE").uppercase()}").joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusBadge(downloadStatusLabel(state), accent)
            }
            if (state in setOf(DownloadState.QUEUED, DownloadState.DOWNLOADING, DownloadState.RETRYING, DownloadState.PAUSED, DownloadState.VERIFYING) || item.downloadedBytes > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (item.totalBytes > 0) "${formatDownloadBytes(item.downloadedBytes)} of ${formatDownloadBytes(item.totalBytes)}" else formatDownloadBytes(item.downloadedBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (state == DownloadState.VERIFYING) "Verifying" else "$progress%", color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                LinearProgressIndicator(progress = { if (state == DownloadState.VERIFYING) 1f else progress / 100f }, Modifier.fillMaxWidth().height(7.dp), color = accent, trackColor = accent.copy(alpha = .14f))
            }
            if (state == DownloadState.DOWNLOADING || state == DownloadState.RETRYING || state == DownloadState.PAUSED) TransferMeta(item)
            if (isTerminal && item.sha256 != null) Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary); Text("Verified ✓", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            if (state == DownloadState.FAILED && !item.errorMessage.isNullOrBlank()) Text(item.errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (isTerminal && item.isApkDownload() && !localFileExists) Text("APK file is missing. Retry to download it again.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Button(onClick = onPrimaryAction, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent), enabled = state != DownloadState.VERIFYING) { Icon(primaryActionIcon(item), null); Spacer(Modifier.width(7.dp)); Text(primaryActionLabel(item), fontWeight = FontWeight.Bold) }; IconButton(onClick = onOpenActions) { Icon(Icons.Default.MoreHoriz, "More actions for ${item.fileName}") } }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Added ${formatDownloadTimestamp(item.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun TransferMeta(item: DownloadEntity) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { val speed = item.speedBytesPerSecond; Text(if (speed > 0) "${formatDownloadBytes(speed)}/s" else "Speed unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); val eta = item.etaSeconds; Text(if (eta != null && eta >= 0) "ETA ${formatEta(eta)}" else "ETA unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun StatusBadge(label: String, accent: Color) { Surface(shape = MaterialTheme.shapes.large, color = accent.copy(alpha = .12f), border = BorderStroke(1.dp, accent.copy(alpha = .28f))) { Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) } }

@Composable
private fun DownloadActionsSheet(item: DownloadEntity, onPause: () -> Unit, onResume: () -> Unit, onRetry: () -> Unit, onCancel: () -> Unit, onInstall: () -> Unit, onOpen: () -> Unit, onInspect: () -> Unit, onShare: () -> Unit, onViewRelease: () -> Unit, onDelete: () -> Unit) {
    val state = item.state
    val localFileExists = item.localPath?.let(::File)?.isFile == true
    val terminal = state == DownloadState.COMPLETED || state == DownloadState.INSTALLABLE
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(item.fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        item.repositoryFullName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item.releaseName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (state == DownloadState.DOWNLOADING || state == DownloadState.QUEUED || state == DownloadState.RETRYING) ActionRow(Icons.Default.Pause, "Pause download", onPause)
        if (state == DownloadState.PAUSED) ActionRow(Icons.Default.PlayArrow, "Resume download", onResume)
        if (state == DownloadState.FAILED || state == DownloadState.CANCELLED) ActionRow(Icons.Default.Refresh, "Retry download", onRetry)
        if (state == DownloadState.DOWNLOADING || state == DownloadState.QUEUED || state == DownloadState.RETRYING || state == DownloadState.PAUSED) ActionRow(Icons.Default.Cancel, "Cancel download", onCancel, destructive = true)
        if (terminal && item.isApkDownload() && localFileExists) { ActionRow(Icons.Default.InstallMobile, "Install application", onInstall); ActionRow(Icons.Default.OpenInNew, "Open installed application", onOpen); ActionRow(Icons.Default.Security, "Inspect APK", onInspect) }
        if (terminal && localFileExists) ActionRow(Icons.Default.Share, "Share file", onShare)
        if (item.releaseUrl?.isNotBlank() == true) ActionRow(Icons.Default.Link, "View release", onViewRelease)
        if (state != DownloadState.DOWNLOADING && state != DownloadState.QUEUED && state != DownloadState.RETRYING && state != DownloadState.PAUSED && state != DownloadState.VERIFYING) ActionRow(Icons.Default.Delete, "Delete file and history", onDelete, destructive = true)
    }
}

@Composable private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit, destructive: Boolean = false) { val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary; Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = tint.copy(alpha = .08f), border = BorderStroke(1.dp, tint.copy(alpha = .20f))) { Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = tint); Text(title, color = tint, fontWeight = FontWeight.Bold) } } }
@Composable private fun ApkSummaryDialog(apk: ApkInspection, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(apk.appName) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SummaryRow("Package", apk.packageName); SummaryRow("Version", "${apk.versionName} (${apk.versionCode})"); SummaryRow("SDK", "API ${apk.minSdk}–${apk.targetSdk}"); SummaryRow("Size", formatDownloadBytes(apk.fileSize)); SummaryRow("Permissions", apk.permissions.size.toString()) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }) }
@Composable private fun SummaryRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis) } }

@Composable
private fun downloadStatusColor(state: DownloadState): Color = when (state) { DownloadState.COMPLETED, DownloadState.INSTALLABLE -> MaterialTheme.colorScheme.primary; DownloadState.FAILED, DownloadState.CANCELLED -> MaterialTheme.colorScheme.error; DownloadState.PAUSED -> MaterialTheme.colorScheme.tertiary; DownloadState.QUEUED -> MaterialTheme.colorScheme.secondary; DownloadState.DOWNLOADING, DownloadState.RETRYING, DownloadState.VERIFYING -> MaterialTheme.colorScheme.primary }
private fun downloadStatusLabel(state: DownloadState): String = when (state) { DownloadState.QUEUED -> "Queued"; DownloadState.DOWNLOADING -> "Downloading"; DownloadState.PAUSED -> "Paused"; DownloadState.VERIFYING -> "Verifying"; DownloadState.COMPLETED -> "Completed"; DownloadState.INSTALLABLE -> "Installable"; DownloadState.FAILED -> "Failed"; DownloadState.RETRYING -> "Retrying"; DownloadState.CANCELLED -> "Cancelled" }
private fun primaryActionLabel(item: DownloadEntity): String = when (item.state) { DownloadState.DOWNLOADING, DownloadState.QUEUED, DownloadState.RETRYING -> "Pause"; DownloadState.PAUSED -> "Resume"; DownloadState.FAILED, DownloadState.CANCELLED -> "Retry"; DownloadState.INSTALLABLE -> "Install"; DownloadState.COMPLETED -> if (item.isApkDownload()) "Open" else "Share"; DownloadState.VERIFYING -> "Verifying" }
private fun primaryActionIcon(item: DownloadEntity): ImageVector = when (item.state) { DownloadState.DOWNLOADING, DownloadState.QUEUED, DownloadState.RETRYING -> Icons.Default.Pause; DownloadState.PAUSED -> Icons.Default.PlayArrow; DownloadState.FAILED, DownloadState.CANCELLED -> Icons.Default.Refresh; DownloadState.INSTALLABLE -> Icons.Default.InstallMobile; DownloadState.COMPLETED -> if (item.isApkDownload()) Icons.Default.OpenInNew else Icons.Default.Share; DownloadState.VERIFYING -> Icons.Default.Security }
private fun downloadProgressPercent(item: DownloadEntity): Int = when { item.state == DownloadState.COMPLETED || item.state == DownloadState.INSTALLABLE -> 100; item.totalBytes <= 0L -> 0; else -> ((item.downloadedBytes.coerceAtLeast(0L) * 100L) / item.totalBytes.coerceAtLeast(1L)).coerceIn(0L, 100L).toInt() }
private fun downloadTypeLabel(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) { "apk", "aab" -> "Application"; "png", "jpg", "jpeg", "webp", "gif", "svg" -> "Image"; "zip", "tar", "gz", "7z", "rar" -> "Archive"; "pdf", "doc", "docx", "txt", "md" -> "Document"; else -> "File" }
private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)
private fun formatDownloadBytes(bytes: Long): String { val safe = bytes.coerceAtLeast(0L).toDouble(); return when { safe >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", safe / (1024 * 1024 * 1024)); safe >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", safe / (1024 * 1024)); safe >= 1024 -> String.format(Locale.US, "%.1f KB", safe / 1024); else -> "${safe.toLong()} B" } }
private fun formatEta(seconds: Long): String = when { seconds < 60 -> "${seconds}s"; seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"; else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m" }
private fun formatDownloadTimestamp(value: Long): String = runCatching { DateTimeFormatter.ofPattern("dd MMM yyyy · h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value)) }.getOrDefault("Recently")
private fun openDownloadedApk(context: Context, item: DownloadEntity): Result<Unit> = runCatching { require(item.isApkDownload()) { "This download is not an APK." }; val file = item.localPath?.let(::File)?.takeIf(File::isFile) ?: error("The downloaded APK file is no longer available. Download it again."); InstalledApkStateResolver.launchInstaller(context, file).getOrThrow() }
private fun openInstalledApplication(context: Context, item: DownloadEntity): Result<Unit> = runCatching { require(item.isApkDownload()) { "This download is not an APK application." }; val file = item.localPath?.let(::File)?.takeIf(File::isFile) ?: error("The downloaded APK file is no longer available. Download it again."); val state = InstalledApkStateResolver.resolve(context, file) ?: error("Unable to read the installed application identity."); require(state.installed) { "This application is not installed yet. Use Install first." }; require(InstalledApkStateResolver.launchInstalledApp(context, state)) { "The installed application cannot be opened on this device." } }
private fun shareDownloadedFile(context: Context, item: DownloadEntity): Result<Unit> = runCatching { val file = item.localPath?.let(::File)?.takeIf(File::isFile) ?: error("The downloaded file is no longer available. Download it again."); val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file); val mime = if (item.isApkDownload()) "application/vnd.android.package-archive" else "application/octet-stream"; context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share ${item.fileName}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
private fun viewRelease(context: Context, item: DownloadEntity): Result<Unit> = runCatching { val url = item.releaseUrl?.trim()?.takeIf { it.startsWith("https://github.com/", ignoreCase = true) } ?: error("The GitHub release link is not available for this download."); context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
