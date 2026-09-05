package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.repository.BulkUploadFile
import com.sayanthrock.githubrock.data.repository.GitHubBulkUploadRepository
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ViewedRepositoryFile(val path: String, val content: String?, val rawUrl: String?, val sizeBytes: Long)
enum class UploadFileStatus { PENDING, UPLOADING, UPLOADED, FAILED }
enum class UploadMode { ALL, INDIVIDUALLY }
data class UploadQueueItem(val id: String = UUID.randomUUID().toString(), val name: String, val path: String, val bytes: ByteArray, val status: UploadFileStatus = UploadFileStatus.PENDING, val error: String? = null)

data class RepositoryFileManagerState(
    val currentPath: String = "", val entries: List<ContentEntry> = emptyList(), val selectedFile: ViewedRepositoryFile? = null,
    val loading: Boolean = false, val operationLabel: String = "Ready", val error: String? = null, val message: String? = null,
    val pullRequestUrl: String? = null, val uploadQueue: List<UploadQueueItem> = emptyList(), val uploadMode: UploadMode = UploadMode.ALL,
    val uploadBranch: String = "", val uploadCommitMessage: String = "", val uploadDestination: String = "", val uploadOverwriteConflicts: Boolean = false
)

@HiltViewModel
class RepositoryFileManagerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, private val repository: GitHubRepository, private val bulkUploader: GitHubBulkUploadRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repo: String = checkNotNull(savedStateHandle["repo"])
    private var defaultBranch = "main"
    private var browseRequestId = 0L
    private val _state = MutableStateFlow(RepositoryFileManagerState())
    val state: StateFlow<RepositoryFileManagerState> = _state.asStateFlow()

    fun start(branch: String) {
        val normalized = branch.trim().ifBlank { "main" }
        val changed = normalized != defaultBranch
        defaultBranch = normalized
        if (changed || _state.value.entries.isEmpty()) loadDirectory(_state.value.currentPath)
    }

    fun loadDirectory(path: String) = viewModelScope.launch {
        val requestId = ++browseRequestId
        val normalized = path.trim('/')
        if (normalized.isNotEmpty() && !isSafePath(normalized)) { reportError("Use a valid repository path"); return@launch }
        startOperation("Opening repository files")
        try {
            val entries = repository.contents(owner, repo, normalized, defaultBranch)
            if (requestId != browseRequestId) return@launch
            _state.update { it.copy(currentPath = normalized, entries = entries.sortedWith(compareByDescending<ContentEntry> { it.type == "dir" }.thenBy { it.name.lowercase() }), selectedFile = null, error = null, message = null) }
            finishOperation("${entries.size} items loaded")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Throwable) { if (requestId == browseRequestId) failOperation(error.message ?: "Unable to load repository files") }
    }

    fun goUp() = loadDirectory(_state.value.currentPath.substringBeforeLast('/', ""))

    fun openFile(entry: ContentEntry) = viewModelScope.launch {
        if (entry.type != "file") return@launch
        val requestId = ++browseRequestId
        startOperation("Opening ${entry.name}")
        if (!isTextFile(entry.name) || entry.size > MAX_VIEWABLE_TEXT_BYTES) {
            _state.update { it.copy(selectedFile = ViewedRepositoryFile(entry.path, null, entry.downloadUrl, entry.size), error = null) }
            finishOperation("Raw file ready")
            return@launch
        }
        try {
            val content = SourceFileDecoder.decode(repository.file(owner, repo, entry.path, defaultBranch))
            if (requestId != browseRequestId) return@launch
            _state.update { it.copy(selectedFile = ViewedRepositoryFile(entry.path, content, entry.downloadUrl, entry.size), error = null) }
            finishOperation("File opened")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Throwable) { if (requestId == browseRequestId) failOperation(error.message ?: "Unable to open this file") }
    }

    fun closeFile() = _state.update { it.copy(selectedFile = null) }
    fun prepareUpload(files: List<UploadQueueItem>, destination: String, mode: UploadMode, branch: String, message: String, overwriteConflicts: Boolean) =
        _state.update { it.copy(uploadQueue = files, uploadDestination = destination.trim('/'), uploadMode = mode, uploadBranch = branch, uploadCommitMessage = message, uploadOverwriteConflicts = overwriteConflicts, error = null, message = null, pullRequestUrl = null) }
    fun setUploadMode(mode: UploadMode) = _state.update { it.copy(uploadMode = mode) }
    fun setOverwriteConflicts(value: Boolean) = _state.update { it.copy(uploadOverwriteConflicts = value) }
    fun reportError(message: String) = _state.update { it.copy(loading = false, operationLabel = "Needs attention", error = message) }
    fun uploadSelected() = viewModelScope.launch { performUpload(_state.value.uploadQueue.filter { it.status != UploadFileStatus.UPLOADED }) }
    fun retryFailed() = viewModelScope.launch { performUpload(_state.value.uploadQueue.filter { it.status == UploadFileStatus.FAILED }) }

    private suspend fun performUpload(selected: List<UploadQueueItem>) {
        if (selected.isEmpty()) { reportError("There are no files to upload"); return }
        val current = _state.value
        if (!BuildRunTracker.isSafeRef(current.uploadBranch) || current.uploadCommitMessage.isBlank()) { reportError("Use a valid review branch and commit message"); return }
        if (selected.any { !isSafePath(it.path) }) { reportError("One or more repository paths are invalid"); return }
        if (selected.sumOf { it.bytes.size.toLong() } > MAX_TOTAL_BYTES) { reportError("The selected files exceed the 100 MB total upload limit"); return }
        startOperation("Checking ${selected.size} destination paths")
        try {
            val conflicts = mutableListOf<String>()
            selected.forEach { item ->
                try {
                    val existing = repository.file(owner, repo, item.path, defaultBranch)
                    if (existing.type == "file") conflicts += item.path
                } catch (error: HttpException) { if (error.code() != 404) throw error }
            }
            if (conflicts.isNotEmpty() && !current.uploadOverwriteConflicts) {
                reportError("Conflicts found: ${conflicts.take(5).joinToString()}${if (conflicts.size > 5) " and ${conflicts.size - 5} more" else ""}. Enable overwrite to continue.")
                return
            }
            val selectedIds = selected.map { it.id }.toSet()
            _state.update { state -> state.copy(uploadQueue = state.uploadQueue.map { if (it.id in selectedIds) it.copy(status = UploadFileStatus.UPLOADING, error = null) else it }, error = null) }
            val files = selected.map { BulkUploadFile(it.path, it.bytes) }
            val result = if (current.uploadMode == UploadMode.ALL) {
                bulkUploader.uploadAll(owner, repo, files, defaultBranch, current.uploadBranch, current.uploadCommitMessage, "Upload ${files.size} files", "Uploaded ${files.size} file(s) from GitHub Rock using one Git commit. The default branch was not overwritten.")
            } else {
                bulkUploader.uploadIndividually(owner, repo, files, defaultBranch, current.uploadBranch, current.uploadCommitMessage, "Upload ${files.size} files", "Uploaded ${files.size} file(s) from GitHub Rock using one commit per file. The default branch was not overwritten.")
            }
            val committed = result.committedFiles.toSet()
            _state.update { state -> state.copy(uploadQueue = state.uploadQueue.map { item ->
                when {
                    item.id !in selectedIds -> item
                    item.path in committed -> item.copy(status = UploadFileStatus.UPLOADED, error = null)
                    result.failedFiles.containsKey(item.path) -> item.copy(status = UploadFileStatus.FAILED, error = result.failedFiles[item.path])
                    else -> item.copy(status = UploadFileStatus.FAILED, error = "Upload did not complete")
                }
            }, pullRequestUrl = result.pullRequest.htmlUrl, message = "Pull request #${result.pullRequest.number} created with ${committed.size} uploaded file(s).", error = null) }
            finishOperation(if (result.failedFiles.isEmpty()) "Upload complete" else "Upload complete with failures")
            loadDirectory(_state.value.currentPath)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Throwable) {
            val selectedIds = selected.map { it.id }.toSet()
            _state.update { state -> state.copy(uploadQueue = state.uploadQueue.map { if (it.id in selectedIds && it.status == UploadFileStatus.UPLOADING) it.copy(status = UploadFileStatus.FAILED, error = error.message ?: "Upload failed") else it }) }
            failOperation(error.message ?: "Unable to upload the selected files")
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun dismissMessage() = _state.update { it.copy(message = null) }
    private fun startOperation(label: String) { _state.update { it.copy(loading = true, operationLabel = label, error = null, message = null) } }
    private fun finishOperation(label: String) { _state.update { it.copy(loading = false, operationLabel = label) } }
    private fun failOperation(message: String) { _state.update { it.copy(loading = false, operationLabel = "Needs attention", error = message) } }
    private fun isSafePath(path: String): Boolean = path.matches(Regex("^[A-Za-z0-9._/-]+$")) && !path.startsWith('/') && !path.endsWith('/') && !path.contains("..") && !path.contains("//")
    private fun isTextFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS || name in setOf("LICENSE", "Dockerfile", "Makefile", "gradlew")
    private companion object {
        const val MAX_VIEWABLE_TEXT_BYTES = 1_000_000L
        const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L
        val TEXT_EXTENSIONS = setOf("txt", "md", "markdown", "kt", "kts", "java", "xml", "json", "yaml", "yml", "gradle", "properties", "toml", "js", "jsx", "ts", "tsx", "css", "scss", "html", "py", "rb", "go", "rs", "c", "cpp", "h", "hpp", "sh", "bat", "ps1", "sql")
    }
}
