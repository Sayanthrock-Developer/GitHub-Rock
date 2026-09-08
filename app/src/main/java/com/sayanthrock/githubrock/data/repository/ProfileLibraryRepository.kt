package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Owns profile-library network access and local persistence/settings boundaries. */
@Singleton
class ProfileLibraryRepository @Inject constructor(
    private val api: GitHubRestApi,
    private val repositoryDao: RepositoryDao,
    private val preferences: AppPreferences
) {
    suspend fun starredRepositories(): List<GitHubRepositoryModel> =
        withContext(Dispatchers.IO) { api.starredRepositories() }

    suspend fun repository(owner: String, name: String): GitHubRepositoryModel? =
        withContext(Dispatchers.IO) { runCatching { api.repository(owner, name) }.getOrNull() }

    suspend fun recentlyViewed(limit: Int = 100): List<GitHubRepositoryModel> =
        withContext(Dispatchers.IO) { repositoryDao.recent(limit).map(RepositoryEntityMapper::toModel) }

    val favouriteKeys: Flow<Set<String>> = preferences.favoriteRepositories

    suspend fun toggleFavourite(fullName: String) {
        preferences.toggleFavoriteRepository(fullName)
    }

    private object RepositoryEntityMapper {
        fun toModel(entity: com.sayanthrock.githubrock.data.local.RepositoryEntity) = GitHubRepositoryModel(
            id = entity.id,
            name = entity.name,
            fullName = entity.fullName,
            owner = Owner(login = entity.owner),
            description = entity.description,
            private = entity.isPrivate,
            htmlUrl = "https://github.com/${entity.fullName}",
            language = entity.language,
            stars = entity.stars,
            updatedAt = entity.updatedAt
        )
    }
}
