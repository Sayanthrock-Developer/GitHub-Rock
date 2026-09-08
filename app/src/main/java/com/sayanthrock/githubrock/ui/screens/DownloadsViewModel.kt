package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.core.util.inspectApk
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val dao: DownloadDao,
    @ApplicationContext context: Context
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val workManager = WorkManager.getInstance(applicationContext)
    private val downloadsDirectory = File(applicationContext.filesDir, "downloads")

    val downloads: StateFlow<List<DownloadEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            dao.observeAll().collect { items ->
                items.forEach { reconcileDownload(it) }
            }
        }
    }

    /**
     * Creates or reuses a persistent download record. The source URL + file name
     * form the asset identity, preventing duplicate workers for the same asset.
     */
    fun enqueue(url: String, fileName: String, expectedPackage: String? = null) = viewModelScope.launch {
        val resolvedUrl = url.trim().takeIf(String::isNotBlank) ?: return@launch
        val safeName = fileName.trim().takeIf(String::isNotBlank) ?: return@launch
        val existing = dao.findBySourceAndName(resolvedUrl, safeName)
        if (existing != null) {
            when (existing.status) {
                "queued", "downloading", "retrying", "paused" -> return@launch
                "completed" -> {
                    if (existing.localPath?.let(::File)?.isFile == true) return@launch
                }
            }
            val restart = existing.copy(
                packageName = expectedPackage ?: existing.packageName,
                status = "queued",
                sha256 = null,
                downloadedBytes = existing.localPath?.let(::File)?.takeIf { it.name.endsWith(".part") }?.length() ?: 0L,
                totalBytes = 0L
            )
            dao.upsert(restart)
            schedule(restart)
            return@launch
        }

        val queued = DownloadEntity(
            fileName = safeName,
            sourceUrl = resolvedUrl,
            status = "queued",
            packageName = expectedPackage
        )
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    fun downloadAgain(download: DownloadEntity) = viewModelScope.launch {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteLocalFile(download.localPath)
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

    fun pause(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES) return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        // Persist the user intent after cancellation so a late worker callback
        // cannot turn an explicitly paused item into an automatic retry.
        dao.updateStatus(download.id, "paused")
    }

    fun resume(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in RESUMABLE_STATUSES) return@launch
        val file = download.localPath?.let(::File)
        val preservedPartial = file?.takeIf { it.parentFile?.canonicalFile == downloadsDirectory.canonicalFile && it.name.endsWith(".part") }
        val resumed = download.copy(
            localPath = preservedPartial?.absolutePath,
            downloadedBytes = preservedPartial?.length() ?: download.downloadedBytes,
            status = "queued",
            sha256 = null
        )
        dao.upsert(resumed)
        schedule(resumed)
    }

    fun cancel(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES && download.status != "paused") return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteLocalFile(download.localPath)
        dao.updateProgress(download.id, "cancelled", 0, 0, null, null)
    }

    fun delete(download: DownloadEntity) = viewModelScope.launch {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteLocalFile(download.localPath)
        dao.delete(download.id)
    }

    fun retry(download: DownloadEntity) = resume(download)

    fun inspectApk(file: File, callback: (Result<ApkInspection>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
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
            callback(result)
        }
    }

    /**
     * Reconciles persisted state with WorkManager after process death/background
     * eviction. Paused/completed/cancelled records are intentionally not revived.
     */
    private suspend fun reconcileDownload(download: DownloadEntity) {
        val local = download.localPath?.let(::File)
        if (download.status == "completed" && (local == null || !local.isFile)) {
            dao.updateProgress(download.id, "failed", 0, download.totalBytes, null, null)
            return
        }
        if (download.status !in RECOVERABLE_STATUSES) return

        val infos = runCatching {
            workManager.getWorkInfosForUniqueWork(DownloadWorker.workName(download.id)).await()
        }.getOrDefault(emptyList())
        val hasActiveWork = infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        if (!hasActiveWork) {
            schedule(download)
        }
    }

    private fun schedule(download: DownloadEntity) {
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
        // A unique work name is the final duplicate-worker guard. REPLACE is used
        // deliberately for explicit Resume/Retry, while reconciliation only
        // schedules when no active work exists.
        workManager.enqueueUniqueWork(DownloadWorker.workName(download.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun deleteLocalFile(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile?.canonicalFile == downloadsDirectory.canonicalFile }?.delete()
    }

    private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)

    companion object {
        private val ACTIVE_STATUSES = setOf("queued", "downloading", "retrying")
        private val RESUMABLE_STATUSES = setOf("paused", "failed", "cancelled", "retrying")
        private val RECOVERABLE_STATUSES = setOf("queued", "downloading", "retrying")
    }
}
