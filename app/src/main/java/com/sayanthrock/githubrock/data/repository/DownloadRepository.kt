package com.sayanthrock.githubrock.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
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

    suspend fun enqueue(url: String, fileName: String, expectedPackage: String? = null) {
        val resolvedUrl = url.trim().takeIf(String::isNotBlank) ?: return
        val queued = DownloadEntity(
            fileName = fileName,
            sourceUrl = resolvedUrl,
            status = "queued",
            packageName = expectedPackage
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
            status = "queued"
        )
        dao.upsert(queued)
        schedule(queued)
    }

    suspend fun pause(download: DownloadEntity) {
        if (download.status !in ACTIVE_STATUSES) return
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        dao.updateStatus(download.id, "paused")
    }

    suspend fun resume(download: DownloadEntity) {
        if (download.status !in setOf("paused", "failed", "cancelled")) return
        dao.updateStatus(download.id, "queued")
        schedule(download.copy(status = "queued"))
    }

    suspend fun cancel(download: DownloadEntity) {
        if (download.status !in ACTIVE_STATUSES && download.status != "paused") return
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteOwnedFile(download.localPath)
        dao.updateProgress(download.id, "cancelled", 0, 0, null, null)
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
            inspectApk(
                applicationContext,
                file,
                expectedPackage = firstPass.packageName,
                previousVersionCode = previous?.versionCode,
                previousPermissions = previous?.permissions?.split("\n")?.filter(String::isNotBlank).orEmpty(),
                previousCertificateSha256 = previous?.certificateSha256
            )
        }
    }

    suspend fun recoverInvalidCompletedDownloads(items: List<DownloadEntity>) {
        items.filter { it.status == "completed" && it.isApkDownload() }.forEach { download ->
            val valid = download.localPath?.let(::File)?.takeIf(File::isFile)?.let { file ->
                withContext(Dispatchers.IO) { runCatching { inspectApk(applicationContext, file) }.isSuccess }
            } == true
            if (!valid) downloadAgain(download)
        }
    }

    suspend fun latestCompletedForPackage(packageName: String): DownloadEntity? =
        dao.latestCompletedForPackage(packageName)

    suspend fun updateProgress(id: Long, status: String, downloaded: Long, total: Long, path: String?, sha: String?) =
        dao.updateProgress(id, status, downloaded, total, path, sha)

    suspend fun updateSecurity(
        id: Long,
        packageName: String,
        versionCode: Long,
        versionName: String?,
        minSdk: Int,
        targetSdk: Int,
        permissions: String,
        certificateSha256: String?,
        signatureSchemes: String,
        architectures: String,
        securityRisk: String,
        securityReasons: String
    ) = dao.updateSecurity(id, packageName, versionCode, versionName, minSdk, targetSdk, permissions, certificateSha256, signatureSchemes, architectures, securityRisk, securityReasons)

    fun schedule(download: DownloadEntity) {
        val input = Data.Builder()
            .putLong(DownloadWorker.KEY_ID, download.id)
            .putString(DownloadWorker.KEY_URL, download.sourceUrl)
            .putString(DownloadWorker.KEY_NAME, download.fileName)
            .apply {
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
        private val ACTIVE_STATUSES = setOf("queued", "downloading", "retrying")
    }
}
