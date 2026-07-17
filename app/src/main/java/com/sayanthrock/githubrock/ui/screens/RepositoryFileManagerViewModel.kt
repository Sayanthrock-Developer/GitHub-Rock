package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.ContentEntry
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.SourceFileDecoder
import com.sayanthrock.githubrock.data.demo.DemoData
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val _state = MutableStateFlow(RepositoryFileManagerState())
    val state: StateFlow<RepositoryFileManagerState> = _state.asStateFlow()

    fun start(branch: String) {
        if (branch.isNotBlank()) defaultBranch = branch
        if (_state.value.entries.isEmpty() && !_state.value.loading) loadDirectory("")
    }

    fun loadDirectory(path: String) = viewModelScope.launch {
        val normalized = path.trim('/')
        if (normalized.isNotEmpty() && !isSafePath(normalized)) {
            _state.update { it.copy(error = "Use a valid repository path") }
            return@launch
        }
        updateProgress(0, "Opening repository files")
        runCatching {
            if (demo) DemoData.contents else repository.contents(owner, repo, normalized, defaultBranch)
        }.onSuccess { entries ->
            updateProgress(75, "Preparing file list")
            _state.update {
                it.copy(
                    currentPath = normalized,
                    entries = entries.sortedWith(compareByDescending<ContentEntry> { entry -> entry.type == "dir" }.thenBy { entry -> entry.name.lowercase() }),
                    selectedFile = null,
                    error = null,
                    message = null
                )
            }
            finishProgress("${entries.size} items loaded")
        }.onFailure { error ->
            failProgress(error.message ?: "Unable to load repository files")
        }
    }

    fun goUp() {
        val parent = _state.value.currentPath.substringBeforeLast('/', "")
        loadDirectory(parent)
    }

    fun openFile(entry: ContentEntry) = viewModelScope.launch {
        if (entry.type != "file") return@launch
        updateProgress(0, "Opening ${entry.name}")
        if (!isTextFile(entry.name) || entry.size > MAX_VIEWABLE_TEXT_BYTES) {
            _state.update {
                it.copy(
                    selectedFile = ViewedRepositoryFile(entry.path, null, entry.downloadUrl, entry.size),
                    error = null
                )
            }
            finishProgress("Raw file ready")
            return@launch
        }

        runCatching {
            if (demo) {
                "# Demo file\n\nDemo mode does not download repository contents.\n"
            } else {
                SourceFileDecoder.decode(repository.file(owner, repo, entry.path, defaultBranch))
            }
        }.onSuccess { content ->
            _state.update {
                it.copy(
                    selectedFile = ViewedRepositoryFile(entry.path, content, entry.downloadUrl, entry.size),
                    error = null
                )
            }
            finishProgress("File opened")
        }.onFailure { error ->
            failProgress(error.message ?: "Unable to open this file")
        }
    }

    fun closeFile() = _state.update { it.copy(selectedFile = null) }

    fun uploadTextFile(
        path: String,
        bytes: ByteArray,
        featureBranch: String,
        commitMessage: String
    ) = viewModelScope.launch {
        if (demo) {
            _state.update { it.copy(error = "Demo mode does not upload files") }
            return@launch
        }
        if (!isSafePath(path)) {
            _state.update { it.copy(error = "Use a valid relative file path") }
            return@launch
        }
        if (bytes.isEmpty() || bytes.size > MAX_UPLOAD_BYTES) {
            _state.update { it.copy(error = "Choose a UTF-8 text or code file up to 1 MB") }
            return@launch
        }
        if (!BuildRunTracker.isSafeRef(featureBranch) || commitMessage.isBlank()) {
            _state.update { it.copy(error = "Use a valid review branch and commit message") }
            return@launch
        }

        val content = runCatching { decodeUtf8(bytes) }.getOrElse {
            _state.update { current -> current.copy(error = "Only valid UTF-8 text and code files can be uploaded") }
            return@launch
        }

        updateProgress(0, "Preparing upload")
        runCatching {
            updateProgress(25, "Creating review branch")
            repository.commitFileAndOpenPullRequest(
                owner = owner,
                repo = repo,
                path = path,
                content = content,
                currentSha = null,
                baseBranch = defaultBranch,
                featureBranch = featureBranch,
                commitMessage = commitMessage.trim(),
                pullTitle = "Upload $path",
                pullBody = "Uploaded from GitHub Rock on a review branch. The default branch was not overwritten."
            )
        }.onSuccess { pull ->
            _state.update {
                it.copy(
                    message = "Pull request #${pull.number} created for $path",
                    pullRequestUrl = pull.htmlUrl,
                    error = null
                )
            }
            finishProgress("Upload complete")
            loadDirectory(_state.value.currentPath)
        }.onFailure { error ->
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
        _state.update { it.copy(loading = false, progress = 100, progressLabel = "Needs attention", error = message) }
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
