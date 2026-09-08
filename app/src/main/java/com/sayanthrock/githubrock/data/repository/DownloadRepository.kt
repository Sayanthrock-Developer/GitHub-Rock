package com.sayanthrock.githubrock.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.local.DownloadState
import com.sayanthrock.githubrock.data.local.state
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadDao,
    @ApplicationContext context: Context
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
        fallbackUrl: String? = null
    ) {
        val resolvedUrl = url.trim().takeIf(String::isNotBlank) ?: return
        val resolvedFallbackUrl = fallbackUrl?.trim()?.takeIf { it.isNotBlank() && it != resolvedUrl }
        val queued = DownloadEntity(
            fileName = fileName,
            sourceUrl = resolvedUrl,
            fallbackUrl = resolvedFallbackUrl,
            status = DownloadState.QUEUED.wireValue,
            expectedSha256 = expectedSha256,
            packageName = expectedPackage,
            repositoryFullName = repositoryFullName,
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
        if (download.state !in ACTIVE_STATES) return
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        transition(download.id, DownloadState.PAUSED)
    }

    suspend fun resume(download: DownloadEntity) {
        if (download.state !in RESUMABLE_STATES) return
        transition(download.id, DownloadState.QUEUED)
        schedule(download.copy(status = DownloadState.QUEUED.wireValue, errorMessage = null))
    }

    suspend fun cancel(download: DownloadEntity) {
        if (download.state !in ACTIVE_STATES && download.state != DownloadState.PAUSED) return
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
        items.filter { it.state in setOf(DownloadState.COMPLETED, DownloadState.INSTALLABLE) && it.isApkDownload() }.forEach { download ->
            val valid = download.localPath?.let(::File)?.takeIf(File::isFile)?.let { file ->
                withContext(Dispatchers.IO) { runCatching { inspectApk(applicationContext, file) }.isSuccess }
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

    private suspend fun transition(id: Long, state: DownloadState, errorMessage: String? = null) {
        dao.updateStatus(id, state.wireValue, errorMessage)
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
                download.localPath?.takeIf { it.endsWith(".part") }?.let { putString(DownloadWorker.KEY_PARTIAL_PATH, it) }
            }
            .build()
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

    companion object {
        private val ACTIVE_STATES = setOf(DownloadState.QUEUED, DownloadState.DOWNLOADING, DownloadState.RETRYING)
        private val RESUMABLE_STATES = setOf(DownloadState.PAUSED, DownloadState.FAILED, DownloadState.CANCELLED)
    }
}
