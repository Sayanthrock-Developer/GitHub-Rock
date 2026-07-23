package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.DownloadMirror
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.ui.components.GlassCard
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

private data class DownloadArtwork(
    val icon: Drawable? = null,
    val appLabel: String? = null,
    val packageName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsRedesignScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val selectedMirror by viewModel.selectedMirror.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFilterName by rememberSaveable { mutableStateOf(DownloadListFilter.All.name) }
    val selectedFilter = DownloadListFilter.entries.firstOrNull { it.name == selectedFilterName }
        ?: DownloadListFilter.All
    val visibleDownloads = remember(downloads, selectedFilter) {
        filterDownloads(downloads, selectedFilter)
    }
    val applicationDownloads = remember(downloads) {
        downloads.filter { it.isApkDownload() }.take(10)
    }

    var actionTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val actionTarget = actionTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteTarget = deleteTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    var cancelTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val cancelTarget = cancelTargetId?.let { id -> downloads.firstOrNull { it.id == id } }
    var showMirrorSheet by rememberSaveable { mutableStateOf(false) }
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DownloadsHero(downloads)
        }

        if (applicationDownloads.isNotEmpty()) {
            item {
                RecentApplicationsRail(
                    downloads = applicationDownloads,
                    onOpen = { actionTargetId = it.id }
                )
            }
        }

        item {
            RedesignedDownloadSourceCard(
                selectedMirror = selectedMirror,
                onChangeMirror = { showMirrorSheet = true }
            )
        }

        item {
            DownloadFilterRow(
                selected = selectedFilter,
                onSelect = { selectedFilterName = it.name }
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
            item {
                RedesignedEmptyDownloadsCard(selectedFilter)
            }
        }

        items(visibleDownloads, key = { it.id }) { item ->
            RedesignedDownloadCard(
                item = item,
                onPrimaryAction = {
                    when (item.status) {
                        "downloading", "queued", "retrying" -> viewModel.pause(item)
                        "paused" -> viewModel.resume(item)
                        "failed", "cancelled" -> viewModel.retry(item)
                        "completed" -> {
                            if (item.isApkDownload()) {
                                openDownloadedApk(context, item).onFailure {
                                    errorMessage = it.message ?: "Android could not open this APK."
                                }
                            } else {
                                shareDownloadedFile(context, item).onFailure {
                                    errorMessage = it.message ?: "Android could not share this file."
                                }
                            }
                        }
                    }
                },
                onOpenActions = { actionTargetId = item.id }
            )
        }
    }

    if (showMirrorSheet) {
        DownloadMirrorSheet(
            selected = selectedMirror,
            onSelect = {
                viewModel.selectMirror(it)
                showMirrorSheet = false
            },
            onDismiss = { showMirrorSheet = false }
        )
    }

    actionTarget?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { actionTargetId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            RedesignedDownloadActionsSheet(
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
            text = { Text("Reading the application name, package, SDK, signature, hash, and permissions.") },
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
                TextButton(onClick = {
                    viewModel.cancel(item)
                    cancelTargetId = null
                }) { Text("Cancel download") }
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
                TextButton(onClick = {
                    viewModel.delete(item)
                    deleteTargetId = null
                }) { Text("Delete") }
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
private fun DownloadsHero(downloads: List<DownloadEntity>) {
    val activeCount = downloads.count { it.status in setOf("queued", "downloading", "retrying", "paused") }
    val completedCount = downloads.count { it.status == "completed" }
    val applicationCount = downloads.count { it.isApkDownload() }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Applications, build artifacts, images, and files in one clean workspace",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadSummaryMetric("Active", activeCount, Modifier.weight(1f))
                DownloadSummaryMetric("Completed", completedCount, Modifier.weight(1f))
                DownloadSummaryMetric("Apps", applicationCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DownloadSummaryMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value.toString(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RecentApplicationsRail(
    downloads: List<DownloadEntity>,
    onOpen: (DownloadEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Recent applications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(downloads, key = { it.id }) { item ->
                RecentApplicationTile(item = item, onClick = { onOpen(item) })
            }
        }
    }
}

@Composable
private fun RecentApplicationTile(item: DownloadEntity, onClick: () -> Unit) {
    val artwork = rememberDownloadArtwork(item)
    val name = preferredApplicationName(item.fileName, artwork.appLabel)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 196.dp, height = 92.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DownloadArtworkView(item = item, artwork = artwork, size = 60.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    downloadStatusLabel(item.status),
                    color = downloadStatusColor(item.status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                artwork.packageName?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RedesignedDownloadSourceCard(
    selectedMirror: DownloadMirror,
    onChangeMirror: () -> Unit
) {
    Surface(
        onClick = onChangeMirror,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Download source", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedMirror.label, fontWeight = FontWeight.Bold)
            }
            AssistChip(
                onClick = onChangeMirror,
                label = { Text("Change") },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp)) }
            )
        }
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
private fun RedesignedDownloadCard(
    item: DownloadEntity,
    onPrimaryAction: () -> Unit,
    onOpenActions: () -> Unit
) {
    val artwork = rememberDownloadArtwork(item)
    val appName = preferredApplicationName(item.fileName, artwork.appLabel)
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
                DownloadArtworkView(item = item, artwork = artwork, size = 64.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (item.isApkDownload()) appName else item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (item.isApkDownload() && appName != item.fileName.substringBeforeLast('.')) {
                            item.fileName
                        } else {
                            "${downloadTypeLabel(item.fileName)} · ${item.fileName.substringAfterLast('.', "FILE").uppercase()}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DownloadStatusBadge(item.status, accent)
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
                    Text("$progress%", color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Added ${formatDownloadTimestamp(item.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DownloadArtworkView(
    item: DownloadEntity,
    artwork: DownloadArtwork,
    size: androidx.compose.ui.unit.Dp
) {
    val remoteArtwork = remember(item.sourceUrl) { githubOwnerArtwork(item.sourceUrl) }
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when {
            artwork.icon != null -> AsyncImage(
                model = artwork.icon,
                contentDescription = "${item.fileName} application icon",
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop
            )
            item.isApkDownload() && remoteArtwork != null -> AsyncImage(
                model = remoteArtwork,
                contentDescription = "${item.fileName} source artwork",
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop
            )
            else -> Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (item.isApkDownload()) Icons.Default.Android else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(size * .48f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun rememberDownloadArtwork(item: DownloadEntity): DownloadArtwork {
    val context = LocalContext.current
    val metadata by produceState(
        initialValue = DownloadArtwork(),
        key1 = item.localPath,
        key2 = item.status
    ) {
        value = withContext(Dispatchers.IO) {
            loadApkArtwork(context, item)
        }
    }
    return metadata
}

@Suppress("DEPRECATION")
private fun loadApkArtwork(context: Context, item: DownloadEntity): DownloadArtwork {
    if (!item.isApkDownload()) return DownloadArtwork()
    val apk = item.localPath?.let(::File)?.takeIf(File::exists) ?: return DownloadArtwork()
    return runCatching {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: return@runCatching DownloadArtwork()
        val applicationInfo = packageInfo.applicationInfo ?: return@runCatching DownloadArtwork()
        applicationInfo.sourceDir = apk.absolutePath
        applicationInfo.publicSourceDir = apk.absolutePath
        DownloadArtwork(
            icon = applicationInfo.loadIcon(packageManager),
            appLabel = applicationInfo.loadLabel(packageManager)?.toString(),
            packageName = packageInfo.packageName
        )
    }.getOrDefault(DownloadArtwork())
}

@Composable
private fun DownloadStatusBadge(status: String, accent: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Text(
            downloadStatusLabel(status),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun RedesignedEmptyDownloadsCard(filter: DownloadListFilter) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(66.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (filter == DownloadListFilter.Applications) Icons.Default.Android else Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(31.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                if (filter == DownloadListFilter.All) "No downloads yet" else "No ${filter.label.lowercase()} downloads",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Repository files, release assets, APKs, and build artifacts will appear here automatically.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RedesignedDownloadActionsSheet(
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
    val artwork = rememberDownloadArtwork(item)
    val active = item.status in setOf("queued", "downloading", "retrying")
    val localFileExists = item.localPath?.let(::File)?.exists() == true

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DownloadArtworkView(item = item, artwork = artwork, size = 58.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    preferredApplicationName(item.fileName, artwork.appLabel),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(item.fileName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (active) DownloadActionRow(Icons.Default.Pause, "Pause download", "Keep the partial file and continue later", onPause)
        if (item.status == "paused") DownloadActionRow(Icons.Default.PlayArrow, "Resume download", "Continue from the saved progress", onResume)
        if (item.status in setOf("failed", "cancelled")) DownloadActionRow(Icons.Default.Refresh, "Retry download", "Start this transfer again", onRetry)
        if (active || item.status == "paused") DownloadActionRow(Icons.Default.Cancel, "Cancel download", "Remove the current partial file", onCancel, destructive = true)
        if (item.status == "completed" && item.isApkDownload() && localFileExists) {
            DownloadActionRow(Icons.Default.InstallMobile, "Install application", "Open Android's secure package installer", onInstall)
            DownloadActionRow(Icons.Default.Security, "Inspect APK", "Review app identity, SDK, signature, hash, and permissions", onInspect)
        }
        if (item.status == "completed" && localFileExists) {
            DownloadActionRow(Icons.Default.Share, "Share file", "Send the completed file using Android share", onShare)
        }
        if (!active && item.status != "paused") {
            DownloadActionRow(Icons.Default.Delete, "Delete file and history", "Permanently remove this local download", onDelete, destructive = true)
        }
    }
}

@Composable
private fun DownloadActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = tint, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadMirrorSheet(
    selected: DownloadMirror,
    onSelect: (DownloadMirror) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Download source", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "Direct GitHub is recommended. Community mirrors receive the selected public GitHub URL.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DownloadMirror.entries.forEach { mirror ->
                Surface(
                    onClick = { onSelect(mirror) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (mirror == selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, if (mirror == selected) MaterialTheme.colorScheme.primary.copy(alpha = .28f) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = mirror == selected, onClick = { onSelect(mirror) })
                        Column(Modifier.weight(1f)) {
                            Text(mirror.label, fontWeight = FontWeight.Bold)
                            Text(mirror.hostLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkSummaryDialog(apk: ApkInspection, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(apk.appName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ApkSummaryRow("Package", apk.packageName)
                ApkSummaryRow("Version", "${apk.versionName} (${apk.versionCode})")
                ApkSummaryRow("SDK", "API ${apk.minSdk}–${apk.targetSdk}")
                ApkSummaryRow("Size", formatDownloadBytes(apk.fileSize))
                ApkSummaryRow("Permissions", apk.permissions.size.toString())
                ApkSummaryRow("Installed signature", when (apk.installedSignatureMatches) {
                    true -> "Matches"
                    false -> "Different"
                    null -> "Not installed"
                })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ApkSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
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

private fun primaryActionIcon(item: DownloadEntity) = when (item.status) {
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

private fun downloadTypeLabel(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
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

private fun githubOwnerArtwork(sourceUrl: String): String? {
    val directOwner = Regex("https://github\\.com/([^/]+)", RegexOption.IGNORE_CASE)
        .find(Uri.decode(sourceUrl))
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf(String::isNotBlank)
    if (directOwner != null) return "https://github.com/$directOwner.png?size=160"

    val uri = runCatching { Uri.parse(sourceUrl) }.getOrNull() ?: return null
    val segments = uri.pathSegments.orEmpty()
    val owner = when {
        uri.host.equals("github.com", ignoreCase = true) -> segments.firstOrNull()
        uri.host.equals("api.github.com", ignoreCase = true) -> {
            val reposIndex = segments.indexOf("repos")
            segments.getOrNull(reposIndex + 1)
        }
        else -> null
    }?.takeIf { it.isNotBlank() && it != "repos" }
    return owner?.let { "https://github.com/$it.png?size=160" }
}

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
    val mime = if (item.isApkDownload()) "application/vnd.android.package-archive" else "application/octet-stream"
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
