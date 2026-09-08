package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubIssue
import com.sayanthrock.githubrock.core.model.IssueComment
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubIssuesRepository @Inject constructor(
    private val api: GitHubRestApi
) {
    suspend fun issues(owner: String, repo: String, state: String): List<GitHubIssue> =
        withContext(Dispatchers.IO) {
            api.issues(owner, repo, state = state, perPage = 100)
                .filter { it.pullRequest == null }
        }

    suspend fun comments(owner: String, repo: String, issueNumber: Int): List<IssueComment> =
        withContext(Dispatchers.IO) {
            api.issueComments(owner, repo, issueNumber)
        }
}
