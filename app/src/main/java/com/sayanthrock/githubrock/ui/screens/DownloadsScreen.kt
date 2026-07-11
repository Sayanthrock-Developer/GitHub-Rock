package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ApkInspector
import java.io.File

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectionFile by remember { mutableStateOf<File?>(null) }
    Column(
        Modifier.fillMaxSize().padding(18.dp).padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Verified artifact pipeline", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Artifacts downloaded from a repository are recovered by WorkManager, hashed with SHA-256 and inspected before Android's system installer opens.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (downloads.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Download, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                Text("Download an Actions artifact or release APK to see it here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        downloads.forEach { item ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.fileName, style = MaterialTheme.typography.titleMedium)
                    Text(item.status.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.totalBytes > 0) LinearProgressIndicator(item.downloadedBytes.toFloat() / item.totalBytes.toFloat(), Modifier.fillMaxWidth())
                    if (item.status == "completed" && item.localPath?.endsWith(".apk", true) == true) {
                        TextButton(onClick = {
                            inspectionFile = item.localPath?.let(::File)
                            inspection = inspectionFile?.let { ApkInspector.inspect(context, it) }
                        }) { Text("Inspect APK") }
                    }
                    if (item.status == "failed") {
                        TextButton(onClick = { viewModel.retry(item) }) { Text("Retry") }
                    }
                    TextButton(onClick = { viewModel.delete(item.id) }) { Text("Delete history") }
                }
            }
        }
    }
    inspection?.let { apk ->
        AlertDialog(
            onDismissRequest = { inspection = null },
            title = { Text(apk.appName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${apk.packageName} • ${apk.versionName} (${apk.versionCode})")
                    Text("${apk.fileSize / 1_048_576} MB • min SDK ${apk.minSdk} • target SDK ${apk.targetSdk}")
                    Text("SHA-256: ${apk.fileSha256}", style = MaterialTheme.typography.bodySmall)
                    Text("Signing: ${apk.signingSha256 ?: "Unavailable"}", style = MaterialTheme.typography.bodySmall)
                    Text("Installed signing match: ${apk.installedSignatureMatches?.toString() ?: "Not installed"}")
                    Text("Permissions: ${apk.permissions.size}")
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
}
