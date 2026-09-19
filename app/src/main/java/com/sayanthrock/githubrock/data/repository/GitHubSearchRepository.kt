package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.CodeSearchItem
import com.sayanthrock.githubrock.core.model.CommitSearchItem
import com.sayanthrock.githubrock.core.model.GitHubIssue
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RepositorySearchPage(val repositories: List<GitHubRepositoryModel>, val totalCount: Int, val hasMore: Boolean)
data class OwnerSearchPage(val owners: List<GitHubUser>, val totalCount: Int, val hasMore: Boolean)
data class CodeSearchPage(val items: List<CodeSearchItem>, val totalCount: Int, val hasMore: Boolean)
data class IssueSearchPage(val issues: List<GitHubIssue>, val totalCount: Int, val hasMore: Boolean)
data class CommitSearchPage(val commits: List<CommitSearchItem>, val totalCount: Int, val hasMore: Boolean)

@Singleton
class GitHubSearchRepository @Inject constructor(private val api: GitHubRestApi) {
    companion object { private const val PAGE_SIZE = 30 }

    private fun hasMore(totalCount: Int, page: Int) = page * PAGE_SIZE < totalCount

    suspend fun repositories(query: String, page: Int, perPage: Int = PAGE_SIZE): RepositorySearchPage = withContext(Dispatchers.IO) {
        val response = api.searchRepositories(query, perPage = perPage, page = page)
        RepositorySearchPage(response.items, response.totalCount, page * perPage < response.totalCount)
    }

    suspend fun owners(query: String, page: Int, perPage: Int = PAGE_SIZE): OwnerSearchPage = withContext(Dispatchers.IO) {
        val response = api.searchUsers(query, perPage = perPage, page = page)
        OwnerSearchPage(response.items, response.totalCount, page * perPage < response.totalCount)
    }

    suspend fun code(query: String, page: Int, perPage: Int = PAGE_SIZE): CodeSearchPage = withContext(Dispatchers.IO) {
        val response = api.searchCode(query, perPage = perPage, page = page)
        CodeSearchPage(response.items, response.totalCount, page * perPage < response.totalCount)
    }

    suspend fun issues(query: String, page: Int, pullRequests: Boolean, perPage: Int = PAGE_SIZE): IssueSearchPage = withContext(Dispatchers.IO) {
        val qualifier = if (pullRequests) "is:pr" else "is:issue"
        val response = api.searchIssues("$query $qualifier", perPage = perPage, page = page)
        IssueSearchPage(response.items, response.totalCount, page * perPage < response.totalCount)
    }

    suspend fun commits(query: String, page: Int, perPage: Int = PAGE_SIZE): CommitSearchPage = withContext(Dispatchers.IO) {
        val response = api.searchCommits(query, perPage = perPage, page = page)
        CommitSearchPage(response.items, response.totalCount, page * perPage < response.totalCount)
    }

    suspend fun topics(query: String, page: Int, perPage: Int = PAGE_SIZE): RepositorySearchPage =
        repositories("topic:$query", page, perPage)

    suspend fun all(query: String, page: Int, perPage: Int = PAGE_SIZE): AllSearchPage = withContext(Dispatchers.IO) {
        coroutineScope {
            val repositories = async { api.searchRepositories(query, perPage = perPage, page = page) }
            val owners = async { api.searchUsers(query, perPage = perPage, page = page) }
            val code = async { api.searchCode(query, perPage = perPage, page = page) }
            val issues = async { api.searchIssues("$query is:issue", perPage = perPage, page = page) }
            val pullRequests = async { api.searchIssues("$query is:pr", perPage = perPage, page = page) }
            val commits = async { api.searchCommits(query, perPage = perPage, page = page) }
            val topics = async { api.searchRepositories("topic:$query", perPage = perPage, page = page) }
            val repo = repositories.await()
            val owner = owners.await()
            val codeResponse = code.await()
            val issueResponse = issues.await()
            val prResponse = pullRequests.await()
            val commitResponse = commits.await()
            val topicResponse = topics.await()
            AllSearchPage(
                RepositorySearchPage(repo.items, repo.totalCount, page * perPage < repo.totalCount),
                OwnerSearchPage(owner.items, owner.totalCount, page * perPage < owner.totalCount),
                CodeSearchPage(codeResponse.items, codeResponse.totalCount, page * perPage < codeResponse.totalCount),
                IssueSearchPage(issueResponse.items, issueResponse.totalCount, page * perPage < issueResponse.totalCount),
                IssueSearchPage(prResponse.items, prResponse.totalCount, page * perPage < prResponse.totalCount),
                CommitSearchPage(commitResponse.items, commitResponse.totalCount, page * perPage < commitResponse.totalCount),
                RepositorySearchPage(topicResponse.items, topicResponse.totalCount, page * perPage < topicResponse.totalCount)
            )
        }
    }
}

data class AllSearchPage(
    val repositories: RepositorySearchPage,
    val owners: OwnerSearchPage,
    val code: CodeSearchPage,
    val issues: IssueSearchPage,
    val pullRequests: IssueSearchPage,
    val commits: CommitSearchPage,
    val topics: RepositorySearchPage
)
