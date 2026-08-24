package com.sayanthrock.githubrock.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ApkInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ApkInspectorScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var inspecting by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        inspecting = true
        error = null
        inspection = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File.createTempFile("github-rock-apk-", ".apk", context.cacheDir)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected APK.")
                    val inspected = ApkInspector.inspect(context, file)
                        ?: error("The selected file is not a readable Android APK.")
                    file.deleteOnExit()
                    inspected
                }
            }
            inspecting = false
            result.onSuccess {
                selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected APK"
                inspection = it
            }.onFailure {
                error = it.message?.takeIf(String::isNotBlank) ?: "Unable to inspect the APK."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("APK Inspector", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "Inspect an APK before installing it: package identity, SDK levels, permissions, signatures, and file integrity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(selectedFileName ?: "No APK selected", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Choose any local .apk file", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { picker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) }, enabled = !inspecting) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    }
                }
            }
        }

        if (inspecting) {
            item { Text("Inspecting APK…", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        }

        inspection?.let { apk ->
            item { InspectionSummaryCard(apk) }
            item { InspectionSecurityCard(apk) }
            item { Text("Requested permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (apk.permissions.isEmpty()) {
                item { Text("No requested permissions", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(apk.permissions, key = { it }) { permission ->
                    PermissionRow(permission)
                }
            }
        }

        error?.let { message ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null)
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = {
                inspection = null
                error = null
                selectedFileName = null
            }) {
                Text("Clear inspection")
            }
        }
    }
}

@Composable
private fun InspectionSummaryCard(apk: ApkInspection) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(apk.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            InspectionRow("Package", apk.packageName)
            InspectionRow("Version", "${apk.versionName.ifBlank { "Unknown" }} (${apk.versionCode})")
            InspectionRow("File size", formatDownloadBytes(apk.fileSize))
            InspectionRow("Min SDK", apk.minSdk.toString())
            InspectionRow("Target SDK", apk.targetSdk.toString())
        }
    }
}

@Composable
private fun InspectionSecurityCard(apk: ApkInspection) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null)
                Text("Security & integrity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            InspectionRow("Signing SHA-256", apk.signingSha256 ?: "Unavailable")
            InspectionRow("File SHA-256", apk.fileSha256)
            val match = apk.installedSignatureMatches
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (match) {
                        true -> Icons.Default.CheckCircle
                        false -> Icons.Default.Warning
                        null -> Icons.Default.Fingerprint
                    },
                    contentDescription = null
                )
                Text(
                    when (match) {
                        true -> "Signing certificate matches the installed package"
                        false -> "Signing certificate does not match the installed package"
                        null -> "No installed package with this ID was found"
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(permission, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InspectionRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
