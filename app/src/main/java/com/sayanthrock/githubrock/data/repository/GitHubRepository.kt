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
    /**
     * Loads the authenticated user's profile, rate limit, and repositories for the dashboard.
     *
     * @return The dashboard data, including an optional rate limit and repositories with artwork attached.
     */
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

    /**
         * Sets or clears the starred status of a repository.
         *
         * @param owner The repository owner's username.
         * @param repo The repository name.
         * @param starred Whether the repository should be starred.
         * @return `true` if the request succeeds, `false` otherwise.
         */
        suspend fun setRepositoryStarred(owner: String, repo: String, starred: Boolean): Boolean =
        if (starred) api.starRepository(owner, repo).isSuccessful else api.unstarRepository(owner, repo).isSuccessful

    /**
 * Forks a repository for the authenticated user.
 *
 * @param owner The repository owner's username.
 * @param repo The repository name.
 * @return The created fork response.
 */
suspend fun forkRepository(owner: String, repo: String) = api.forkRepository(owner, repo)

    /**
     * Creates a draft release for a repository.
     *
     * @param owner The repository owner's username or organization name.
     * @param repo The repository name.
     * @param tag The release tag.
     * @param name The release name, or blank to omit it.
     * @param body The release description, or blank to omit it.
     * @param prerelease Whether the release is marked as a prerelease.
     * @return The created draft release.
     */
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

    /**
         * Deletes a release from a repository.
         *
         * @param owner The repository owner's username.
         * @param repo The repository name.
         * @param releaseId The ID of the release to delete.
         * @return `true` if the deletion succeeded, `false` otherwise.
         */
        suspend fun deleteRelease(owner: String, repo: String, releaseId: Long): Boolean =
        api.deleteRelease(owner, repo, releaseId).isSuccessful

    /**
     * Updates the metadata and publication status of a release.
     *
     * @param releaseId The identifier of the release to update.
     * @param name The release name; blank values are omitted.
     * @param body The release description; blank values are omitted.
     * @param draft Whether the release is a draft.
     * @param prerelease Whether the release is marked as a prerelease.
     */
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

    /**
     * Saves a repository to the local recent-repositories store.
     *
     * @param repository The repository to save.
     */
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
    /**
 * Retrieves a file from a repository at the specified reference.
 *
 * @param owner The repository owner's username or organization name.
 * @param repo The repository name.
 * @param path The path to the file within the repository.
 * @param ref The branch, tag, or commit reference, or null for the default reference.
 */
suspend fun file(owner: String, repo: String, path: String, ref: String?) = api.file(owner, repo, path, ref)
    /**
         * Checks whether branch protection is enabled for a repository branch.
         *
         * @param owner The repository owner's login.
         * @param repo The repository name.
         * @param branch The branch name.
         * @return `true` if the request succeeds, `false` otherwise.
         */
        suspend fun branchProtected(owner: String, repo: String, branch: String): Boolean =
        api.branchProtection(owner, repo, branch).isSuccessful

    /**
         * Retrieves issues for a repository, excluding pull requests.
         *
         * @param owner The repository owner's username.
         * @param repo The repository name.
         * @return The repository's issues.
         */
        suspend fun issues(owner: String, repo: String): List<GitHubIssue> =
        api.issues(owner, repo).filter { it.pullRequest == null }

    /**
     * Creates an issue in a repository.
     *
     * @param owner The repository owner's username.
     * @param repo The repository name.
     * @param title The issue title.
     * @param body The issue description.
     * @return The created issue.
     */
    suspend fun createIssue(owner: String, repo: String, title: String, body: String): GitHubIssue {
        check(title.isNotBlank()) { "An issue title is required" }
        return api.createIssue(owner, repo, CreateIssueRequest(title, body.takeIf { it.isNotBlank() }))
    }

    /**
     * Updates the state of an issue.
     *
     * @param state The desired issue state, either `open` or `closed`.
     * @return The updated issue.
     */
    suspend fun updateIssueState(owner: String, repo: String, issueNumber: Int, state: String): GitHubIssue {
        val normalized = state.trim().lowercase(Locale.ROOT)
        check(normalized in ISSUE_STATES) { "Issue state must be open or closed" }
        return api.updateIssue(owner, repo, issueNumber, UpdateIssueRequest(state = normalized))
    }

    /**
     * Updates the labels, assignees, and milestone for an issue.
     *
     * @param owner The repository owner's login.
     * @param repo The repository name.
     * @param issueNumber The issue number.
     * @param labels The labels to assign to the issue.
     * @param assignees The users to assign to the issue.
     * @param milestone The milestone ID to assign, or `null` to omit it.
     */
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

    /**
         * Adds a reaction to an issue.
         *
         * @param owner The repository owner's username.
         * @param repo The repository name.
         * @param issueNumber The issue number.
         * @param content The reaction content.
         */
        suspend fun addIssueReaction(owner: String, repo: String, issueNumber: Int, content: String) =
        api.addIssueReaction(owner, repo, issueNumber, IssueReactionRequest(content))

    /**
 * Retrieves pull requests for a repository.
 *
 * @param owner The repository owner's username or organization name.
 * @param repo The repository name.
 * @return The repository's pull requests.
 */
suspend fun pulls(owner: String, repo: String) = api.pullRequests(owner, repo)

    /**
     * Creates a pull request between two branches.
     *
     * @param head The branch containing the proposed changes.
     * @param base The branch that will receive the changes.
     * @param body The pull request description; blank text is omitted.
     * @return The created pull request.
     */
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

    /**
 * Retrieves the workflows for a repository.
 *
 * @param owner The repository owner's login or organization name.
 * @param repo The repository name.
 * @return The repository's workflows.
 */
suspend fun workflows(owner: String, repo: String) = api.workflows(owner, repo).workflows
    /**
 * Retrieves workflow runs for a repository.
 *
 * @param owner The repository owner's username or organization name.
 * @param repo The repository name.
 * @return The repository's workflow runs.
 */
suspend fun runs(owner: String, repo: String) = api.workflowRuns(owner, repo).runs
    /**
         * Retrieves workflow runs for a specific workflow.
         *
         * @param owner The repository owner's name.
         * @param repo The repository name.
         * @param workflowId The workflow identifier.
         * @return The workflow runs associated with the workflow.
         */
        suspend fun runsForWorkflow(owner: String, repo: String, workflowId: Long) =
        api.workflowRunsForWorkflow(owner, repo, workflowId).runs
    /**
 * Retrieves a workflow run.
 *
 * @param owner The repository owner's login or organization name.
 * @param repo The repository name.
 * @param runId The workflow run identifier.
 * @return The requested workflow run.
 */
suspend fun run(owner: String, repo: String, runId: Long) = api.workflowRun(owner, repo, runId)
    /**
 * Retrieves the releases for a repository.
 *
 * @param owner The repository owner's username or organization name.
 * @param repo The repository name.
 * @return The repository's releases.
 */
suspend fun releases(owner: String, repo: String) = api.releases(owner, repo)

    /**
     * Dispatches a workflow for the specified branch or tag.
     *
     * @param owner The repository owner's username or organization name.
     * @param repo The repository name.
     * @param workflowId The workflow identifier.
     * @param ref The branch or tag on which to run the workflow.
     * @param inputs The input values passed to the workflow.
     * @return `true` if the workflow dispatch request succeeds, `false` otherwise.
     */
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

    /**
         * Cancels a workflow run.
         *
         * @param owner The repository owner's login.
         * @param repo The repository name.
         * @param runId The workflow run identifier.
         * @return `true` if the cancellation request succeeds, `false` otherwise.
         */
        suspend fun cancel(owner: String, repo: String, runId: Long): Boolean =
        api.cancelWorkflow(owner, repo, runId).isSuccessful

    /**
         * Reruns a workflow run.
         *
         * @param owner The repository owner's login or organization name.
         * @param repo The repository name.
         * @param runId The workflow run identifier.
         * @return `true` if the rerun request succeeds, `false` otherwise.
         */
        suspend fun rerun(owner: String, repo: String, runId: Long): Boolean =
        api.rerunWorkflow(owner, repo, runId).isSuccessful

    /**
     * Commits a file on a review branch and opens a pull request targeting the base branch.
     *
     * @param path The repository-relative path of the file to commit.
     * @param currentSha The file's current commit SHA, if updating an existing file.
     * @param baseBranch The branch targeted by the pull request.
     * @param featureBranch The branch used for the file commit.
     * @param commitMessage The commit message.
     * @param pullTitle The pull request title.
     * @throws IllegalStateException If validation fails or the file commit is unsuccessful.
     * @return The created pull request.
     */
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

    /**
     * Moves a file to a new path and opens a pull request with the change.
     *
     * @param sourcePath The current file path.
     * @param destinationPath The new file path.
     * @param sourceSha The current version identifier of the source file.
     * @param content The content to write at the destination path.
     * @param baseBranch The branch targeted by the pull request.
     * @param featureBranch The branch containing the file move.
     * @param commitMessage The commit message for the move.
     * @return The created pull request.
     */
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

    /**
     * Deletes a file on a review branch and opens a pull request with the change.
     *
     * @param path The repository-relative path of the file to delete.
     * @param sha The current version identifier of the file.
     * @param baseBranch The branch targeted by the pull request.
     * @param featureBranch The temporary branch containing the deletion.
     * @param commitMessage The commit message for the deletion.
     * @return The created pull request.
     */
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

    /**
         * Retrieves comments for an issue.
         *
         * @param owner The repository owner's username.
         * @param repo The repository name.
         * @param issueNumber The issue number.
         */
        suspend fun issueComments(owner: String, repo: String, issueNumber: Int) =
        api.issueComments(owner, repo, issueNumber)

    /**
     * Adds a comment to an issue.
     *
     * @param owner The repository owner.
     * @param repo The repository name.
     * @param issueNumber The issue number.
     * @param body The comment text.
     * @return The created issue comment.
     */
    suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String): IssueComment {
        check(body.isNotBlank()) { "A comment cannot be empty" }
        return api.addIssueComment(owner, repo, issueNumber, mapOf("body" to body.trim()))
    }

    /**
         * Retrieves the reviews for a pull request.
         *
         * @param pullNumber The number of the pull request.
         * @return The pull request reviews.
         */
        suspend fun pullReviews(owner: String, repo: String, pullNumber: Int) =
        api.pullReviews(owner, repo, pullNumber)

    /**
     * Submits a review for a pull request.
     *
     * @param pullNumber The pull request number.
     * @param event The review event, such as `APPROVE`, `REQUEST_CHANGES`, or `COMMENT`.
     * @param body The review comment; required for events other than `APPROVE`.
     * @return The submitted pull request review.
     */
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

    /**
     * Merges a pull request using the specified merge strategy.
     *
     * @param owner The repository owner's username or organization name.
     * @param repo The repository name.
     * @param pullNumber The pull request number.
     * @param method The merge strategy: `merge`, `squash`, or `rebase`.
     * @return The merge response.
     */
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

    /**
         * Retrieves the jobs associated with a workflow run.
         *
         * @param owner The repository owner's login or organization name.
         * @param repo The repository name.
         * @param runId The workflow run identifier.
         * @return The workflow run's jobs.
         */
        suspend fun workflowJobs(owner: String, repo: String, runId: Long) =
        api.workflowJobs(owner, repo, runId).jobs

    /**
         * Retrieves artifacts produced by a workflow run.
         *
         * @param owner The repository owner's username or organization name.
         * @param repo The repository name.
         * @param runId The workflow run identifier.
         * @return The artifacts associated with the workflow run.
         */
        suspend fun workflowArtifacts(owner: String, repo: String, runId: Long) =
        api.workflowArtifacts(owner, repo, runId).artifacts

    /**
     * Retrieves the logs for a workflow job.
     *
     * @param owner The repository owner's username or organization name.
     * @param repo The repository name.
     * @param jobId The workflow job identifier.
     * @return The workflow job logs as a string.
     */
    suspend fun workflowJobLogs(owner: String, repo: String, jobId: Long): String {
        val response = api.workflowJobLogs(owner, repo, jobId)
        check(response.isSuccessful) { "Unable to load workflow logs (HTTP ${response.code()})" }
        val body = response.body() ?: error("GitHub returned an empty workflow log response")
        return body.use { it.string() }
    }

    /**
     * Executes an operation on a temporary review branch created from the base branch.
     *
     * The review branch is cleaned up if the operation does not complete successfully.
     *
     * @param owner The repository owner's username or organization.
     * @param repo The repository name.
     * @param baseBranch The branch from which to create the review branch.
     * @param featureBranch The temporary branch used for the operation.
     * @param operation The operation to execute on the review branch.
     * @return The result produced by the operation.
     */
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
                check(api.deleteBranch(owner, repo, featureBranch).isSuccessful) {
                    "Unable to clean up the failed review branch"
                }
            } catch (_: Exception) {
                // Preserve the original operation failure; cleanup is best effort.
            }
        }
    }

    /**
     * Validates the source and destination paths used in a file operation.
     *
     * @param sourcePath The source file path.
     * @param destinationPath The destination file path.
     * @throws IllegalStateException If either path is unsafe.
     */
    private fun validateFileOperationPaths(sourcePath: String, destinationPath: String) {
        check(isSafeRepositoryPath(sourcePath) && isSafeRepositoryPath(destinationPath)) {
            "Use valid relative file paths"
        }
    }

    /**
     * Validates branch names and ensures the review branch differs from the base branch.
     *
     * @param baseBranch The branch receiving the proposed changes.
     * @param featureBranch The branch used for the proposed changes.
     */
    private fun validateReviewBranches(baseBranch: String, featureBranch: String) {
        check(BuildRunTracker.isSafeRef(baseBranch)) { "Unsafe base branch name" }
        check(BuildRunTracker.isSafeRef(featureBranch)) { "Unsafe review branch name" }
        check(baseBranch != featureBranch) { "The review branch must differ from the base branch" }
    }

    /**
             * Determines whether a repository path meets the allowed format and traversal restrictions.
             *
             * @param path The repository-relative path to validate.
             * @return `true` if the path is safe, `false` otherwise.
             */
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
