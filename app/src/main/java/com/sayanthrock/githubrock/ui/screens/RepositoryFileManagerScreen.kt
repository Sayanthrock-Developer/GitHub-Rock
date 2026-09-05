package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LocalUploadFile(val name: String, val relativePath: String, val bytes: ByteArray)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryFileManagerScreen(repository: GitHubRepositoryModel?, onBack: () -> Unit, viewModel: RepositoryFileManagerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showUploadDialog by remember { mutableStateOf(false) }
    var localFiles by remember { mutableStateOf<List<LocalUploadFile>>(emptyList()) }
    var destination by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(UploadMode.ALL) }
    var overwrite by remember { mutableStateOf(false) }

    LaunchedEffect(repository?.id, repository?.defaultBranch) { viewModel.start(repository?.defaultBranch ?: "main") }
    LaunchedEffect(state.pullRequestUrl) { if (!state.pullRequestUrl.isNullOrBlank()) showUploadDialog = false }

    fun openUrl(url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    fun showUpload(files: List<LocalUploadFile>) {
        if (files.isEmpty()) { viewModel.reportError("No readable files were found"); return }
        localFiles = files
        destination = state.currentPath
        branch = "github-rock/upload-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        message = if (files.size == 1) "Upload ${files.first().name}" else "Upload ${files.size} files"
        mode = UploadMode.ALL
        overwrite = false
        showUploadDialog = true
    }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch { runCatching { uris.mapIndexed { i, uri -> readUriFile(uri, "file-${i + 1}", context) } }.onSuccess(::showUpload).onFailure { viewModel.reportError(it.message ?: "Unable to read selected files") } }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { runCatching { readFolderFiles(uri, context) }.onSuccess(::showUpload).onFailure { viewModel.reportError(it.message ?: "Unable to read selected folder") } }
    }

    Scaffold(topBar = { TopAppBar(title = { Column { Text("Repository files", fontWeight = FontWeight.Bold); Text(repository?.fullName ?: "GitHub repository", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OperationStatus(state.loading, state.operationLabel, state.error != null) }
            state.error?.let { item { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ErrorOutline, null); Spacer(Modifier.size(8.dp)); Text(it, Modifier.weight(1f)); TextButton(onClick = viewModel::dismissError) { Text("Dismiss") } } } } }
            state.message?.let { item { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.padding(12.dp)) { Text(it, fontWeight = FontWeight.Bold); state.pullRequestUrl?.let { url -> TextButton(onClick = { openUrl(url) }) { Text("Open pull request"); Icon(Icons.Default.OpenInNew, null) } } } } } }
            item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.currentPath.ifBlank { "Repository root" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Upload multiple files or an entire folder. All uploads stay on a review branch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::goUp, enabled = state.currentPath.isNotBlank() && !state.loading, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.size(5.dp)); Text("Up") }
                    Button(onClick = { multiPicker.launch(arrayOf("*/*")) }, enabled = !state.loading, modifier = Modifier.weight(1f)) { Icon(Icons.Default.UploadFile, null); Spacer(Modifier.size(5.dp)); Text("Add files") }
                }
                OutlinedButton(onClick = { folderPicker.launch(null) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Folder, null); Spacer(Modifier.size(5.dp)); Text("Add folder") }
            } } }
            if (state.uploadQueue.isNotEmpty()) item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Upload queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                state.uploadQueue.forEach { file ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (file.status == UploadFileStatus.UPLOADED) Icons.Default.CheckCircle else if (file.status == UploadFileStatus.FAILED) Icons.Default.ErrorOutline else Icons.Default.UploadFile, null, tint = if (file.status == UploadFileStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp)); Column(Modifier.weight(1f)) { Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(file.error ?: file.status.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (state.uploadQueue.any { it.status == UploadFileStatus.FAILED }) OutlinedButton(onClick = viewModel::retryFailed, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Retry failed files only") }
            } } }
            state.selectedFile?.let { file -> item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(file.path, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold); TextButton(onClick = viewModel::closeFile) { Text("Close") } }; if (file.content != null) { OutlinedButton(onClick = { clipboard.setText(AnnotatedString(file.content)) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ContentCopy, null); Text("Copy file") }; SelectionContainer { Text(file.content, modifier = Modifier.fillMaxWidth(), fontFamily = FontFamily.Monospace, maxLines = 32, overflow = TextOverflow.Ellipsis) } }; file.rawUrl?.let { url -> TextButton(onClick = { openUrl(url) }) { Text("Open raw file"); Icon(Icons.Default.OpenInNew, null) } } } } } }
            items(state.entries, key = { it.path }) { entry -> FileEntryCard(entry, !state.loading, { if (entry.type == "dir") viewModel.loadDirectory(entry.path) else viewModel.openFile(entry) }, entry.downloadUrl?.let { url -> { openUrl(url) } }) }
        }
    }

    if (showUploadDialog) AlertDialog(onDismissRequest = { if (!state.loading) showUploadDialog = false }, title = { Text("Upload ${localFiles.size} file${if (localFiles.size == 1) "" else "s"}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Review selected files, destination and commit mode before upload.", style = MaterialTheme.typography.bodySmall)
        localFiles.take(8).forEach { Text("• ${it.relativePath}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (localFiles.size > 8) Text("+ ${localFiles.size - 8} more")
        OutlinedTextField(value = destination, onValueChange = { destination = it }, enabled = !state.loading, label = { Text("Repository destination folder") }, singleLine = true)
        OutlinedTextField(value = branch, onValueChange = { branch = it }, enabled = !state.loading, label = { Text("Review branch") }, singleLine = true)
        OutlinedTextField(value = message, onValueChange = { message = it }, enabled = !state.loading, label = { Text("Commit message") }, singleLine = true)
        Text("Upload mode", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = mode == UploadMode.ALL, onClick = { mode = UploadMode.ALL }); Text("Upload All — one Git commit") }
        Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = mode == UploadMode.INDIVIDUALLY, onClick = { mode = UploadMode.INDIVIDUALLY }); Text("Upload Individually — one commit per file") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = overwrite, onCheckedChange = { overwrite = it }); Text("Allow overwrite of existing files") }
    } }, confirmButton = { TextButton(enabled = !state.loading, onClick = {
        val base = destination.trim().trim('/')
        val queue = localFiles.map { file -> UploadQueueItem(name = file.name, path = listOfNotNull(base.takeIf(String::isNotBlank), file.relativePath).joinToString("/"), bytes = file.bytes) }
        viewModel.prepareUpload(queue, base, mode, branch, message, overwrite)
        viewModel.uploadSelected()
    }) { Text(if (state.loading) "Uploading…" else "Upload") } }, dismissButton = { TextButton(enabled = !state.loading, onClick = { showUploadDialog = false }) { Text("Cancel") } })
}

private suspend fun readUriFile(uri: Uri, fallback: String, context: Context): LocalUploadFile = withContext(Dispatchers.IO) {
    val name = DocumentFile.fromSingleUri(context, uri)?.name ?: fallback
    LocalUploadFile(name, sanitizePath(name), context.contentResolver.openInputStream(uri)?.use(::readLimited) ?: error("Unable to read $name"))
}

private suspend fun readFolderFiles(uri: Uri, context: Context): List<LocalUploadFile> = withContext(Dispatchers.IO) {
    val root = DocumentFile.fromTreeUri(context, uri) ?: error("Unable to open the selected folder")
    val result = mutableListOf<LocalUploadFile>()
    fun visit(folder: DocumentFile, prefix: String) {
        folder.listFiles().forEach { child ->
            val name = child.name?.takeIf(String::isNotBlank) ?: return@forEach
            val relative = if (prefix.isBlank()) sanitizePath(name) else "$prefix/${sanitizePath(name)}"
            if (child.isDirectory) visit(child, relative) else if (child.isFile) {
                val bytes = context.contentResolver.openInputStream(child.uri)?.use(::readLimited) ?: error("Unable to read $relative")
                result += LocalUploadFile(name, relative, bytes)
            }
        }
    }
    visit(root, "")
    result
}

private fun readLimited(input: java.io.InputStream): ByteArray {
    val max = 20L * 1024L * 1024L
    val out = ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0L
    while (true) { val count = input.read(buffer); if (count < 0) break; total += count; if (total > max) error("A selected file exceeds the 20 MB limit"); out.write(buffer, 0, count) }
    if (total == 0L) error("Empty files cannot be uploaded")
    return out.toByteArray()
}
private fun sanitizePath(value: String): String = value.replace("/", "_").replace("\\", "_").trim().ifBlank { "uploaded-file" }

@Composable private fun OperationStatus(loading: Boolean, label: String, error: Boolean) { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = (if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = .10f), border = BorderStroke(1.dp, (if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = .3f))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(if (error) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null); Spacer(Modifier.size(10.dp)); Text(label, fontWeight = FontWeight.Bold) } } }

@Composable private fun FileEntryCard(entry: ContentEntry, enabled: Boolean, onClick: () -> Unit, onRaw: (() -> Unit)?) { Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = if (entry.type == "dir") Icons.Default.Folder else Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (entry.type == "dir") "Folder" else "${entry.size} bytes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (onRaw != null) IconButton(onClick = onRaw, enabled = enabled) { Icon(Icons.Default.OpenInNew, "Open raw") } } } }
