package com.sayanthrock.githubrock.data.repository

import android.util.Base64
import com.sayanthrock.githubrock.core.model.GitRefRequest
import com.sayanthrock.githubrock.core.model.PullRequest
import com.sayanthrock.githubrock.core.model.PullRequestRequest
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class BulkUploadFile(val path: String, val bytes: ByteArray)

data class BulkUploadResult(
    val pullRequest: PullRequest,
    val committedFiles: List<String>,
    val failedFiles: Map<String, String>
)

@Singleton
class GitHubBulkUploadRepository @Inject constructor(
    private val api: GitHubRestApi
) {
    suspend fun uploadAll(
        owner: String,
        repo: String,
        files: List<BulkUploadFile>,
        baseBranch: String,
        featureBranch: String,
        commitMessage: String,
        pullTitle: String,
        pullBody: String
    ): BulkUploadResult = withContext(Dispatchers.IO) {
        validate(files, baseBranch, featureBranch, commitMessage, pullTitle)
        val baseCommit = api.branchReference(owner, repo, baseBranch).target.sha
        createReviewBranch(owner, repo, baseBranch, featureBranch, baseCommit)
        var completed = false
        try {
            val baseTree = treeSha(owner, repo, baseCommit)
            val blobs = files.map { file ->
                file.path to createBlob(owner, repo, file.bytes)
            }
            val tree = createTree(owner, repo, baseTree, blobs)
            val commit = createCommit(owner, repo, commitMessage, tree, baseCommit)
            updateBranch(owner, repo, featureBranch, commit)
            val pull = api.createPullRequest(
                owner,
                repo,
                PullRequestRequest(pullTitle.trim(), featureBranch, baseBranch, pullBody)
            )
            completed = true
            BulkUploadResult(pull, files.map { it.path }, emptyMap())
        } finally {
            if (!completed) cleanupBranch(owner, repo, featureBranch)
        }
    }

    suspend fun uploadIndividually(
        owner: String,
        repo: String,
        files: List<BulkUploadFile>,
        baseBranch: String,
        featureBranch: String,
        commitMessage: String,
        pullTitle: String,
        pullBody: String
    ): BulkUploadResult = withContext(Dispatchers.IO) {
        validate(files, baseBranch, featureBranch, commitMessage, pullTitle)
        val baseCommit = api.branchReference(owner, repo, baseBranch).target.sha
        createReviewBranch(owner, repo, baseBranch, featureBranch, baseCommit)
        var currentCommit = baseCommit
        val committed = mutableListOf<String>()
        val failures = linkedMapOf<String, String>()
        var completed = false
        try {
            for (file in files) {
                try {
                    val baseTree = treeSha(owner, repo, currentCommit)
                    val blob = createBlob(owner, repo, file.bytes)
                    val tree = createTree(owner, repo, baseTree, listOf(file.path to blob))
                    val commit = createCommit(owner, repo, "$commitMessage: ${file.path}", tree, currentCommit)
                    updateBranch(owner, repo, featureBranch, commit)
                    currentCommit = commit
                    committed += file.path
                } catch (error: Throwable) {
                    failures[file.path] = error.message ?: "Upload failed"
                }
            }
            check(committed.isNotEmpty()) { "No file could be uploaded" }
            val pull = api.createPullRequest(
                owner,
                repo,
                PullRequestRequest(pullTitle.trim(), featureBranch, baseBranch, pullBody)
            )
            completed = true
            BulkUploadResult(pull, committed, failures)
        } finally {
            if (!completed && committed.isEmpty()) cleanupBranch(owner, repo, featureBranch)
        }
    }

    private suspend fun createReviewBranch(owner: String, repo: String, baseBranch: String, featureBranch: String, baseCommit: String) {
        check(BuildRunTracker.isSafeRef(baseBranch)) { "Use a valid base branch" }
        check(BuildRunTracker.isSafeRef(featureBranch)) { "Use a valid review branch" }
        check(baseBranch != featureBranch) { "Source and base branches must be different" }
        check(api.createBranch(owner, repo, GitRefRequest("refs/heads/$featureBranch", baseCommit)).isSuccessful) {
            "Unable to create the review branch"
        }
    }

    private suspend fun createBlob(owner: String, repo: String, bytes: ByteArray): String {
        val response = api.createGitBlob(
            owner,
            repo,
            buildJsonObject {
                put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
                put("encoding", "base64")
            }
        )
        return response["sha"]?.jsonPrimitive?.content ?: error("GitHub did not return a blob SHA")
    }

    private suspend fun treeSha(owner: String, repo: String, commitSha: String): String {
        val commit = api.gitCommit(owner, repo, commitSha)
        return commit["tree"]?.jsonObject?.get("sha")?.jsonPrimitive?.content
            ?: error("GitHub did not return the base tree SHA")
    }

    private suspend fun createTree(owner: String, repo: String, baseTree: String, entries: List<Pair<String, String>>): String {
        val tree: JsonArray = buildJsonArray {
            entries.forEach { (path, sha) ->
                add(buildJsonObject {
                    put("path", path)
                    put("mode", "100644")
                    put("type", "blob")
                    put("sha", sha)
                })
            }
        }
        val response = api.createGitTree(
            owner,
            repo,
            buildJsonObject {
                put("base_tree", baseTree)
                put("tree", tree)
            }
        )
        return response["sha"]?.jsonPrimitive?.content ?: error("GitHub did not return a tree SHA")
    }

    private suspend fun createCommit(owner: String, repo: String, message: String, tree: String, parent: String): String {
        val response = api.createGitCommit(
            owner,
            repo,
            buildJsonObject {
                put("message", message.trim())
                put("tree", tree)
                put("parents", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(parent)) })
            }
        )
        return response["sha"]?.jsonPrimitive?.content ?: error("GitHub did not return a commit SHA")
    }

    private suspend fun updateBranch(owner: String, repo: String, branch: String, commit: String) {
        api.updateGitBranchRef(
            owner,
            repo,
            branch,
            buildJsonObject {
                put("sha", commit)
                put("force", false)
            }
        )
    }

    private suspend fun cleanupBranch(owner: String, repo: String, branch: String) {
        withContext(NonCancellable) {
            runCatchingPreservingCancellation { api.deleteBranch(owner, repo, branch) }
        }
    }

    private fun validate(files: List<BulkUploadFile>, baseBranch: String, featureBranch: String, commitMessage: String, pullTitle: String) {
        check(files.isNotEmpty()) { "Select at least one file" }
        check(BuildRunTracker.isSafeRef(baseBranch)) { "Use a valid base branch" }
        check(BuildRunTracker.isSafeRef(featureBranch)) { "Use a valid review branch" }
        check(baseBranch != featureBranch) { "Source and base branches must be different" }
        check(commitMessage.isNotBlank()) { "A commit message is required" }
        check(pullTitle.isNotBlank()) { "A pull request title is required" }
        val paths = files.map { it.path }
        check(paths.distinct().size == paths.size) { "Duplicate repository paths are not allowed" }
        files.forEach {
            check(it.bytes.isNotEmpty()) { "${it.path} is empty" }
            check(it.bytes.size <= MAX_FILE_BYTES) { "${it.path} exceeds the 20 MB upload limit" }
            check(isSafePath(it.path)) { "Invalid repository path: ${it.path}" }
        }
        check(files.sumOf { it.bytes.size.toLong() } <= MAX_TOTAL_BYTES) { "The selected files exceed the 100 MB total upload limit" }
    }

    private fun isSafePath(path: String): Boolean =
        path.matches(Regex("^[A-Za-z0-9._/-]+$")) &&
            !path.startsWith('/') && !path.endsWith('/') && !path.contains("..") && !path.contains("//")

    private companion object {
        const val MAX_FILE_BYTES = 20L * 1024L * 1024L
        const val MAX_TOTAL_BYTES = 100L * 1024L * 1024L
    }
}
