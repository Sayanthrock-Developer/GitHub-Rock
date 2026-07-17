package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingTextUpload(
    val suggestedPath: String,
    val bytes: ByteArray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryFileManagerScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    viewModel: RepositoryFileManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUpload by remember { mutableStateOf<PendingTextUpload?>(null) }
    var uploadPath by remember { mutableStateOf("") }
    var uploadBranch by remember { mutableStateOf("") }
    var uploadMessage by remember { mutableStateOf("") }

    LaunchedEffect(repository?.id, repository?.defaultBranch) {
        viewModel.start(repository?.defaultBranch ?: "main")
    }

    val openUrl: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val displayName = context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "uploaded-file.txt"
                    val bytes = context.contentResolver.openInputStream(uri)?.use(::readLimited)
                        ?: error("Unable to read the selected file")
                    PendingTextUpload(sanitizeFileName(displayName), bytes)
                }
            }.onSuccess { upload ->
                pendingUpload = upload
                uploadPath = state.currentPath.takeIf(String::isNotBlank)?.let { "$it/${upload.suggestedPath}" }
                    ?: upload.suggestedPath
                uploadBranch = "github-rock/upload-${System.currentTimeMillis() / 1000}"
                uploadMessage = "Upload ${upload.suggestedPath}"
            }.onFailure {
                viewModel.dismissMessage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Repository files")
                        Text(
                            repository?.fullName ?: "GitHub repository",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FileOperationProgress(
                    progress = state.progress,
                    label = state.progressLabel,
                    hasError = state.error != null
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileStatusFrame(
                        label = if (state.error == null) "API healthy" else "Needs attention",
                        healthy = state.error == null
                    )
                    FileStatusFrame(
                        label = "${state.entries.size} items",
                        healthy = state.error == null
                    )
                    FileStatusFrame(
                        label = "Review-branch uploads",
                        healthy = true
                    )
                }
            }

            state.error?.let { message ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null)
                            Text(message, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::dismissError) { Text("Dismiss") }
                        }
                    }
                }
            }

            state.message?.let { message ->
                item {
                    Surface(
                        color = HEALTHY_GREEN.copy(alpha = .12f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, HEALTHY_GREEN.copy(alpha = .42f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(message, color = HEALTHY_GREEN, fontWeight = FontWeight.Bold)
                            state.pullRequestUrl?.takeIf(String::isNotBlank)?.let { url ->
                                TextButton(onClick = { openUrl(url) }) {
                                    Text("Open pull request")
                                    Spacer(Modifier.size(6.dp))
                                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            state.currentPath.ifBlank { "Repository root" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = viewModel::goUp,
                                enabled = state.currentPath.isNotBlank() && !state.loading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Up")
                            }
                            Button(
                                onClick = {
                                    picker.launch(
                                        arrayOf(
                                            "text/*",
                                            "application/json",
                                            "application/xml",
                                            "application/javascript"
                                        )
                                    )
                                },
                                enabled = !state.loading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Upload")
                            }
                        }
                        Text(
                            "View repository files and upload UTF-8 code or text files up to 1 MB. Uploads always create a review branch and pull request.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            state.selectedFile?.let { file ->
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(file.path, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${file.sizeBytes} bytes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = viewModel::closeFile) { Text("Close") }
                            }
                            if (file.content != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
                                ) {
                                    SelectionContainer {
                                        Text(
                                            file.content,
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 32,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            file.rawUrl?.takeIf(String::isNotBlank)?.let { url ->
                                OutlinedButton(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text(if (file.content == null) "View or download raw file" else "Open raw file")
                                }
                            }
                        }
                    }
                }
            }

            items(state.entries, key = { it.path }) { entry ->
                FileEntryCard(
                    entry = entry,
                    onClick = {
                        if (entry.type == "dir") viewModel.loadDirectory(entry.path) else viewModel.openFile(entry)
                    },
                    onOpenRaw = entry.downloadUrl?.let { url -> { openUrl(url) } }
                )
            }
        }
    }

    pendingUpload?.let { upload ->
        AlertDialog(
            onDismissRequest = { pendingUpload = null },
            title = { Text("Upload code or text file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The default branch will not be overwritten. GitHub Rock creates a review branch and pull request.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = uploadPath,
                        onValueChange = { uploadPath = it },
                        label = { Text("Repository path") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uploadBranch,
                        onValueChange = { uploadBranch = it },
                        label = { Text("Review branch") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uploadMessage,
                        onValueChange = { uploadMessage = it },
                        label = { Text("Commit message") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.uploadTextFile(uploadPath, upload.bytes, uploadBranch, uploadMessage)
                        pendingUpload = null
                    }
                ) { Text("Open pull request") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUpload = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileOperationProgress(progress: Int, label: String, hasError: Boolean) {
    val accent = if (hasError) MaterialTheme.colorScheme.error else HEALTHY_GREEN
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .36f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontWeight = FontWeight.Bold)
                Text("${progress.coerceIn(0, 100)} / 100", color = accent, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = accent
            )
        }
    }
}

@Composable
private fun FileStatusFrame(label: String, healthy: Boolean) {
    val accent = if (healthy) HEALTHY_GREEN else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = .10f),
        border = BorderStroke(1.dp, accent.copy(alpha = .34f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (healthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(label, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FileEntryCard(
    entry: ContentEntry,
    onClick: () -> Unit,
    onOpenRaw: (() -> Unit)?
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        if (entry.type == "dir") Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (entry.type == "dir") "Folder" else "${entry.size} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onOpenRaw != null) {
                IconButton(onClick = onOpenRaw) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open raw ${entry.name}")
                }
            }
        }
    }
}

private fun readLimited(input: java.io.InputStream): ByteArray {
    val buffer = ByteArray(16 * 1024)
    val output = ByteArrayOutputStream()
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > MAX_PICKER_BYTES) error("Choose a UTF-8 text or code file up to 1 MB")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun sanitizeFileName(value: String): String = value
    .trim()
    .replace(Regex("[^A-Za-z0-9._-]+"), "-")
    .trim('-')
    .ifBlank { "uploaded-file.txt" }

private val HEALTHY_GREEN = Color(0xFF2DA44E)
private const val MAX_PICKER_BYTES = 1_000_000
