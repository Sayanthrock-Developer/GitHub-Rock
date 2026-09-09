package com.sayanthrock.githubrock.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.ReleaseChecksumResolver
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.DownloadState
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val workManager = WorkManager.getInstance(applicationContext)
    private val downloadsDirectory = File(applicationContext.filesDir, "downloads")

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
    ) {
        val requestedUrl = url.trim().takeIf(String::isNotBlank) ?: return
        val repositoryName = repositoryFullName?.trim()?.takeIf(String::isNotBlank)
        val resolvedAsset = resolveReleaseAsset(requestedUrl, fileName, repositoryName, assetId)
        val publicRelease = isPublicGitHubReleaseUrl(requestedUrl)
        val browserUrl = resolvedAsset?.browserDownloadUrl?.trim()?.takeIf(String::isNotBlank)
        val apiAssetUrl = resolvedAsset?.downloadUrl?.trim()?.takeIf(String::isNotBlank)
            ?: assetId?.takeIf { repositoryName != null }?.let { id ->
                "https://api.github.com/repos/$repositoryName/releases/assets/$id"
            }
        val resolvedUrl = if (publicRelease && browserUrl != null) browserUrl else requestedUrl
        val suppliedFallback = fallbackUrl?.trim()?.takeIf(String::isNotBlank)
        val derivedFallback = when {
            suppliedFallback != null && suppliedFallback != resolvedUrl -> suppliedFallback
            resolvedUrl != browserUrl && browserUrl != null -> browserUrl
            apiAssetUrl != null && apiAssetUrl != resolvedUrl -> apiAssetUrl
            else -> null
        }
        val resolvedFallbackUrl = derivedFallback?.takeIf { it.isNotBlank() && it != resolvedUrl }
        val resolvedChecksumUrl = checksumUrl?.trim()?.takeIf(String::isNotBlank)
            ?: resolveReleaseChecksumUrl(resolvedUrl, fileName, repositoryName, assetId)

        // A repeated tap for the same release asset must reuse its existing
        // persistent download instead of creating another row/work request.
        val existing = dao.findExisting(resolvedUrl, assetId)
        if (existing != null) {
            val existingFile = existing.localPath?.let(::File)
            val fileAvailable = existingFile?.isFile == true && existingFile.length() > 0L
            when {
                existing.status == DownloadState.PAUSED.wireValue -> {
                    resume(existing)
                    return
                }
                existing.status in ACTIVE_STATES -> return
                existing.status in COMPLETED_STATES && fileAvailable -> return
                existing.status in COMPLETED_STATES && !fileAvailable -> {
                    downloadAgain(existing)
                    return
                }
            }
        }

        val queued = DownloadEntity(
            fileName = fileName,
            sourceUrl = resolvedUrl,
            fallbackUrl = resolvedFallbackUrl,
            checksumUrl = resolvedChecksumUrl,
            status = DownloadState.QUEUED.wireValue,
            expectedSha256 = expectedSha256,
            packageName = expectedPackage,
            repositoryFullName = repositoryName,
            releaseName = releaseName,
            releaseUrl = releaseUrl,
            assetId = assetId
        )
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    suspend fun downloadAgain(download: DownloadEntity) {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
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
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
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
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteOwnedFile(download.localPath)
        dao.updateProgress(download.id, DownloadState.CANCELLED.wireValue, 0, 0, null, null, 0, null, null)
    }

    suspend fun delete(download: DownloadEntity) {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
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
                        withContext(Dispatchers.IO) {
                            runCatching { inspectApk(applicationContext, candidate) }.isSuccess
                        }
                    } else {
                        candidate.length() > 0L
                    }
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

    private suspend fun resolveReleaseAsset(
        sourceUrl: String,
        fileName: String,
        repositoryFullName: String?,
        assetId: Long?
    ): ReleaseAsset? = withContext(Dispatchers.IO) {
        runCatching {
            val release = releaseFromDownloadUrl(sourceUrl, repositoryFullName, assetId) ?: return@runCatching null
            release.assets.firstOrNull { asset ->
                (assetId != null && asset.id == assetId) ||
                    asset.name == fileName ||
                    asset.downloadUrl == sourceUrl ||
                    asset.browserDownloadUrl == sourceUrl
            }
        }.getOrNull()
    }

    private suspend fun resolveReleaseChecksumUrl(
        sourceUrl: String,
        fileName: String,
        repositoryFullName: String?,
        assetId: Long?
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val release = releaseFromDownloadUrl(sourceUrl, repositoryFullName, assetId) ?: return@runCatching null
            val target = release.assets.firstOrNull {
                it.id == assetId || it.name == fileName || it.downloadUrl == sourceUrl || it.browserDownloadUrl == sourceUrl
            } ?: return@runCatching null
            if (!target.name.endsWith(".apk", true) && !target.name.endsWith(".aab", true)) return@runCatching null
            val checksum = ReleaseChecksumResolver.findFor(target, release.assets) ?: return@runCatching null
            val publicUrl = checksum.browserDownloadUrl?.takeIf(String::isNotBlank)
            if (isPublicGitHubReleaseUrl(sourceUrl)) publicUrl ?: checksum.downloadUrl else checksum.downloadUrl
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
            val tag = segments[tagIndex + 1]
            val encodedTag = java.net.URLEncoder.encode(tag, Charsets.UTF_8.name()).replace("+", "%20")
            return fetchRelease("https://api.github.com/repos/$owner/$repo/releases/tags/$encodedTag")
        }
        if (assetId != null || path.contains("/releases/assets/")) {
            val releasesUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=100"
            val request = Request.Builder().url(releasesUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitHub-Rock/1.0").build()
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
        val request = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "GitHub-Rock/1.0").build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return json.decodeFromString<Release>(body)
        }
    }

    fun schedule(download: DownloadEntity) {
        val input = Data.Builder()
            .putLong(DownloadWorker.KEY_ID, download.id)
            .putString(DownloadWorker.KEY_URL, download.sourceUrl)
            .putString(DownloadWorker.KEY_NAME, download.fileName)
            .apply {
                download.fallbackUrl?.takeIf(String::isNotBlank)?.let { putString(DownloadWorker.KEY_FALLBACK_URL, it) }
                download.expectedSha256?.takeIf(String::isNotBlank)?.let { putString(DownloadWorker.KEY_SHA256, it) }
                download.packageName?.takeIf(String::isNotBlank)?.let { putString(DownloadWorker.KEY_EXPECTED_PACKAGE, it) }
                download.checksumUrl?.takeIf(String::isNotBlank)?.let { putString(DownloadWorker.KEY_CHECKSUM_URL, it) }
                download.localPath?.takeIf { it.endsWith(".part") }?.let { putString(DownloadWorker.KEY_PARTIAL_PATH, it) }
            }.build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(DownloadWorker.workName(download.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun deleteOwnedFile(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile?.canonicalFile == downloadsDirectory.canonicalFile }?.delete()
    }

    private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)

    private fun isPublicGitHubReleaseUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.host.equals("github.com", ignoreCase = true) && uri.path?.contains("/releases/download/", ignoreCase = true) == true
    }.getOrDefault(false)

    companion object {
        // Paused is intentionally excluded: enqueue() must be able to detect it
        // and call resume() rather than treating it as already active.
        private val ACTIVE_STATES = setOf(
            DownloadState.QUEUED.wireValue,
            DownloadState.DOWNLOADING.wireValue,
            DownloadState.RETRYING.wireValue
        )
        private val COMPLETED_STATES = setOf(
            DownloadState.COMPLETED.wireValue,
            DownloadState.INSTALLABLE.wireValue
        )
        private val RESUMABLE_STATES = setOf(
            DownloadState.PAUSED.wireValue,
            DownloadState.FAILED.wireValue,
            DownloadState.CANCELLED.wireValue
        )
    }
}
