package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuildsActionState(
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val pullRequestUrl: String? = null
)

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BuildsActionState())
    val state: StateFlow<BuildsActionState> = _state.asStateFlow()

    fun createWorkflowPullRequest(
        selected: GitHubRepositoryModel,
        featureBranch: String,
        yaml: String,
        artifact: AndroidArtifactType
    ) = viewModelScope.launch {
        if (!featureBranch.matches(Regex("^[A-Za-z0-9._/-]+$"))) {
            _state.update { it.copy(error = "Use a valid review branch name", message = null) }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null, message = null, pullRequestUrl = null) }
        runCatching {
            repository.commitFileAndOpenPullRequest(
                owner = selected.owner.login,
                repo = selected.name,
                path = ".github/workflows/android-build.yml",
                content = yaml,
                currentSha = null,
                baseBranch = selected.defaultBranch,
                featureBranch = featureBranch,
                commitMessage = "Add Android build workflow",
                pullTitle = "Add Android ${artifact.name} workflow",
                pullBody = "Generated and reviewed in GitHub Rock. Uses GitHub-hosted runners and does not contain signing secrets."
            )
        }.onSuccess { pull ->
            _state.update { it.copy(message = "Pull request #${pull.number} created", pullRequestUrl = pull.htmlUrl) }
        }.onFailure { error ->
            _state.update { it.copy(error = error.message ?: "Unable to create workflow pull request") }
        }
        _state.update { it.copy(loading = false) }
    }
}
