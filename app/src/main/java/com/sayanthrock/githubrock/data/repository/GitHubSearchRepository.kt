package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.CodeSearchItem
import com.sayanthrock.githubrock.core.model.CommitSearchItem
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.IssueSearchItem
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RepositorySearchPage(val repositories: List<GitHubRepositoryModel>, val hasMore: Boolean)
data class OwnerSearchPage(val owners: List<GitHubUser>, val hasMore: Boolean)
data class CodeSearchPage(val items: List<CodeSearchItem>, val hasMore: Boolean)
data class IssueSearchPage(val items: List<IssueSearchItem>, val hasMore: Boolean)
data class CommitSearchPage(val items: List<CommitSearchItem>, val hasMore: Boolean)
data class UnifiedSearchPage(
    val repositories: RepositorySearchPage,
    val code: CodeSearchPage,
    val issues: IssueSearchPage,
    val pullRequests: IssueSearchPage,
    val owners: OwnerSearchPage,
    val commits: CommitSearchPage
) {
    val hasMore: Boolean
        get() = repositories.hasMore || code.hasMore || issues.hasMore || pullRequests.hasMore || owners.hasMore || commits.hasMore
}

@Singleton
class GitHubSearchRepository @Inject constructor(private val api: GitHubRestApi) {
    suspend fun repositories(query: String, page: Int, perPage: Int = 30): RepositorySearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchRepositories(query, perPage = perPage, page = page)
            RepositorySearchPage(response.items, page * perPage < response.totalCount)
        }

    suspend fun owners(query: String, page: Int, perPage: Int = 30): OwnerSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchUsers(query, perPage = perPage, page = page)
            OwnerSearchPage(response.items, page * perPage < response.totalCount)
        }

    suspend fun code(query: String, page: Int, perPage: Int = 30): CodeSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchCode(query, perPage = perPage, page = page)
            CodeSearchPage(response.items, page * perPage < response.totalCount)
        }

    suspend fun issues(query: String, page: Int, perPage: Int = 30): IssueSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchIssues(query, perPage = perPage, page = page)
            IssueSearchPage(response.items, page * perPage < response.totalCount)
        }

    suspend fun pullRequests(query: String, page: Int, perPage: Int = 30): IssueSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchIssues("$query type:pr", perPage = perPage, page = page)
            IssueSearchPage(response.items, response.items.size == perPage)
        }

    suspend fun commits(query: String, page: Int, perPage: Int = 30): CommitSearchPage =
        withContext(Dispatchers.IO) {
            val response = api.searchCommits(query, perPage = perPage, page = page)
            CommitSearchPage(response.items, page * perPage < response.totalCount)
        }

    suspend fun topics(query: String, page: Int, perPage: Int = 30): RepositorySearchPage =
        repositories("topic:$query", page, perPage)

    suspend fun all(query: String, page: Int, perPage: Int = 30): UnifiedSearchPage = withContext(Dispatchers.IO) {
        coroutineScope {
            val calls = listOf(
                async { repositories(query, page, perPage) },
                async { code(query, page, perPage) },
                async { issues(query, page, perPage) },
                async { pullRequests(query, page, perPage) },
                async { owners(query, page, perPage) },
                async { commits(query, page, perPage) }
            )
            val (repositories, code, issues, pullRequests, owners, commits) = calls.awaitAll()
            UnifiedSearchPage(
                repositories as RepositorySearchPage,
                code as CodeSearchPage,
                issues as IssueSearchPage,
                pullRequests as IssueSearchPage,
                owners as OwnerSearchPage,
                commits as CommitSearchPage
            )
        }
    }
}
