package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Owns GitHub network access for the profile library. */
@Singleton
class ProfileLibraryRepository @Inject constructor(
    private val api: GitHubRestApi
) {
    suspend fun starredRepositories(): List<GitHubRepositoryModel> =
        withContext(Dispatchers.IO) { api.starredRepositories() }

    suspend fun repository(owner: String, name: String): GitHubRepositoryModel? =
        withContext(Dispatchers.IO) {
            runCatching { api.repository(owner, name) }.getOrNull()
        }
}