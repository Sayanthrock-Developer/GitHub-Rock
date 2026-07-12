package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.local.RepositoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val api: GitHubRestApi,
    private val recentDao: RepositoryDao
) {
    suspend fun dashboard(): DashboardPayload = withContext(Dispatchers.IO) {
        val profile = api.me()
        val rate = api.rateLimit().rate
        val repos = api.repositories()
        DashboardPayload(profile, rate, repos)
    }

    suspend fun publicRepositories(query: String): List<GitHubRepositoryModel> =
        api.searchRepositories(query.ifBlank { "android stars:>1000" }, sort = "updated").items

    suspend fun setRepositoryStarred(owner: String, repo: String, starred: Boolean): Boolean =
        if (starred) api.starRepository(owner, repo).isSuccessful else api.unstarRepository(owner, repo).isSuccessful

    suspend fun forkRepository(owner: String, repo: String) = api.forkRepository(owner, repo)
    suspend fun createDraftRelease(owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean) =
        api.createRelease(owner, repo, CreateReleaseRequest(tag, name.takeIf { it.isNotBlank() }, body.takeIf { it.isNotBlank() }, draft = true, prerelease = prerelease))
    suspend fun deleteRelease(owner: String, repo: String, releaseId: Long): Boolean = api.deleteRelease(owner, repo, releaseId).isSuccessful
    suspend fun updateRelease(owner: String, repo: String, releaseId: Long, name: String, body: String, draft: Boolean, prerelease: Boolean) =
        api.updateRelease(owner, repo, releaseId, UpdateReleaseRequest(name.takeIf { it.isNotBlank() }, body.takeIf { it.isNotBlank() }, draft, prerelease))

    suspend fun remember(repository: GitHubRepositoryModel) = recentDao.upsert(
        RepositoryEntity(
            id = repository.id,
            owner = repository.owner.login,
            name = repository.name,
            fullName = repository.fullName,
            description = repository.description,
            language = repository.language,
            stars = repository.stars,
            isPrivate = repository.private,
            updatedAt = repository.updatedAt
        )
    )

    suspend fun contents(owner: String, repo: String, path: String, ref: String?) = api.contents(owner, repo, path, ref)
    suspend fun file(owner: String, repo: String, path: String, ref: String?) = api.file(owner, repo, path, ref)
    suspend fun issues(owner: String, repo: String) = api.issues(owner, repo).filterNot { it.body?.contains("pull request", ignoreCase = true) == true }
    suspend fun createIssue(owner: String, repo: String, title: String, body: String) = api.createIssue(owner, repo, CreateIssueRequest(title, body.takeIf { it.isNotBlank() }))
    suspend fun updateIssueState(owner: String, repo: String, issueNumber: Int, state: String) = api.updateIssue(owner, repo, issueNumber, UpdateIssueRequest(state))
    suspend fun pulls(owner: String, repo: String) = api.pullRequests(owner, repo)
    suspend fun createPullRequest(owner: String, repo: String, title: String, head: String, base: String, body: String) =
        api.createPullRequest(owner, repo, PullRequestRequest(title, head, base, body.takeIf { it.isNotBlank() }))
    suspend fun workflows(owner: String, repo: String) = api.workflows(owner, repo).workflows
    suspend fun runs(owner: String, repo: String) = api.workflowRuns(owner, repo).runs
    suspend fun releases(owner: String, repo: String) = api.releases(owner, repo)

    suspend fun dispatch(owner: String, repo: String, workflowId: Long, ref: String, inputs: Map<String, String>): Boolean =
        api.dispatchWorkflow(owner, repo, workflowId, WorkflowDispatchRequest(ref, inputs)).isSuccessful

    suspend fun cancel(owner: String, repo: String, runId: Long): Boolean = api.cancelWorkflow(owner, repo, runId).isSuccessful
    suspend fun rerun(owner: String, repo: String, runId: Long): Boolean = api.rerunWorkflow(owner, repo, runId).isSuccessful

    suspend fun commitFileAndOpenPullRequest(
        owner: String,
        repo: String,
        path: String,
        content: String,
        currentSha: String?,
        baseBranch: String,
        featureBranch: String,
        commitMessage: String,
        pullTitle: String,
        pullBody: String
    ): PullRequest {
        check(featureBranch.matches(Regex("^[A-Za-z0-9._/-]+$"))) { "Unsafe branch name" }
        val baseSha = api.branchReference(owner, repo, baseBranch).target.sha
        check(api.createBranch(owner, repo, GitRefRequest("refs/heads/$featureBranch", baseSha)).isSuccessful) {
            "Unable to create the review branch"
        }
        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        check(api.commitFile(owner, repo, path, FileCommitRequest(commitMessage, encoded, featureBranch, currentSha)).isSuccessful) {
            "Unable to commit the file"
        }
        return api.createPullRequest(owner, repo, PullRequestRequest(pullTitle, featureBranch, baseBranch, pullBody))
    }

    suspend fun issueComments(owner: String, repo: String, issueNumber: Int) = api.issueComments(owner, repo, issueNumber)
    suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String) = api.addIssueComment(owner, repo, issueNumber, mapOf("body" to body))
    suspend fun pullReviews(owner: String, repo: String, pullNumber: Int) = api.pullReviews(owner, repo, pullNumber)
    suspend fun submitPullReview(owner: String, repo: String, pullNumber: Int, event: String, body: String) = api.submitPullReview(owner, repo, pullNumber, ReviewRequest(body, event))
    suspend fun mergePullRequest(owner: String, repo: String, pullNumber: Int, method: String): MergeResponse = api.mergePullRequest(owner, repo, pullNumber, mapOf("merge_method" to method))
    suspend fun workflowJobs(owner: String, repo: String, runId: Long) = api.workflowJobs(owner, repo, runId)["jobs"].orEmpty()
    suspend fun workflowArtifacts(owner: String, repo: String, runId: Long) = api.workflowArtifacts(owner, repo, runId)["artifacts"].orEmpty()
    suspend fun workflowJobLogs(owner: String, repo: String, jobId: Long): String = api.workflowJobLogs(owner, repo, jobId).body()?.string().orEmpty()
}

data class DashboardPayload(
    val profile: GitHubUser,
    val rateLimit: RateLimit,
    val repositories: List<GitHubRepositoryModel>
)
