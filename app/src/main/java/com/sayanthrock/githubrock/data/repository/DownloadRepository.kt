package com.sayanthrock.githubrock.data.repository

import android.content.Context
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import com.sayanthrock.githubrock.core.util.ReleaseChecksumResolver
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadDao,
    @ApplicationContext context: Context,
    @Named("downloadClient") private val downloadClient: OkHttpClient,
    private val json: Json
) {
    private val applicationContext = context.applicationContext
    private val downloadsDirectory = File(applicationContext.filesDir, "downloads")
    private val enqueueMutex = Mutex()
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<Long, Job>()

    fun observeAll(): Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun enqueue(
        url: String,
        fileName: String,
        expectedPackage: String? = null,
        repositoryFullName: String? = null,
        releaseName: String? = null,
        releaseUrl: String? = null,
        assetId: Long? = null,
        expectedSha256: String? = null,
        fallbackUrl: String? = null,
        checksumUrl: String? = null
    ) = enqueueMutex.withLock {
        val requestedUrl = url.trim().takeIf(String::isNotBlank) ?: return@withLock
        val repositoryName = repositoryFullName?.trim()?.takeIf(String::isNotBlank)
        val resolvedAsset = resolveReleaseAsset(requestedUrl, fileName, repositoryName, assetId)
        val resolvedAssetId = assetId ?: resolvedAsset?.id
        val browserUrl = resolvedAsset?.browserDownloadUrl?.trim()?.takeIf(String::isNotBlank)
        val resolvedUrl = if (isPublicGitHubReleaseUrl(requestedUrl) && browserUrl != null) browserUrl else requestedUrl
        val resolvedChecksumUrl = checksumUrl?.trim()?.takeIf(String::isNotBlank)
            ?: resolveReleaseChecksumUrl(resolvedUrl, fileName, repositoryName, resolvedAssetId)

        val existing = findExistingDownload(dao, resolvedAssetId, resolvedUrl)
        if (existing != null) {
            if (existing.status in ACTIVE_STATES) return@withLock
            val existingFile = existing.localPath?.let(::File)
            if (existing.status in TERMINAL_STATES && existingFile?.isFile == true && existingFile.length() > 0L) {
                return@withLock
            }
            val resumed = existing.copy(
                fileName = fileName,
                sourceUrl = resolvedUrl,
                fallbackUrl = null,
                checksumUrl = resolvedChecksumUrl,
                status = DownloadState.QUEUED.wireValue,
                expectedSha256 = expectedSha256 ?: existing.expectedSha256,
                packageName = expectedPackage ?: existing.packageName,
                repositoryFullName = repositoryName ?: existing.repositoryFullName,
                releaseName = releaseName ?: existing.releaseName,
                releaseUrl = releaseUrl ?: existing.releaseUrl,
                assetId = resolvedAssetId ?: existing.assetId,
                errorMessage = null
            )
            dao.upsert(resumed)
            schedule(resumed)
            return@withLock
        }

        val queued = DownloadEntity(
            fileName = fileName,
            sourceUrl = resolvedUrl,
            fallbackUrl = null,
            checksumUrl = resolvedChecksumUrl,
            status = DownloadState.QUEUED.wireValue,
            expectedSha256 = expectedSha256,
            packageName = expectedPackage,
            repositoryFullName = repositoryName,
            releaseName = releaseName,
            releaseUrl = releaseUrl,
            assetId = resolvedAssetId
        )
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    suspend fun downloadAgain(download: DownloadEntity) {
        cancelJob(download.id)
        deleteOwnedFile(download.localPath)
        val queued = download.copy(
            localPath = null,
            totalBytes = 0,
            downloadedBytes = 0,
            sha256 = null,
            status = DownloadState.QUEUED.wireValue,
            speedBytesPerSecond = 0,
            etaSeconds = null,
            errorMessage = null
        )
        dao.upsert(queued)
        schedule(queued)
    }

    suspend fun pause(download: DownloadEntity) {
        if (download.status !in ACTIVE_STATES) return
        cancelJob(download.id)
        dao.updateProgress(download.id, DownloadState.PAUSED.wireValue, download.downloadedBytes,
            download.totalBytes, download.localPath, download.sha256, download.speedBytesPerSecond,
            download.etaSeconds, null)
    }

    suspend fun resume(download: DownloadEntity) {
        if (download.status !in RESUMABLE_STATES) return
        val queued = download.copy(status = DownloadState.QUEUED.wireValue, errorMessage = null)
        dao.upsert(queued)
        schedule(queued)
    }

    suspend fun cancel(download: DownloadEntity) {
        if (download.status !in ACTIVE_STATES && download.status != DownloadState.PAUSED.wireValue) return
        cancelJob(download.id)
        deleteOwnedFile(download.localPath)
        dao.updateProgress(download.id, DownloadState.CANCELLED.wireValue, 0, 0, null, null, 0, null, null)
    }

    suspend fun delete(download: DownloadEntity) {
        cancelJob(download.id)
        deleteOwnedFile(download.localPath)
        dao.delete(download.id)
    }

    suspend fun inspectApk(file: File): Result<ApkInspection> = withContext(Dispatchers.IO) {
        runCatching {
            val firstPass = inspectApk(applicationContext, file)
            val previous = dao.latestCompletedForPackage(firstPass.packageName)
            inspectApk(applicationContext, file, expectedPackage = firstPass.packageName,
                previousVersionCode = previous?.versionCode,
                previousPermissions = previous?.permissions?.split("\n")?.filter(String::isNotBlank).orEmpty(),
                previousCertificateSha256 = previous?.certificateSha256)
        }
    }

    suspend fun recoverInvalidCompletedDownloads(items: List<DownloadEntity>) {
        items.filter { it.status == DownloadState.COMPLETED.wireValue || it.status == DownloadState.INSTALLABLE.wireValue }
            .forEach { download ->
                val file = download.localPath?.let(::File)
                val valid = file?.takeIf(File::isFile)?.let { candidate ->
                    if (download.isApkDownload()) {
                        withContext(Dispatchers.IO) { runCatching { inspectApk(applicationContext, candidate) }.isSuccess }
                    } else candidate.length() > 0L
                } == true
                if (!valid) downloadAgain(download)
            }
    }

    suspend fun updateProgress(
        id: Long,
        state: DownloadState,
        downloaded: Long,
        total: Long,
        path: String?,
        sha: String?,
        speedBytesPerSecond: Long = 0,
        etaSeconds: Long? = null,
        errorMessage: String? = null
    ) = dao.updateProgress(id, state.wireValue, downloaded, total, path, sha,
        speedBytesPerSecond.coerceAtLeast(0), etaSeconds?.coerceAtLeast(0), errorMessage)

    suspend fun updateSecurity(
        id: Long, packageName: String, versionCode: Long, versionName: String?, minSdk: Int, targetSdk: Int,
        permissions: String, certificateSha256: String?, signatureSchemes: String, architectures: String,
        securityRisk: String, securityReasons: String
    ) = dao.updateSecurity(id, packageName, versionCode, versionName, minSdk, targetSdk, permissions,
        certificateSha256, signatureSchemes, architectures, securityRisk, securityReasons)

    suspend fun latestCompletedForPackage(packageName: String): DownloadEntity? = dao.latestCompletedForPackage(packageName)

    fun schedule(download: DownloadEntity) {
        cancelJob(download.id)
        jobs[download.id] = downloadScope.launch {
            downloadDirect(download)
        }.also { job -> job.invokeOnCompletion { jobs.remove(download.id, job) } }
    }

    private suspend fun downloadDirect(download: DownloadEntity) {
        val name = download.fileName.safeFileName().takeIf(String::isNotBlank) ?: throw IllegalArgumentException("Invalid file name")
        downloadsDirectory.mkdirs()
        val partial = File(downloadsDirectory, "${download.id}-$name.part")
        val final = File(downloadsDirectory, "${download.id}-$name")
        var knownTotal = 0L
        try {
            val existing = partial.takeIf(File::isFile)?.length() ?: 0L
            if (existing > 0L) partial.delete()

            val request = Request.Builder()
                .url(download.sourceUrl)
                .header("User-Agent", "GitHub-Rock/1.0")
                .header("Accept", "application/octet-stream")
                .header("Accept-Encoding", "identity")
                .build()

            download.updateQueuedPath(partial)
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed: HTTP ${response.code}")
                val body = response.body ?: error("Empty download response")
                val contentType = body.contentType()?.toString()?.lowercase().orEmpty()
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    error("GitHub returned a non-binary response ($contentType)")
                }
                if (name.endsWith(".apk", true) && contentType.contains("text/plain")) {
                    error("GitHub returned text instead of the APK binary")
                }
                knownTotal = body.contentLength().takeIf { it >= 0L } ?: 0L
                updateProgress(download.id, DownloadState.DOWNLOADING, 0L, knownTotal, partial.absolutePath, download.expectedSha256)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                var lastPublished = 0L
                val startedAt = System.nanoTime()
                body.byteStream().use { input ->
                    partial.outputStream().buffered().use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            if (downloaded - lastPublished >= 256 * 1024L) {
                                val elapsed = (System.nanoTime() - startedAt) / 1_000_000_000.0
                                val speed = if (elapsed > 0) (downloaded / elapsed).toLong() else 0L
                                val eta = if (speed > 0 && knownTotal > downloaded) (knownTotal - downloaded + speed - 1) / speed else null
                                updateProgress(download.id, DownloadState.DOWNLOADING, downloaded, knownTotal, partial.absolutePath, download.expectedSha256, speed, eta)
                                lastPublished = downloaded
                            }
                        }
                    }
                }
                if (knownTotal > 0L && partial.length() != knownTotal) error("Download size mismatch: ${partial.length()} of $knownTotal bytes")
            }

            currentCoroutineContext().ensureActive()
            if (!partial.isFile || partial.length() <= 0L) error("Downloaded file is empty")
            updateProgress(download.id, DownloadState.VERIFYING, partial.length(), knownTotal, partial.absolutePath, download.expectedSha256)
            val sha = sha256(partial)
            val expected = download.expectedSha256?.trim()?.takeIf(String::isNotBlank)
            if (expected != null && !ChecksumVerifier.matches(sha, expected)) error("SHA-256 verification failed")

            if (name.endsWith(".apk", true)) {
                val inspection = inspectApk(applicationContext, partial, expectedPackage = download.packageName)
                updateSecurity(download.id, inspection.packageName, inspection.versionCode, inspection.versionName,
                    inspection.minSdk, inspection.targetSdk, inspection.permissions.joinToString("\n"),
                    inspection.certificateSha256, inspection.signatureSchemes.joinToString(","),
                    inspection.architectures.joinToString(","), if (inspection.riskReasons.isEmpty()) "low" else "review",
                    inspection.riskReasons.joinToString("\n"))
            }

            if (final.exists()) final.delete()
            check(partial.renameTo(final)) { "Unable to finalize download" }
            check(final.isFile && final.length() == partialLengthOrFinal(final)) { "Final download file is unavailable" }
            val finalSha = sha256(final)
            check(finalSha.equals(sha, ignoreCase = true)) { "Final download bytes changed" }
            if (expected != null) check(ChecksumVerifier.matches(finalSha, expected)) { "Final SHA-256 verification failed" }
            if (name.endsWith(".apk", true)) {
                val finalInspection = inspectApk(applicationContext, final, expectedPackage = download.packageName)
                check(finalInspection.packageName.isNotBlank()) { "Final APK package name is missing" }
            }
            val state = if (name.endsWith(".apk", true)) DownloadState.INSTALLABLE else DownloadState.COMPLETED
            updateProgress(download.id, state, final.length(), final.length(), final.absolutePath, finalSha, 0L, 0L, null)
        } catch (cancelled: CancellationException) {
            updateProgress(download.id, DownloadState.PAUSED.wireValueState(), partial.lengthSafe(), knownTotal, partial.takeIf(File::isFile)?.absolutePath, download.expectedSha256, 0L, null, null)
            throw cancelled
        } catch (error: Exception) {
            partial.delete()
            updateProgress(download.id, DownloadState.FAILED, 0L, knownTotal, null, download.expectedSha256, 0L, null, error.message ?: "Download failed")
        }
    }

    private fun DownloadEntity.updateQueuedPath(partial: File) {
        downloadScope.launch { updateProgress(id, DownloadState.QUEUED, 0L, 0L, partial.absolutePath, expectedSha256) }
    }

    private fun partialLengthOrFinal(file: File): Long = file.length()
    private fun File.lengthSafe(): Long = if (isFile) length() else 0L
    private fun DownloadState.wireValueState(): DownloadState = this

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun resolveReleaseAsset(sourceUrl: String, fileName: String, repositoryFullName: String?, assetId: Long?): ReleaseAsset? = withContext(Dispatchers.IO) {
        runCatching {
            val release = releaseFromDownloadUrl(sourceUrl, repositoryFullName, assetId) ?: return@runCatching null
            release.assets.firstOrNull { asset -> (assetId != null && asset.id == assetId) || asset.name == fileName || asset.downloadUrl == sourceUrl || asset.browserDownloadUrl == sourceUrl }
        }.getOrNull()
    }

    private suspend fun resolveReleaseChecksumUrl(sourceUrl: String, fileName: String, repositoryFullName: String?, assetId: Long?): String? = withContext(Dispatchers.IO) {
        runCatching {
            val release = releaseFromDownloadUrl(sourceUrl, repositoryFullName, assetId) ?: return@runCatching null
            val target = release.assets.firstOrNull { it.id == assetId || it.name == fileName || it.downloadUrl == sourceUrl || it.browserDownloadUrl == sourceUrl } ?: return@runCatching null
            if (!target.name.endsWith(".apk", true) && !target.name.endsWith(".aab", true)) return@runCatching null
            ReleaseChecksumResolver.findFor(target, release.assets)?.browserDownloadUrl?.takeIf(String::isNotBlank)
        }.getOrNull()
    }

    private fun releaseFromDownloadUrl(sourceUrl: String, repositoryFullName: String?, assetId: Long?): Release? {
        val uri = URI(sourceUrl)
        val path = uri.path.orEmpty()
        val segments = path.split('/').filter(String::isNotBlank)
        val repoIndex = segments.indexOf("repos")
        val repositoryName = repositoryFullName?.trim()?.takeIf(String::isNotBlank)
        val owner = when {
            repoIndex >= 0 && segments.size > repoIndex + 2 -> segments[repoIndex + 1]
            repositoryName != null -> repositoryName.substringBefore('/')
            else -> null
        }
        val repo = when {
            repoIndex >= 0 && segments.size > repoIndex + 2 -> segments[repoIndex + 2]
            repositoryName != null -> repositoryName.substringAfter('/', "")
            else -> null
        }
        if (owner.isNullOrBlank() || repo.isNullOrBlank()) return null
        val tagIndex = segments.indexOf("download")
        if (tagIndex >= 0 && segments.size > tagIndex + 1) {
            val tag = tagSegment(segments[tagIndex + 1])
            return fetchRelease("https://api.github.com/repos/$owner/$repo/releases/tags/$tag")
        }
        if (assetId != null || path.contains("/releases/assets/")) {
            val request = Request.Builder().url("https://api.github.com/repos/$owner/$repo/releases?per_page=100")
                .header("Accept", "application/vnd.github+json").header("User-Agent", "GitHub-Rock/1.0").build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val releases = json.decodeFromString<List<Release>>(body)
                return releases.firstOrNull { release -> release.assets.any { it.id == assetId || it.downloadUrl == sourceUrl } }
            }
        }
        return null
    }

    private fun fetchRelease(url: String): Release? {
        val request = Request.Builder().url(url).header("Accept", "application/vnd.github+json").header("User-Agent", "GitHub-Rock/1.0").build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return json.decodeFromString<Release>(response.body?.string() ?: return null)
        }
    }

    private fun tagSegment(tag: String): String = java.net.URLEncoder.encode(tag, Charsets.UTF_8.name()).replace("+", "%20")
    private fun cancelJob(id: Long) { jobs.remove(id)?.cancel() }
    private fun deleteOwnedFile(path: String?) { path?.let(::File)?.takeIf { it.parentFile?.canonicalFile == downloadsDirectory.canonicalFile }?.delete() }
    private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)
    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun isPublicGitHubReleaseUrl(url: String): Boolean = runCatching { val uri = URI(url); uri.host.equals("github.com", true) && uri.path?.contains("/releases/download/", true) == true }.getOrDefault(false)

    companion object {
        private val ACTIVE_STATES = setOf(DownloadState.QUEUED.wireValue, DownloadState.DOWNLOADING.wireValue, DownloadState.RETRYING.wireValue)
        private val RESUMABLE_STATES = setOf(DownloadState.PAUSED.wireValue, DownloadState.FAILED.wireValue, DownloadState.CANCELLED.wireValue)
        private val TERMINAL_STATES = setOf(DownloadState.COMPLETED.wireValue, DownloadState.INSTALLABLE.wireValue)
    }
}

internal suspend fun findExistingDownload(dao: DownloadDao, resolvedAssetId: Long?, resolvedUrl: String): DownloadEntity? = resolvedAssetId?.let { dao.findByAssetId(it) } ?: dao.findBySourceUrl(resolvedUrl)
