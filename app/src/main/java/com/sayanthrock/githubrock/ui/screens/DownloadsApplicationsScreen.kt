package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.MainActivity
import com.sayanthrock.githubrock.core.util.InstalledApkState
import com.sayanthrock.githubrock.core.util.InstalledApkStateResolver
import com.sayanthrock.githubrock.data.local.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private enum class ApplicationFilter(val label: String) {
    All("All"), Apps("Apps"), Files("Files"), Active("Downloading"), Installed("Installed")
}

@Composable
fun DownloadsApplicationsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableStateOf(0) }
    var filterName by rememberSaveable { mutableStateOf(ApplicationFilter.All.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedApk by remember { mutableStateOf<DownloadEntity?>(null) }
    var appStates by remember { mutableStateOf<Map<Long, InstalledApkState>>(emptyMap()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(downloads, refreshKey) {
        val apkItems = downloads.filter { it.isApkDownload() && it.status == "completed" && it.localPath != null }
        appStates = withContext(Dispatchers.IO) {
            apkItems.mapNotNull { item ->
                val file = item.localPath?.let(::File) ?: return@mapNotNull null
                InstalledApkStateResolver.resolve(context, file)?.let { item.id to it }
            }.toMap()
        }
    }

    val selectedFilter = ApplicationFilter.entries.firstOrNull { it.name == filterName } ?: ApplicationFilter.All
    val visible = downloads.filter { item ->
        when (selectedFilter) {
            ApplicationFilter.All -> true
            ApplicationFilter.Apps -> item.isApkDownload()
            ApplicationFilter.Files -> !item.isApkDownload()
            ApplicationFilter.Active -> item.status in setOf("queued", "downloading", "retrying", "paused")
            ApplicationFilter.Installed -> appStates[item.id]?.installed == true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Downloads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Tap an application name to open its repository inside GitHub Rock.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ApplicationFilter.entries.toList(), key = { it.name }) { option ->
                    FilterChip(selected = option == selectedFilter, onClick = { filterName = option.name }, label = { Text(option.label) })
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text("Nothing here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(visible, key = { it.id }) { item ->
            val state = appStates[item.id]
            if (item.isApkDownload()) {
                ApplicationDownloadCard(
                    item = item,
                    state = state,
                    onOpenRepository = { owner, repo -> openNativeRepository(context, owner, repo) },
                    onPrimary = {
                        if (item.status != "completed") {
                            when (item.status) {
                                "paused" -> viewModel.resume(item)
                                "failed", "cancelled" -> viewModel.retry(item)
                                else -> viewModel.pause(item)
                            }
                            return@ApplicationDownloadCard
                        }
                        val file = item.localPath?.let(::File)
                        if (file == null || !file.isFile || file.length() <= 0L) {
                            viewModel.downloadAgain(item)
                            return@ApplicationDownloadCard
                        }
                        val inspection = runCatching { InstalledApkStateResolver.resolve(context, file) }.getOrNull()
                        if (inspection == null) {
                            errorMessage = "The downloaded APK could not be read. Download it again."
                            return@ApplicationDownloadCard
                        }
                        if (state?.installed == true && state.isSameOrOlderVersion) {
                            if (!InstalledApkStateResolver.launchInstalledApp(context, state)) errorMessage = "Android could not open the installed application."
                        } else {
                            openApkInstaller(context, file).onFailure { errorMessage = it.message ?: "Android could not open the package installer." }
                        }
                    },
                    onPause = { viewModel.pause(item) },
                    onResume = { viewModel.resume(item) },
                    onRetry = { viewModel.retry(item) },
                    onInspect = { selectedApk = item },
                    onDelete = { viewModel.delete(item) }
                )
            } else {
                FileDownloadCard(
                    item = item,
                    onPrimary = {
                        when {
                            item.status in setOf("downloading", "queued", "retrying") -> viewModel.pause(item)
                            item.status == "paused" -> viewModel.resume(item)
                            item.status in setOf("failed", "cancelled") -> viewModel.retry(item)
                            else -> shareDownload(context, item).onFailure { errorMessage = it.message ?: "Android could not share this file." }
                        }
                    },
                    onDelete = { viewModel.delete(item) }
                )
            }
        }
    }

    selectedApk?.let { item ->
        val file = item.localPath?.let(::File)
        val state = appStates[item.id]
        AlertDialog(
            onDismissRequest = { selectedApk = null },
            title = { Text(state?.label ?: item.fileName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Package: ${state?.packageName ?: "Unavailable"}")
                    Text("Version: ${state?.downloadedVersionName ?: "Unavailable"}")
                    Text("Size: ${formatDownloadBytes(item.downloadedBytes.takeIf { it > 0 } ?: file?.length() ?: 0)}")
                    Text(if (state?.installed == true) "Installed" else "Not installed")
                }
            },
            confirmButton = { TextButton(onClick = { selectedApk = null }) { Text("Close") } }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("Action unavailable") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun ApplicationDownloadCard(
    item: DownloadEntity,
    state: InstalledApkState?,
    onOpenRepository: (String, String) -> Unit,
    onPrimary: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryLabel = when {
        item.status != "completed" -> when (item.status) { "paused" -> "Resume"; "failed", "cancelled" -> "Retry"; else -> "Pause" }
        state?.installed == true && state.isUpdateAvailable -> "Update"
        state?.installed == true -> "Open"
        item.localPath?.let(::File)?.isFile != true -> "Download again"
        else -> "Install"
    }
    val repository = remember(item.sourceUrl) { repositoryFromDownloadUrl(item.sourceUrl) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(state?.icon)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = state?.label ?: item.fileName.substringBeforeLast('.'),
                        modifier = Modifier.then(if (repository != null) Modifier.clickable { onOpenRepository(repository.first, repository.second) } else Modifier),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (repository != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(state?.packageName ?: "APK application", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${state?.downloadedVersionName?.let { "v$it" } ?: "Version unavailable"} · ${formatDownloadBytes(item.totalBytes.takeIf { it > 0 } ?: item.downloadedBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (repository != null) Text("${repository.first}/${repository.second}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onInspect) { Icon(Icons.Default.MoreHoriz, contentDescription = "Application details") }
            }
            if (item.status != "completed") LinearProgressIndicator(progress = { downloadProgressPercent(item) / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
            Text(
                when {
                    item.status != "completed" -> item.status.replaceFirstChar { it.uppercase() }
                    item.localPath?.let(::File)?.isFile != true -> "APK unavailable — download again"
                    state?.installed == true && state.isUpdateAvailable -> "Update available"
                    state?.installed == true -> "Installed"
                    else -> "Ready to install"
                },
                color = if (state?.installed == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                    Icon(when (primaryLabel) { "Open" -> Icons.Default.PlayArrow; "Install", "Update", "Download again" -> Icons.Default.Android; "Resume" -> Icons.Default.PlayArrow; "Retry" -> Icons.Default.Refresh; else -> Icons.Default.Pause }, contentDescription = null)
                    Spacer(Modifier.width(6.dp)); Text(primaryLabel)
                }
                OutlinedButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete download") }
            }
        }
    }
}

@Composable
private fun FileDownloadCard(item: DownloadEntity, onPrimary: () -> Unit, onDelete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(50.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.InsertDriveFile, contentDescription = null) } }
            Column(Modifier.weight(1f)) {
                Text(item.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.status.replaceFirstChar { it.uppercase() }} · ${formatDownloadBytes(item.totalBytes.takeIf { it > 0 } ?: item.downloadedBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrimary) { Icon(when (item.status) { "paused" -> Icons.Default.PlayArrow; "failed", "cancelled" -> Icons.Default.Refresh; "completed" -> Icons.Default.Share; else -> Icons.Default.Pause }, contentDescription = "File action") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete file") }
        }
    }
}

@Composable
private fun AppIcon(icon: Drawable?) {
    Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        if (icon == null) Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, contentDescription = "Application", modifier = Modifier.size(32.dp)) }
        else Image(bitmap = remember(icon) { drawableToBitmap(icon).asImageBitmap() }, contentDescription = "Application icon", modifier = Modifier.padding(7.dp).fillMaxSize())
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
    }
}

private fun repositoryFromDownloadUrl(url: String): Pair<String, String>? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val parts = uri.pathSegments
    if (host == "github.com" && parts.size >= 5 && parts[2].equals("releases", true) && parts[3].equals("download", true)) {
        return parts[0].takeIf(String::isNotBlank)?.let { owner -> parts[1].takeIf(String::isNotBlank)?.let { repo -> owner to repo } }
    }
    if (host == "api.github.com" && parts.size >= 6 && parts[0] == "repos" && parts[3].equals("releases", true) && parts[4].equals("assets", true)) {
        return parts[1].takeIf(String::isNotBlank)?.let { owner -> parts[2].takeIf(String::isNotBlank)?.let { repo -> owner to repo } }
    }
    return null
}

private fun openNativeRepository(context: Context, owner: String, repo: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        data = Uri.parse("githubrock://repo/${Uri.encode(owner)}/${Uri.encode(repo)}")
    }
    context.startActivity(intent)
}

private fun openApkInstaller(context: Context, file: File): Result<Unit> = runCatching {
    require(file.isFile && file.length() > 0L) { "The downloaded APK file is no longer available. Download it again." }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        error("Allow GitHub Rock to install unknown apps, then try again.")
    }
    val archive = InstalledApkStateResolver.resolve(context, file)
        ?: error("The downloaded file is not a readable APK. Download it again.")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    require(intent.resolveActivity(context.packageManager) != null) { "No Android package installer is available on this device." }
    context.grantUriPermission(
        intent.resolveActivity(context.packageManager)?.packageName,
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    )
    archive.packageName.isNotBlank()
    context.startActivity(intent)
}

private fun shareDownload(context: Context, item: DownloadEntity): Result<Unit> = runCatching {
    val file = requireNotNull(item.localPath?.let(::File)) { "The downloaded file is no longer available." }
    require(file.exists()) { "The downloaded file is no longer available." }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }, "Share ${item.fileName}"))
}

private fun downloadProgressPercent(item: DownloadEntity): Int = when {
    item.status == "completed" -> 100
    item.totalBytes <= 0 -> 0
    else -> (item.downloadedBytes.coerceAtLeast(0) * 100 / item.totalBytes).toInt().coerceIn(0, 100)
}

private fun formatDownloadBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
