package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RenameBranchRequest
import com.sayanthrock.githubrock.core.model.RepositoryCreationForm
import com.sayanthrock.githubrock.core.model.RepositoryLicenseTemplate
import com.sayanthrock.githubrock.core.model.RepositoryOwnerOption
import com.sayanthrock.githubrock.core.model.RepositoryOwnerType
import com.sayanthrock.githubrock.core.network.RepositoryCreationApi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class CreateRepositoryState(
    val owners: List<RepositoryOwnerOption> = emptyList(),
    val gitignoreTemplates: List<String> = emptyList(),
    val licenses: List<RepositoryLicenseTemplate> = emptyList(),
    val loadingOptions: Boolean = false,
    val optionsLoaded: Boolean = false,
    val submitting: Boolean = false,
    val optionWarning: String? = null,
    val error: String? = null,
    val createdRepository: GitHubRepositoryModel? = null,
    val successWarning: String? = null
)

@HiltViewModel
class CreateRepositoryViewModel @Inject constructor(
    private val api: RepositoryCreationApi
) : ViewModel() {
    private val _state = MutableStateFlow(CreateRepositoryState())
    val state: StateFlow<CreateRepositoryState> = _state.asStateFlow()

    fun loadOptions() {
        if (_state.value.loadingOptions || _state.value.optionsLoaded) return
        viewModelScope.launch {
            _state.update { it.copy(loadingOptions = true, error = null, optionWarning = null) }
            try {
                val result = coroutineScope {
                    val user = async { api.authenticatedUser() }
                    val organizations = async { runCatching { api.organizations() } }
                    val gitignoreTemplates = async { runCatching { api.gitignoreTemplates() } }
                    val licenses = async { runCatching { api.licenses() } }
                    OptionsResult(
                        user = user.await(),
                        organizations = organizations.await().getOrDefault(emptyList()),
                        gitignoreTemplates = gitignoreTemplates.await().getOrDefault(emptyList()),
                        licenses = licenses.await().getOrDefault(emptyList()),
                        optionalRequestFailed = organizations.await().isFailure ||
                            gitignoreTemplates.await().isFailure || licenses.await().isFailure
                    )
                }
                val owners = buildList {
                    add(RepositoryOwnerOption(result.user.login, result.user.avatarUrl, RepositoryOwnerType.User))
                    result.organizations.forEach { organization ->
                        add(
                            RepositoryOwnerOption(
                                login = organization.login,
                                avatarUrl = organization.avatarUrl,
                                type = RepositoryOwnerType.Organization
                            )
                        )
                    }
                }
                _state.update {
                    it.copy(
                        owners = owners,
                        gitignoreTemplates = result.gitignoreTemplates.sorted(),
                        licenses = result.licenses.sortedBy(RepositoryLicenseTemplate::name),
                        loadingOptions = false,
                        optionsLoaded = true,
                        optionWarning = if (result.optionalRequestFailed) {
                            "Some owner or template options could not be loaded. Repository creation is still available."
                        } else null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        loadingOptions = false,
                        optionsLoaded = false,
                        error = repositoryCreationError(error, privateRepository = false, organization = false)
                    )
                }
            }
        }
    }

    fun create(form: RepositoryCreationForm) {
        if (_state.value.submitting) return
        val validationError = form.validationError()
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        val owner = _state.value.owners.firstOrNull { it.login == form.ownerLogin }
        if (owner == null) {
            _state.update { it.copy(error = "Choose an available owner account.") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(submitting = true, error = null, createdRepository = null, successWarning = null)
            }
            try {
                val created = when (owner.type) {
                    RepositoryOwnerType.User -> api.createUserRepository(form.toRequest())
                    RepositoryOwnerType.Organization -> api.createOrganizationRepository(owner.login, form.toRequest())
                }
                val requestedBranch = form.defaultBranch.trim()
                var result = created
                var warning: String? = null
                if (form.initializeReadme && requestedBranch != created.defaultBranch) {
                    val rename = api.renameBranch(
                        owner = created.owner.login,
                        repository = created.name,
                        branch = created.defaultBranch,
                        request = RenameBranchRequest(requestedBranch)
                    )
                    if (rename.isSuccessful) {
                        result = created.copy(defaultBranch = requestedBranch)
                    } else {
                        warning = "Repository created, but the default branch could not be renamed. It remains ${created.defaultBranch}."
                    }
                }
                _state.update {
                    it.copy(
                        submitting = false,
                        createdRepository = result,
                        successWarning = warning
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        submitting = false,
                        error = repositoryCreationError(
                            error,
                            privateRepository = form.privateRepository,
                            organization = owner.type == RepositoryOwnerType.Organization
                        )
                    )
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun consumeCreatedRepository() = _state.update {
        it.copy(createdRepository = null, successWarning = null)
    }

    private data class OptionsResult(
        val user: com.sayanthrock.githubrock.core.model.GitHubUser,
        val organizations: List<com.sayanthrock.githubrock.core.model.RepositoryOrganization>,
        val gitignoreTemplates: List<String>,
        val licenses: List<RepositoryLicenseTemplate>,
        val optionalRequestFailed: Boolean
    )
}

internal fun repositoryCreationError(
    error: Throwable,
    privateRepository: Boolean,
    organization: Boolean
): String = when (error) {
    is HttpException -> when (error.code()) {
        401 -> "GitHub authorization expired. Sign in again before creating a repository."
        403 -> when {
            organization -> "GitHub denied repository creation. The OAuth token needs the repo scope and your organization role must allow repository creation."
            privateRepository -> "GitHub denied private repository creation. Reauthorize with the repo OAuth scope."
            else -> "GitHub denied repository creation. Reauthorize with the public_repo or repo OAuth scope."
        }
        404 -> if (organization) {
            "The organization is unavailable or your membership does not permit repository creation."
        } else {
            "The selected owner account is unavailable."
        }
        422 -> "GitHub rejected this repository. The name may already exist or one of the selected templates is invalid."
        429 -> "GitHub rate limiting is active. Try again after the rate-limit reset."
        else -> "GitHub could not create the repository (HTTP ${error.code()})."
    }
    is IOException -> "Network connection failed while creating the repository."
    else -> error.message?.takeIf(String::isNotBlank) ?: "Unable to create the repository."
}
