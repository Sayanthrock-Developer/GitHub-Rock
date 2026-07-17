package com.sayanthrock.githubrock.data.repository

import android.util.Base64
import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import com.sayanthrock.githubrock.data.local.RepositoryDao
import com.sayanthrock.githubrock.data.local.RepositoryEntity
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val api: GitHubRestApi,
    private val recentDao: RepositoryDao,
    private val artworkResolver: RepositoryArtworkResolver
) {
    suspend fun dashboard(): DashboardPayload = withContext(Dispatchers.IO) {
        coroutineScope {
            val profile = async { api.me() }
            val rate = async {
                runCatchingPreservingCancellation { api.rateLimit().rate }.getOrNull()
            }
            val repositories = async { api.repositories() }
            DashboardPayload(
                profile = profile.await(),
                rateLimit = rate.await(),
                repositories = artworkResolver.attach(repositories.await())
            )
        }
    }

    suspend fun publicRepositories(query: String): List<GitHubRepositoryModel> = withContext(Dispatchers.IO) {
        val repositories = api.searchRepositories(
            query.ifBlank { "android stars:>1000" },
            sort = "updated"
        ).items
        artworkResolver.attach(repositories)
    }

    suspend fun setRepositoryStarred(owner: String, repo: String, starred: Boolean): Boolean =
        if (starred) api.starRepository(owner, repo).isSuccessful else api.unstarRepository(owner, repo).isSuccessful

    suspend fun forkRepository(owner: String, repo: String) = api.forkRepository(owner, repo)

    suspend fun createDraftRelease(
        owner: String,
        repo: String,
        tag: String,
        name: String,
        body: String,
        prerelease: Boolean
    ): Release {
        check(BuildRunTracker.isSafeRef(tag)) { "Use a valid release tag" }
        return api.createRelease(
            owner,
            repo,
            CreateReleaseRequest(
                tag,
                name.takeIf { it.isNotBlank() },
                body.takeIf { it.isNotBlank() },
                draft = true,
                prerelease = prerelease
            )
        )
    }

    suspend fun deleteRelease(owner: String, repo: String, releaseId: Long): Boolean =
        api.deleteRelease(owner, repo, releaseId).isSuccessful

    suspend fun updateRelease(
        owner: String,
        repo: String,
        releaseId: Long,
        name: String,
        body: String,
        draft: Boolean,
        prerelease: Boolean
    ) = api.updateRelease(
        owner,
        repo,
        releaseId,
        UpdateReleaseRequest(name.takeIf { it.isNotBlank() }, body.takeIf { it.isNotBlank() }, draft, prerelease)
    )

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
    suspend fun branchProtected(owner: String, repo: String, branch: String): Boolean =
        api.branchProtection(owner, repo, branch).isSuccessful

    suspend fun issues(owner: String, repo: String): List<GitHubIssue> =
        api.issues(owner, repo).filter { it.pullRequest == null }

    suspend fun createIssue(owner: String, repo: String, title: String, body: String): GitHubIssue {
        check(title.isNotBlank()) { "An issue title is required" }
        return api.createIssue(owner, repo, CreateIssueRequest(title, body.takeIf { it.isNotBlank() }))
    }

    suspend fun updateIssueState(owner: String, repo: String, issueNumber: Int, state: String): GitHubIssue {
        val normalized = state.trim().lowercase(Locale.ROOT)
        check(normalized in ISSUE_STATES) { "Issue state must be open or closed" }
        return api.updateIssue(owner, repo, issueNumber, UpdateIssueRequest(state = normalized))
    }

    suspend fun updateIssueMetadata(
        owner: String,
        repo: String,
        issueNumber: Int,
        labels: List<String>,
        assignees: List<String>,
        milestone: Int?
    ) = api.updateIssue(
        owner,
        repo,
        issueNumber,
        UpdateIssueRequest(labels = labels, assignees = assignees, milestone = milestone)
    )

    suspend fun addIssueReaction(owner: String, repo: String, issueNumber: Int, content: String) =
        api.addIssueReaction(owner, repo, issueNumber, IssueReactionRequest(content))

    suspend fun pulls(owner: String, repo: String) = api.pullRequests(owner, repo)

    suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
        body: String
    ): PullRequest {
        check(title.isNotBlank()) { "A pull request title is required" }
        validateReviewBranches(base, head)
        return api.createPullRequest(owner, repo, PullRequestRequest(title, head, base, body.takeIf { it.isNotBlank() }))
    }

    suspend fun workflows(owner: String, repo: String) = api.workflows(owner, repo).workflows
    suspend fun runs(owner: String, repo: String) = api.workflowRuns(owner, repo).runs
    suspend fun runsForWorkflow(owner: String, repo: String, workflowId: Long) =
        api.workflowRunsForWorkflow(owner, repo, workflowId).runs
    suspend fun run(owner: String, repo: String, runId: Long) = api.workflowRun(owner, repo, runId)
    suspend fun releases(owner: String, repo: String) = api.releases(owner, repo)

    suspend fun dispatch(
        owner: String,
        repo: String,
        workflowId: Long,
        ref: String,
        inputs: Map<String, String>
    ): Boolean {
        check(BuildRunTracker.isSafeRef(ref)) { "Use a valid branch or tag" }
        return api.dispatchWorkflow(owner, repo, workflowId, WorkflowDispatchRequest(ref, inputs)).isSuccessful
    }

    suspend fun cancel(owner: String, repo: String, runId: Long): Boolean =
        api.cancelWorkflow(owner, repo, runId).isSuccessful

    suspend fun rerun(owner: String, repo: String, runId: Long): Boolean =
        api.rerunWorkflow(owner, repo, runId).isSuccessful

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
        validateFileOperationPaths(path, path)
        validateReviewBranches(baseBranch, featureBranch)
        check(commitMessage.isNotBlank()) { "A commit message is required" }
        check(pullTitle.isNotBlank()) { "A pull request title is required" }

        return withReviewBranch(owner, repo, baseBranch, featureBranch) {
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            check(
                api.commitFile(
                    owner,
                    repo,
                    path,
                    FileCommitRequest(commitMessage.trim(), encoded, featureBranch, currentSha)
                ).isSuccessful
            ) { "Unable to commit the file" }
            api.createPullRequest(
                owner,
                repo,
                PullRequestRequest(pullTitle.trim(), featureBranch, baseBranch, pullBody)
            )
        }
    }

    suspend fun renameOrMoveFileAndOpenPullRequest(
        owner: String,
        repo: String,
        sourcePath: String,
        destinationPath: String,
        sourceSha: String,
        content: String,
        baseBranch: String,
        featureBranch: String,
        commitMessage: String
    ): PullRequest {
        validateFileOperationPaths(sourcePath, destinationPath)
        validateReviewBranches(baseBranch, featureBranch)
        check(sourcePath != destinationPath) { "Choose a different destination path" }
        check(sourceSha.isNotBlank()) { "The source file version is unavailable" }
        check(commitMessage.isNotBlank()) { "A commit message is required" }

        return withReviewBranch(owner, repo, baseBranch, featureBranch) {
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            check(
                api.commitFile(
                    owner,
                    repo,
                    destinationPath,
                    FileCommitRequest(commitMessage.trim(), encoded, featureBranch)
                ).isSuccessful
            ) { "Unable to create the destination file" }
            check(
                api.deleteFile(
                    owner,
                    repo,
                    sourcePath,
                    FileDeleteRequest(commitMessage.trim(), featureBranch, sourceSha)
                ).isSuccessful
            ) { "Unable to remove the original file from the review branch" }
            api.createPullRequest(
                owner,
                repo,
                PullRequestRequest(
                    "Move $sourcePath to $destinationPath",
                    featureBranch,
                    baseBranch,
                    "Prepared in GitHub Rock on a new review branch. The default branch was not overwritten."
                )
            )
        }
    }

    suspend fun deleteFileAndOpenPullRequest(
        owner: String,
        repo: String,
        path: String,
        sha: String,
        baseBranch: String,
        featureBranch: String,
        commitMessage: String
    ): PullRequest {
        validateFileOperationPaths(path, path)
        validateReviewBranches(baseBranch, featureBranch)
        check(sha.isNotBlank()) { "The file version is unavailable" }
        check(commitMessage.isNotBlank()) { "A commit message is required" }

        return withReviewBranch(owner, repo, baseBranch, featureBranch) {
            check(
                api.deleteFile(
                    owner,
                    repo,
                    path,
                    FileDeleteRequest(commitMessage.trim(), featureBranch, sha)
                ).isSuccessful
            ) { "Unable to delete the file from the review branch" }
            api.createPullRequest(
                owner,
                repo,
                PullRequestRequest(
                    "Delete $path",
                    featureBranch,
                    baseBranch,
                    "Prepared in GitHub Rock on a new review branch. The default branch was not overwritten."
                )
            )
        }
    }

    suspend fun issueComments(owner: String, repo: String, issueNumber: Int) =
        api.issueComments(owner, repo, issueNumber)

    suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String): IssueComment {
        check(body.isNotBlank()) { "A comment cannot be empty" }
        return api.addIssueComment(owner, repo, issueNumber, mapOf("body" to body.trim()))
    }

    suspend fun pullReviews(owner: String, repo: String, pullNumber: Int) =
        api.pullReviews(owner, repo, pullNumber)

    suspend fun submitPullReview(
        owner: String,
        repo: String,
        pullNumber: Int,
        event: String,
        body: String
    ): PullRequestReview {
        val normalizedEvent = event.trim().uppercase(Locale.ROOT)
        check(normalizedEvent in REVIEW_EVENTS) { "Unsupported pull request review event" }
        if (normalizedEvent != "APPROVE") check(body.isNotBlank()) { "A review comment is required" }
        return api.submitPullReview(owner, repo, pullNumber, ReviewRequest(body.trim(), normalizedEvent))
    }

    suspend fun mergePullRequest(
        owner: String,
        repo: String,
        pullNumber: Int,
        method: String
    ): MergeResponse {
        val normalizedMethod = method.trim().lowercase(Locale.ROOT)
        check(normalizedMethod in MERGE_METHODS) { "Use merge, squash, or rebase" }
        return api.mergePullRequest(owner, repo, pullNumber, mapOf("merge_method" to normalizedMethod))
    }

    suspend fun workflowJobs(owner: String, repo: String, runId: Long) =
        api.workflowJobs(owner, repo, runId).jobs

    suspend fun workflowArtifacts(owner: String, repo: String, runId: Long) =
        api.workflowArtifacts(owner, repo, runId).artifacts

    suspend fun workflowJobLogs(owner: String, repo: String, jobId: Long): String {
        val response = api.workflowJobLogs(owner, repo, jobId)
        check(response.isSuccessful) { "Unable to load workflow logs (HTTP ${response.code()})" }
        val body = response.body() ?: error("GitHub returned an empty workflow log response")
        return body.use { it.string() }
    }

    private suspend fun <T> withReviewBranch(
        owner: String,
        repo: String,
        baseBranch: String,
        featureBranch: String,
        operation: suspend () -> T
    ): T {
        val baseSha = api.branchReference(owner, repo, baseBranch).target.sha
        check(api.createBranch(owner, repo, GitRefRequest("refs/heads/$featureBranch", baseSha)).isSuccessful) {
            "Unable to create the review branch"
        }

        var completed = false
        return try {
            operation().also { completed = true }
        } finally {
            if (!completed) cleanupReviewBranch(owner, repo, featureBranch)
        }
    }

    private suspend fun cleanupReviewBranch(owner: String, repo: String, featureBranch: String) {
        withContext(NonCancellable) {
            try {
                api.deleteBranch(owner, repo, featureBranch)
            } catch (_: Exception) {
                // Preserve the original operation failure; cleanup is best effort.
            }
        }
    }

    private fun validateFileOperationPaths(sourcePath: String, destinationPath: String) {
        check(isSafeRepositoryPath(sourcePath) && isSafeRepositoryPath(destinationPath)) {
            "Use valid relative file paths"
        }
    }

    private fun validateReviewBranches(baseBranch: String, featureBranch: String) {
        check(BuildRunTracker.isSafeRef(baseBranch)) { "Unsafe base branch name" }
        check(BuildRunTracker.isSafeRef(featureBranch)) { "Unsafe review branch name" }
        check(baseBranch != featureBranch) { "The review branch must differ from the base branch" }
    }

    private fun isSafeRepositoryPath(path: String): Boolean =
        path.matches(Regex("^[A-Za-z0-9._/-]+$")) &&
            !path.startsWith('/') &&
            !path.endsWith('/') &&
            !path.contains("..") &&
            !path.contains("//")

    private companion object {
        val ISSUE_STATES = setOf("open", "closed")
        val REVIEW_EVENTS = setOf("APPROVE", "REQUEST_CHANGES", "COMMENT")
        val MERGE_METHODS = setOf("merge", "squash", "rebase")
    }
}

data class DashboardPayload(
    val profile: GitHubUser,
    val rateLimit: RateLimit?,
    val repositories: List<GitHubRepositoryModel>
)
