package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RepositorySearchPage(
    val repositories: List<GitHubRepositoryModel>,
    val hasMore: Boolean
)

data class OwnerSearchPage(
    val owners: List<GitHubUser>,
    val hasMore: Boolean
)

@Singleton
class GitHubSearchRepository @Inject constructor(
    private val api: GitHubRestApi
) {
    suspend fun repositories(query: String, page: Int, perPage: Int = 30): RepositorySearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchRepositories(query, perPage = perPage, page = page)
            RepositorySearchPage(response.items, response.items.size == perPage)
        }

    suspend fun owners(query: String, page: Int, perPage: Int = 30): OwnerSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchUsers(query, perPage = perPage, page = page)
            OwnerSearchPage(response.items, response.items.size == perPage)
        }

    suspend fun topics(query: String, page: Int, perPage: Int = 30): RepositorySearchPage =
        repositories("topic:$query", page, perPage)

    suspend fun all(query: String, page: Int, perPage: Int = 30): Pair<RepositorySearchPage, OwnerSearchPage> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val repositories = async { api.searchRepositories(query, perPage = perPage, page = page) }
                val owners = async { api.searchUsers(query, perPage = perPage, page = page) }
                val repoResponse = repositories.await()
                val ownerResponse = owners.await()
                RepositorySearchPage(repoResponse.items, repoResponse.items.size == perPage) to
                    OwnerSearchPage(ownerResponse.items, ownerResponse.items.size == perPage)
            }
        }
}
