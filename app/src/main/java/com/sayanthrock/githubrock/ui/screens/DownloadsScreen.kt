package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.DownloadMirror
import com.sayanthrock.githubrock.core.model.ManualDownloadRequest
import com.sayanthrock.githubrock.core.model.ManualDownloadType
import com.sayanthrock.githubrock.core.model.validateManualDownload
import com.sayanthrock.githubrock.core.util.ApkInspection
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
    val actionTarget = actionTargetId?.let { id -> downloads.firstOrNull { it.id == id } }

    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionFile by remember { mutableStateOf<File?>(null) }
    var inspectionLoading by remember { mutableStateOf(false) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var inspectionRequestKey by remember { mutableStateOf(0L) }

    var deleteTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var cancelTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var showAddDownload by rememberSaveable { mutableStateOf(false) }
    var showMirrors by rememberSaveable { mutableStateOf(false) }

    val cancelInspection: () -> Unit = {
        inspectionRequestKey += 1
        inspectionLoading = false
        inspectionFile = null
    }

    LaunchedEffect(actionTargetId, actionTarget) {
        if (actionTargetId != null && actionTarget == null) actionTargetId = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Downloads",
                subtitle = "Transfers, completed files, and APK safety"
            )
        }
        item {
            DownloadCommandBar(
                selectedMirror = selectedMirror,
                onChangeMirror = { showMirrors = true },
                onAddDownload = { showAddDownload = true }
            )
        }

        if (downloads.isEmpty()) {
            item { EmptyDownloadsCard(onAddDownload = { showAddDownload = true }) }
        }

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

    if (showAddDownload) {
        ManualDownloadDialog(
            selectedMirror = selectedMirror,
            onDismiss = { showAddDownload = false },
            onAdd = { request ->
                viewModel.enqueue(request.url, request.fileName)
                showAddDownload = false
            }
        )
    }

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
                onCancel = {
                    actionTargetId = null
                    cancelTarget = item
                },
                onRetry = {
                    viewModel.retry(item)
                    actionTargetId = null
                },
                onInspect = {
                    val file = item.localPath?.let(::File)?.takeIf(File::exists)
                    actionTargetId = null
                    if (file == null) {
                        inspectionError = "The downloaded APK file is no longer available."
                    } else {
                        val requestKey = inspectionRequestKey + 1
                        inspectionRequestKey = requestKey
                        inspectionFile = file
                        inspectionLoading = true
                        inspectionError = null
                        viewModel.inspectApk(file) { result ->
                            if (inspectionRequestKey == requestKey) {
                                inspectionLoading = false
                                inspection = result.getOrNull()
                                inspectionError = result.exceptionOrNull()?.message
                                    ?.takeIf(String::isNotBlank)
                                    ?: if (result.isFailure) "Unable to inspect this APK." else null
                                if (inspection == null) inspectionFile = null
                            }
                        }
                    }
                },
                onShare = {
                    shareDownload(context, item)
                    actionTargetId = null
                },
                onDelete = {
                    actionTargetId = null
                    deleteTarget = item
                }
            )
        }
    }

    if (inspectionLoading) {
        ModalBottomSheet(
            onDismissRequest = cancelInspection,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            dragHandle = null
        ) {
            ApkInspectionLoadingSheet(onCancel = cancelInspection)
        }
    }

    inspection?.let { apk ->
        ModalBottomSheet(
            onDismissRequest = {
                inspection = null
                inspectionFile = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ApkInspectionSheet(
                apk = apk,
                installEnabled = inspectionFile?.exists() == true,
                onInstall = {
                    inspectionFile?.takeIf(File::exists)?.let { file ->
                        installApk(context, file).onFailure { problem ->
                            inspectionError = problem.message?.takeIf(String::isNotBlank)
                                ?: "Android could not open the package installer. Allow installs from this source and try again."
                        }
                    }
                },
                onClose = {
                    inspection = null
                    inspectionFile = null
                }
            )
        }
    }

    inspectionError?.let { message ->
        AlertDialog(
            onDismissRequest = { inspectionError = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("APK action unavailable") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { inspectionError = null }) { Text("Close") }
            }
        )
    }

    cancelTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel download?") },
            text = { Text("The partial file will be removed. The item remains in history and can be restarted.") },
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

@Composable
internal fun DownloadCommandBar(
    selectedMirror: DownloadMirror,
    onChangeMirror: () -> Unit,
    onAddDownload: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Download source",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        selectedMirror.label,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Button(
            onClick = onAddDownload,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add download", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyDownloadsCard(onAddDownload: () -> Unit) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
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
                "Add a trusted GitHub image, release asset, APK, or file.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onAddDownload) { Text("Add image or file") }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onOpenActions: () -> Unit
) {
    val controls = downloadControls(item.status)
    val progressLevel = downloadProgressLevel(item.downloadedBytes, item.totalBytes, item.status)
    val progress = progressLevel / 100f
    val accent = downloadStatusColor(item.status)

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(downloadStatusIcon(item.status), contentDescription = null, tint = accent)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${downloadTypeLabel(item.fileName)} · ${downloadFormatLabel(item.fileName)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                DownloadStatusPill(item.status, accent)
            }

            if (item.downloadedBytes > 0 || item.status == "completed") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (item.totalBytes > 0) {
                            "${formatBytes(item.downloadedBytes)} of ${formatBytes(item.totalBytes)}"
                        } else {
                            formatBytes(item.downloadedBytes)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$progressLevel%",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = accent,
                trackColor = accent.copy(alpha = .14f)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    DownloadControl.Pause in controls -> {
                        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Pause")
                        }
                    }
                    DownloadControl.Resume in controls -> {
                        Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Resume")
                        }
                    }
                    DownloadControl.Retry in controls -> {
                        Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (item.status == "cancelled") "Restart" else "Retry")
                        }
                    }
                    else -> Spacer(Modifier.weight(1f))
                }
                OutlinedButton(onClick = onOpenActions, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Actions")
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
                    "Added ${formatDownloadTime(item.createdAt)}",
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
private fun DownloadStatusPill(status: String, accent: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = .12f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun DownloadActionsSheet(
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
    val hasLocalFile = item.localPath != null
    val isApk = item.localPath?.endsWith(".apk", ignoreCase = true) == true
    val accent = downloadStatusColor(item.status)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = accent.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(downloadStatusIcon(item.status), contentDescription = null, tint = accent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${downloadTypeLabel(item.fileName)} · ${downloadFormatLabel(item.fileName)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (DownloadControl.Pause in controls) {
            DownloadSheetAction(Icons.Default.Pause, "Pause download", "Keep the partial file and resume later", onPause)
        }
        if (DownloadControl.Resume in controls) {
            DownloadSheetAction(Icons.Default.PlayArrow, "Resume download", "Continue from the saved progress", onResume)
        }
        if (DownloadControl.Retry in controls) {
            DownloadSheetAction(
                Icons.Default.Refresh,
                if (item.status == "cancelled") "Restart download" else "Retry download",
                "Start this transfer again",
                onRetry
            )
        }
        if (DownloadControl.Cancel in controls) {
            DownloadSheetAction(
                Icons.Default.Cancel,
                "Cancel download",
                "Remove the current partial file",
                onCancel,
                destructive = true
            )
        }
        if (item.status == "completed" && isApk) {
            DownloadSheetAction(
                Icons.Default.Security,
                "Inspect APK",
                "Review package, SDK, signature, hash, and permissions",
                onInspect
            )
        }
        if (item.status == "completed" && hasLocalFile) {
            DownloadSheetAction(
                Icons.Default.Share,
                "Share file",
                "Send the completed file using Android share",
                onShare
            )
        }
        if (!isActive && item.status != "paused") {
            DownloadSheetAction(
                Icons.Default.Delete,
                "Delete history and file",
                "Permanently remove the local file and record",
                onDelete,
                destructive = true
            )
        }
    }
}

@Composable
private fun DownloadSheetAction(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = tint.copy(alpha = .08f),
        border = BorderStroke(1.dp, tint.copy(alpha = .22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = tint, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun ApkInspectionLoadingSheet(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator()
        Text("Inspecting APK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Reading the package, SDK requirements, signing certificate, file hash, and permissions.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun ApkInspectionSheet(
    apk: ApkInspection,
    installEnabled: Boolean,
    onInstall: () -> Unit,
    onClose: () -> Unit
) {
    val signatureState = when (apk.installedSignatureMatches) {
        true -> "Matches installed app"
        false -> "Does not match installed app"
        null -> "Not installed"
    }
    val signatureAccent = when (apk.installedSignatureMatches) {
        true -> MaterialTheme.colorScheme.tertiary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    apk.appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    apk.packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InspectionMetric("Version", "${apk.versionName} (${apk.versionCode})", Modifier.weight(1f))
            InspectionMetric("File size", formatBytes(apk.fileSize), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InspectionMetric("Minimum SDK", apk.minSdk.toString(), Modifier.weight(1f))
            InspectionMetric("Target SDK", apk.targetSdk.toString(), Modifier.weight(1f))
        }

        InspectionSectionTitle(Icons.Default.VerifiedUser, "Signing identity")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = signatureAccent.copy(alpha = .08f),
            border = BorderStroke(1.dp, signatureAccent.copy(alpha = .24f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (apk.installedSignatureMatches == false) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = signatureAccent
                    )
                    Text(signatureState, color = signatureAccent, fontWeight = FontWeight.Bold)
                }
                SelectableHash(label = "Signing SHA-256", value = apk.signingSha256 ?: "Unavailable")
            }
        }

        InspectionSectionTitle(Icons.Default.Fingerprint, "File integrity")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .50f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                SelectableHash(label = "File SHA-256", value = apk.fileSha256)
            }
        }

        InspectionSectionTitle(Icons.Default.AdminPanelSettings, "Requested permissions")
        Text(
            "${apk.permissions.size} permission${if (apk.permissions.size == 1) "" else "s"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (apk.permissions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = .08f)
            ) {
                Text(
                    "No requested permissions",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            apk.permissions.forEach { permission ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(permission, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Close")
            }
            Button(onClick = onInstall, enabled = installEnabled, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Default.InstallMobile, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Install")
            }
        }
    }
}

@Composable
private fun InspectionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InspectionSectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectableHash(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DownloadMirrorDialog(
    selected: DownloadMirror,
    onSelect: (DownloadMirror) -> Unit,
    onDismiss: () -> Unit
) {
    var pending by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download source") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Select the endpoint used for new downloads. Direct GitHub is the official and safest default.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MirrorSection("Official", listOf(DownloadMirror.Direct), pending) { pending = it }
                MirrorSection("Community", DownloadMirror.entries.filter { it.community }, pending) { pending = it }
                Text(
                    "Community mirrors are third-party services. Existing downloads keep their original endpoint.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSelect(pending)
                onDismiss()
            }) { Text("Use selected") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MirrorSection(
    title: String,
    mirrors: List<DownloadMirror>,
    selected: DownloadMirror,
    onSelect: (DownloadMirror) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                mirrors.forEachIndexed { index, mirror ->
                    Surface(onClick = { onSelect(mirror) }, color = Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(mirror.label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (mirror == DownloadMirror.Direct) {
                                        "Official GitHub endpoint"
                                    } else {
                                        "Third-party community endpoint"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(selected = selected == mirror, onClick = { onSelect(mirror) })
                        }
                    }
                    if (index < mirrors.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 14.dp))
                }
            }
        }
    }
}

@Composable
private fun ManualDownloadDialog(
    selectedMirror: DownloadMirror,
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
                    Text(
                        "Source: ${selectedMirror.label}",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ManualDownloadType.Image,
                        onClick = {
                            type = ManualDownloadType.Image
                            error = null
                        },
                        label = { Text("Image") },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == ManualDownloadType.File,
                        onClick = {
                            type = ManualDownloadType.File
                            error = null
                        },
                        label = { Text("File") },
                        leadingIcon = {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("GitHub HTTPS download link") },
                    placeholder = { Text("https://github.com/…") },
                    singleLine = true,
                    isError = error != null
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it.take(120)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Save as filename") },
                    placeholder = { Text(if (type == ManualDownloadType.Image) "image.png" else "release.apk") },
                    singleLine = true,
                    supportingText = { Text("Optional — a safe name is created from the link.") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    "Only trusted GitHub HTTPS source links are accepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

private enum class DownloadControl { Pause, Resume, Retry, Cancel }

private fun downloadControls(status: String): Set<DownloadControl> = when (status) {
    "queued", "downloading", "retrying" -> setOf(DownloadControl.Pause, DownloadControl.Cancel)
    "paused" -> setOf(DownloadControl.Resume, DownloadControl.Cancel)
    "failed", "cancelled" -> setOf(DownloadControl.Retry)
    else -> emptySet()
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
    else -> (downloadedBytes.coerceAtLeast(0) * 100 / totalBytes).toInt().coerceIn(0, 100)
}

private fun isImageDownload(fileName: String): Boolean =
    fileName.substringAfterLast('.', "").lowercase(Locale.US) in setOf("png", "jpg", "jpeg", "webp", "gif")

private fun downloadTypeLabel(fileName: String): String = if (isImageDownload(fileName)) "Image" else "File"

private fun downloadFormatLabel(fileName: String): String =
    fileName.substringAfterLast('.', "file").ifBlank { "file" }.uppercase(Locale.US)

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

private fun shareDownload(context: android.content.Context, item: DownloadEntity) {
    val file = item.localPath?.let(::File) ?: return
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = downloadMimeType(item.fileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }, "Share ${item.fileName}"))
}

private fun installApk(context: android.content.Context, file: File): Result<Unit> = runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    })
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

private fun formatDownloadTime(epochMillis: Long): String =
    runCatching { downloadTimeFormatter.format(Instant.ofEpochMilli(epochMillis)) }
        .getOrDefault("Time unavailable")
