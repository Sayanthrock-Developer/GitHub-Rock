package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.CopyableRepositoryFile
import com.sayanthrock.githubrock.core.util.RepositoryCopyBundle
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ViewedRepositoryFile(
    val path: String,
    val content: String?,
    val rawUrl: String?,
    val sizeBytes: Long
)

data class RepositoryFileManagerState(
    val currentPath: String = "",
    val entries: List<ContentEntry> = emptyList(),
    val selectedFile: ViewedRepositoryFile? = null,
    val copyBundle: String? = null,
    val loading: Boolean = false,
    val progress: Int = 100,
    val progressLabel: String = "Ready",
    val error: String? = null,
    val message: String? = null,
    val pullRequestUrl: String? = null
)

@HiltViewModel
class RepositoryFileManagerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GitHubRepository
) : ViewModel() {
    private val owner: String = checkNotNull(savedStateHandle["owner"])
    private val repo: String = checkNotNull(savedStateHandle["repo"])
    private val demo: Boolean = savedStateHandle["demo"] ?: false
    private var defaultBranch: String = "main"
    private var browseRequestId: Long = 0

    private val _state = MutableStateFlow(RepositoryFileManagerState())
    val state: StateFlow<RepositoryFileManagerState> = _state.asStateFlow()

    fun start(branch: String) {
        val normalizedBranch = branch.trim().ifBlank { "main" }
        val branchChanged = normalizedBranch != defaultBranch
        defaultBranch = normalizedBranch
        if (branchChanged || _state.value.entries.isEmpty()) {
            loadDirectory(_state.value.currentPath)
        }
    }

    fun loadDirectory(path: String) = viewModelScope.launch {
        val requestId = ++browseRequestId
        val normalized = path.trim('/')
        if (normalized.isNotEmpty() && !isSafePath(normalized)) {
            if (requestId == browseRequestId) reportError("Use a valid repository path")
            return@launch
        }
        updateProgress(0, "Opening repository files")
        try {
            val entries = if (demo) DemoData.contents else repository.contents(owner, repo, normalized, defaultBranch)
            if (requestId != browseRequestId) return@launch
            updateProgress(75, "Preparing file list")
            _state.update {
                it.copy(
                    currentPath = normalized,
                    entries = entries.sortedWith(
                        compareByDescending<ContentEntry> { entry -> entry.type == "dir" }
                            .thenBy { entry -> entry.name.lowercase() }
                    ),
                    selectedFile = null,
                    copyBundle = null,
                    error = null,
                    message = null
                )
            }
            finishProgress("${entries.size} items loaded")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (requestId == browseRequestId) failProgress(error.message ?: "Unable to load repository files")
        }
    }

    fun goUp() {
        val parent = _state.value.currentPath.substringBeforeLast('/', "")
        loadDirectory(parent)
    }

    fun openFile(entry: ContentEntry) = viewModelScope.launch {
        if (entry.type != "file") return@launch
        val requestId = ++browseRequestId
        updateProgress(0, "Opening ${entry.name}")
        if (!isTextFile(entry.name) || entry.size > MAX_VIEWABLE_TEXT_BYTES) {
            if (requestId != browseRequestId) return@launch
            _state.update {
                it.copy(
                    selectedFile = ViewedRepositoryFile(entry.path, null, entry.downloadUrl, entry.size),
                    error = null
                )
            }
            finishProgress("Raw file ready")
            return@launch
        }

        try {
            val content = if (demo) {
                "# Demo file\n\nDemo mode does not download repository contents.\n"
            } else {
                SourceFileDecoder.decode(repository.file(owner, repo, entry.path, defaultBranch))
            }
            if (requestId != browseRequestId) return@launch
            _state.update {
                it.copy(
                    selectedFile = ViewedRepositoryFile(entry.path, content, entry.downloadUrl, entry.size),
                    error = null
                )
            }
            finishProgress("File opened")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (requestId == browseRequestId) failProgress(error.message ?: "Unable to open this file")
        }
    }

    fun copyVisibleTextFiles() = viewModelScope.launch {
        val candidates = _state.value.entries.filter { entry ->
            entry.type == "file" && isTextFile(entry.name) && entry.size <= MAX_VIEWABLE_TEXT_BYTES
        }
        if (candidates.isEmpty()) {
            reportError("No visible text or code files are available to copy")
            return@launch
        }
        if (candidates.size > RepositoryCopyBundle.MAX_FILES) {
            reportError("This folder has too many text files. Copy up to ${RepositoryCopyBundle.MAX_FILES} at a time")
            return@launch
        }

        updateProgress(0, "Reading ${candidates.size} files")
        try {
            val files = candidates.mapIndexed { index, entry ->
                val content = if (demo) {
                    "# Demo copy for ${entry.path}\n"
                } else {
                    SourceFileDecoder.decode(repository.file(owner, repo, entry.path, defaultBranch))
                }
                updateProgress(
                    progress = ((index + 1) * 85 / candidates.size).coerceIn(1, 85),
                    label = "Reading ${index + 1} / ${candidates.size}"
                )
                CopyableRepositoryFile(entry.path, content)
            }
            val bundle = RepositoryCopyBundle.build(files)
            _state.update {
                it.copy(
                    copyBundle = bundle,
                    message = "${files.size} text files copied",
                    error = null
                )
            }
            finishProgress("Copy complete")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failProgress(error.message ?: "Unable to copy these files")
        }
    }

    fun consumeCopyBundle() = _state.update { it.copy(copyBundle = null) }
    fun closeFile() = _state.update { it.copy(selectedFile = null) }

    fun prepareUpload() = _state.update {
        it.copy(error = null, message = null, pullRequestUrl = null)
    }

    fun reportError(message: String) = _state.update {
        it.copy(
            loading = false,
            progress = 100,
            progressLabel = "Needs attention",
            error = message
        )
    }

    fun uploadTextFile(
        path: String,
        bytes: ByteArray,
        featureBranch: String,
        commitMessage: String
    ) = viewModelScope.launch {
        if (demo) {
            reportError("Demo mode does not upload files")
            return@launch
        }
        if (!isSafePath(path)) {
            reportError("Use a valid relative file path")
            return@launch
        }
        if (bytes.isEmpty() || bytes.size > MAX_UPLOAD_BYTES) {
            reportError("Choose a UTF-8 text or code file up to 1 MB")
            return@launch
        }
        if (!BuildRunTracker.isSafeRef(featureBranch) || commitMessage.isBlank()) {
            reportError("Use a valid review branch and commit message")
            return@launch
        }

        val content = runCatching { decodeUtf8(bytes) }.getOrElse {
            reportError("Only valid UTF-8 text and code files can be uploaded")
            return@launch
        }

        updateProgress(0, "Preparing upload")
        try {
            updateProgress(15, "Checking destination path")
            val existingEntry = try {
                repository.file(owner, repo, path, defaultBranch)
            } catch (error: HttpException) {
                if (error.code() == 404) null else throw error
            }
            existingEntry?.let { check(it.type == "file") { "The destination path is not a file" } }
            val currentSha = existingEntry?.sha?.takeIf(String::isNotBlank)

            updateProgress(35, "Creating review branch")
            val pull = repository.commitFileAndOpenPullRequest(
                owner = owner,
                repo = repo,
                path = path,
                content = content,
                currentSha = currentSha,
                baseBranch = defaultBranch,
                featureBranch = featureBranch,
                commitMessage = commitMessage.trim(),
                pullTitle = if (currentSha == null) "Upload $path" else "Update $path",
                pullBody = "Uploaded from GitHub Rock on a review branch. The default branch was not overwritten."
            )
            _state.update {
                it.copy(
                    message = "Pull request #${pull.number} created for $path",
                    pullRequestUrl = pull.htmlUrl,
                    error = null
                )
            }
            finishProgress("Upload complete")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failProgress(error.message ?: "Unable to upload this file")
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun updateProgress(progress: Int, label: String) {
        _state.update {
            it.copy(
                loading = true,
                progress = progress.coerceIn(0, 100),
                progressLabel = label,
                error = null,
                message = null
            )
        }
    }

    private fun finishProgress(label: String) {
        _state.update { it.copy(loading = false, progress = 100, progressLabel = label) }
    }

    private fun failProgress(message: String) {
        _state.update {
            it.copy(
                loading = false,
                progress = 100,
                progressLabel = "Needs attention",
                error = message
            )
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun isSafePath(path: String): Boolean =
        path.matches(Regex("^[A-Za-z0-9._/-]+$")) &&
            !path.startsWith('/') &&
            !path.endsWith('/') &&
            !path.contains("..") &&
            !path.contains("//")

    private fun isTextFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in TEXT_EXTENSIONS || name in setOf("LICENSE", "Dockerfile", "Makefile", "gradlew")
    }

    private companion object {
        const val MAX_VIEWABLE_TEXT_BYTES = 1_000_000L
        const val MAX_UPLOAD_BYTES = 1_000_000
        val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "kt", "kts", "java", "xml", "json", "yaml", "yml",
            "gradle", "properties", "toml", "js", "jsx", "ts", "tsx", "css", "scss", "html",
            "py", "rb", "go", "rs", "c", "cpp", "h", "hpp", "sh", "bat", "ps1", "sql"
        )
    }
}
