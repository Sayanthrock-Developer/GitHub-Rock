package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.data.local.RepositoryDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns persistence and mapping for the recently viewed repository list. */
@Singleton
class RecentRepositoriesRepository @Inject constructor(
    private val repositoryDao: RepositoryDao
) {
    fun observeRecent(limit: Int): Flow<List<GitHubRepositoryModel>> =
        repositoryDao.observeRecent(limit).map { entities ->
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
}
