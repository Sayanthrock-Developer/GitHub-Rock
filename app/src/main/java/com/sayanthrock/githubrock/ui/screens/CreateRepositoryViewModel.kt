package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RenameBranchRequest
import com.sayanthrock.githubrock.core.model.RepositoryCreationForm
import com.sayanthrock.githubrock.core.model.RepositoryLicenseTemplate
import com.sayanthrock.githubrock.core.model.RepositoryOrganization
import com.sayanthrock.githubrock.core.model.RepositoryOwnerOption
import com.sayanthrock.githubrock.core.model.RepositoryOwnerType
import com.sayanthrock.githubrock.core.network.RepositoryCreationApi
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
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

internal enum class RepositoryCreationOperation {
    LoadOptions,
    CreateRepository
}

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
                    val userDeferred = async { api.authenticatedUser() }
                    val organizationsDeferred = async {
                        runCatchingPreservingCancellation { api.organizations() }
                    }
                    val gitignoreDeferred = async {
                        runCatchingPreservingCancellation { api.gitignoreTemplates() }
                    }
                    val licensesDeferred = async {
                        runCatchingPreservingCancellation { api.licenses() }
                    }
                    val organizations = organizationsDeferred.await()
                    val gitignoreTemplates = gitignoreDeferred.await()
                    val licenses = licensesDeferred.await()
                    OptionsResult(
                        user = userDeferred.await(),
                        organizations = organizations.getOrDefault(emptyList()),
                        gitignoreTemplates = gitignoreTemplates.getOrDefault(emptyList()),
                        licenses = licenses.getOrDefault(emptyList()),
                        optionalRequestFailed = organizations.isFailure ||
                            gitignoreTemplates.isFailure || licenses.isFailure
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
                        error = repositoryCreationError(
                            error = error,
                            privateRepository = false,
                            organization = false,
                            operation = RepositoryCreationOperation.LoadOptions
                        )
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
                    val renameResult = runCatchingPreservingCancellation {
                        api.renameBranch(
                            owner = created.owner.login,
                            repository = created.name,
                            branch = created.defaultBranch,
                            request = RenameBranchRequest(requestedBranch)
                        )
                    }
                    val renameResponse = renameResult.getOrNull()
                    if (renameResponse?.isSuccessful == true) {
                        result = created.copy(defaultBranch = requestedBranch)
                    } else {
                        warning = branchRenameWarning(
                            defaultBranch = created.defaultBranch,
                            failure = renameResult.exceptionOrNull()
                        )
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
                            error = error,
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
        val user: GitHubUser,
        val organizations: List<RepositoryOrganization>,
        val gitignoreTemplates: List<String>,
        val licenses: List<RepositoryLicenseTemplate>,
        val optionalRequestFailed: Boolean
    )
}

internal fun branchRenameWarning(defaultBranch: String, failure: Throwable?): String =
    if (failure is IOException) {
        "Repository created, but the network failed while renaming the default branch. It remains $defaultBranch."
    } else {
        "Repository created, but the default branch could not be renamed. It remains $defaultBranch."
    }

internal fun repositoryCreationError(
    error: Throwable,
    privateRepository: Boolean,
    organization: Boolean,
    operation: RepositoryCreationOperation = RepositoryCreationOperation.CreateRepository
): String = when (error) {
    is HttpException -> when (error.code()) {
        401 -> if (operation == RepositoryCreationOperation.LoadOptions) {
            "GitHub authorization expired. Sign in again to load repository creation options."
        } else {
            "GitHub authorization expired. Sign in again before creating a repository."
        }
        403 -> when {
            operation == RepositoryCreationOperation.LoadOptions ->
                "GitHub denied access to repository creation options. Reauthorize with read:user and read:org scopes."
            organization ->
                "GitHub denied repository creation. The OAuth token needs the repo scope and your organization role must allow repository creation."
            privateRepository ->
                "GitHub denied private repository creation. Reauthorize with the repo OAuth scope."
            else ->
                "GitHub denied repository creation. Reauthorize with the public_repo or repo OAuth scope."
        }
        404 -> when {
            operation == RepositoryCreationOperation.LoadOptions ->
                "Repository creation options are unavailable for this account."
            organization ->
                "The organization is unavailable or your membership does not permit repository creation."
            else -> "The selected owner account is unavailable."
        }
        422 -> if (operation == RepositoryCreationOperation.LoadOptions) {
            "GitHub rejected the repository option request."
        } else {
            "GitHub rejected this repository. The name may already exist or one of the selected templates is invalid."
        }
        429 -> "GitHub rate limiting is active. Try again after the rate-limit reset."
        else -> if (operation == RepositoryCreationOperation.LoadOptions) {
            "GitHub could not load repository creation options (HTTP ${error.code()})."
        } else {
            "GitHub could not create the repository (HTTP ${error.code()})."
        }
    }
    is IOException -> if (operation == RepositoryCreationOperation.LoadOptions) {
        "Network connection failed while loading repository creation options."
    } else {
        "Network connection failed while creating the repository."
    }
    else -> error.message?.takeIf(String::isNotBlank) ?: if (
        operation == RepositoryCreationOperation.LoadOptions
    ) {
        "Unable to load repository creation options."
    } else {
        "Unable to create the repository."
    }
}
