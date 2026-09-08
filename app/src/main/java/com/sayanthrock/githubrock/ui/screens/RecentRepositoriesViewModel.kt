package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.data.local.RepositoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Persisted recently viewed repositories. This includes public repositories that
 * are opened from Explore/search even when they are not part of the signed-in
 * account's repository list.
 */
@HiltViewModel
class RecentRepositoriesViewModel @Inject constructor(
    repositoryDao: RepositoryDao
) : ViewModel() {
    val repositories: StateFlow<List<GitHubRepositoryModel>> = repositoryDao
        .observeRecent(limit = MAX_RECENT)
        .map { entities ->
            entities.map { entity ->
                GitHubRepositoryModel(
                    id = entity.id,
                    name = entity.name,
                    fullName = entity.fullName,
                    owner = Owner(login = entity.owner),
                    description = entity.description,
                    private = entity.isPrivate,
                    updatedAt = entity.updatedAt,
                    language = entity.language,
                    stars = entity.stars
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        const val MAX_RECENT = 10
    }
}
