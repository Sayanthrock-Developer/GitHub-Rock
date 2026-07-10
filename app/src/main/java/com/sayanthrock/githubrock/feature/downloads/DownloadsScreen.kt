package com.sayanthrock.githubrock.feature.downloads

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sayanthrock.githubrock.core.apk.ApkInspection
import com.sayanthrock.githubrock.core.apk.ApkInspector
import com.sayanthrock.githubrock.core.download.ArtifactDownloadWorker
import com.sayanthrock.githubrock.ui.theme.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    var url by rememberSaveable { mutableStateOf("") }
    var expectedSha by rememberSaveable { mutableStateOf("") }
    var workId by rememberSaveable { mutableStateOf<String?>(null) }
    var workInfo by remember { mutableStateOf<WorkInfo?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var inspection by remember { mutableStateOf<ApkInspection?>(null) }
    var inspectedFile by remember { mutableStateOf<File?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(workId) {
        workInfo = null
        val id = workId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (id != null) {
            while (true) {
                val latest = withContext(Dispatchers.IO) {
                    workManager.getWorkInfoById(id).get()
                }
                workInfo = latest
                if (latest?.state?.isFinished == true) break
                delay(600)
            }
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Downloads",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Background artifact downloads with SHA-256 verification.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("HTTPS artifact URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = expectedSha,
                        onValueChange = { expectedSha = it },
                        label = { Text("Expected SHA-256 (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!url.startsWith("https://")) {
                                validationError = "Only HTTPS downloads are allowed."
                                return@Button
                            }
                            if (
                                expectedSha.isNotBlank() &&
                                !expectedSha.matches(Regex("[A-Fa-f0-9]{64}"))
                            ) {
                                validationError = "SHA-256 must be 64 hexadecimal characters."
                                return@Button
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val request = OneTimeWorkRequestBuilder<ArtifactDownloadWorker>()
                                .setInputData(ArtifactDownloadWorker.input(url, expectedSha))
                                .setConstraints(
                                    Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build()
                                )
                                .setBackoffCriteria(
                                    BackoffPolicy.EXPONENTIAL,
                                    15,
                                    TimeUnit.SECONDS
                                )
                                .addTag(ArtifactDownloadWorker.TAG)
                                .build()
                            workManager.enqueue(request)
                            workId = request.id.toString()
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, null)
                        Text(" Download")
                    }
                }
            }
        }

        workInfo?.let { work ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Download status: ${work.state.name.lowercase()}",
                            fontWeight = FontWeight.SemiBold
                        )
                        val progress = work.progress.getInt(
                            ArtifactDownloadWorker.KEY_PROGRESS,
                            0
                        )
                        if (!work.state.isFinished) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("$progress%")
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { workManager.cancelWorkById(work.id) }
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Text(" Cancel")
                            }
                        }
                        when (work.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                val path = work.outputData
                                    .getString(ArtifactDownloadWorker.KEY_FILE_PATH)
                                    .orEmpty()
                                val sha = work.outputData
                                    .getString(ArtifactDownloadWorker.KEY_ACTUAL_SHA)
                                    .orEmpty()
                                Text(path, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "SHA-256: $sha",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (path.endsWith(".apk", ignoreCase = true)) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            runCatching {
                                                File(path).also { inspectedFile = it }
                                                    .let { ApkInspector(context).inspect(it) }
                                            }.onSuccess { inspection = it }
                                                .onFailure { validationError = it.message }
                                        }
                                    ) {
                                        Icon(Icons.Default.Security, null)
                                        Text(" Inspect APK")
                                    }
                                }
                            }

                            WorkInfo.State.FAILED -> Text(
                                work.outputData.getString(ArtifactDownloadWorker.KEY_ERROR)
                                    ?: "Download failed.",
                                color = MaterialTheme.colorScheme.error
                            )

                            else -> Unit
                        }
                    }
                }
            }
        }

        item {
            Text(
                "WorkManager restores active downloads after app restarts. Pause/resume, mirror selection, duplicate history and user-selected storage are planned and are not shown as complete in this version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    inspection?.let { apk ->
        AlertDialog(
            onDismissRequest = { inspection = null },
            title = { Text(apk.applicationName) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Package: ${apk.packageName}")
                    Text("Version: ${apk.versionName} (${apk.versionCode})")
                    Text("SDK: min ${apk.minSdk} · target ${apk.targetSdk}")
                    Text("File size: ${apk.fileSize} bytes")
                    Text("SHA-256: ${apk.sha256}")
                    Text("Signing fingerprint: ${apk.signingFingerprint}")
                    Text("Installed signature match: ${apk.signaturesMatch ?: "Not installed"}")
                    Spacer(Modifier.height(8.dp))
                    Text("Requested permissions", fontWeight = FontWeight.SemiBold)
                    apk.requestedPermissions.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        inspectedFile?.let { ApkInspector(context).install(it) }
                    }
                ) {
                    Text("Open installer")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspection = null }) {
                    Text("Close")
                }
            }
        )
    }
}
