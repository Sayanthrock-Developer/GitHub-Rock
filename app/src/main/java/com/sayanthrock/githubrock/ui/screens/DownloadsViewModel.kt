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
import com.sayanthrock.githubrock.download.DownloadState
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
                items.forEach { download ->
                    if (download.status == DownloadState.COMPLETED.wire && download.isApkDownload()) {
                        val valid = download.localPath?.let(::File)?.let { file ->
                            withContext(Dispatchers.IO) {
                                file.isFile && runCatching { inspectApk(applicationContext, file) }.isSuccess
                            }
                        } == true
                        if (!valid) {
                            markForRecovery(download)
                            return@forEach
                        }
                    }
                    reconcileDownload(download)
                }
            }
        }
    }

    /** Creates or reuses a persistent record; source URL + file name identify an asset. */
    fun enqueue(url: String, fileName: String, expectedPackage: String? = null) = viewModelScope.launch {
        val resolvedUrl = url.trim().takeIf(String::isNotBlank) ?: return@launch
        val safeName = fileName.trim().takeIf(String::isNotBlank) ?: return@launch
        val existing = dao.findBySourceAndName(resolvedUrl, safeName)
        if (existing != null) {
            when (existing.status) {
                DownloadState.QUEUED.wire, DownloadState.DOWNLOADING.wire, DownloadState.RETRYING.wire, DownloadState.PAUSED.wire -> return@launch
                DownloadState.COMPLETED.wire -> if (existing.localPath?.let(::File)?.isFile == true) return@launch
            }
            val partial = existing.localPath?.let(::File)?.takeIf { isSafePartial(it) }
            val restart = existing.copy(
                packageName = expectedPackage ?: existing.packageName,
                status = DownloadState.QUEUED.wire,
                sha256 = null,
                downloadedBytes = partial?.length() ?: 0L,
                totalBytes = 0L,
                localPath = partial?.absolutePath
            )
            dao.upsert(restart)
            schedule(restart)
            return@launch
        }

        val queued = DownloadEntity(fileName = safeName, sourceUrl = resolvedUrl, status = DownloadState.QUEUED.wire, packageName = expectedPackage)
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    fun downloadAgain(download: DownloadEntity) = viewModelScope.launch {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteLocalFile(download.localPath)
        val queued = download.copy(localPath = null, totalBytes = 0, downloadedBytes = 0, sha256 = null, status = DownloadState.QUEUED.wire)
        dao.upsert(queued)
        schedule(queued)
    }

    fun pause(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES) return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        dao.updateStatus(download.id, DownloadState.PAUSED.wire)
    }

    fun resume(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in RESUMABLE_STATUSES) return@launch
        val partial = download.localPath?.let(::File)?.takeIf(::isSafePartial)
        val resumed = download.copy(
            localPath = partial?.absolutePath,
            downloadedBytes = partial?.length() ?: download.downloadedBytes,
            status = DownloadState.QUEUED.wire,
            sha256 = null
        )
        dao.upsert(resumed)
        schedule(resumed)
    }

    fun cancel(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES && download.status != DownloadState.PAUSED.wire) return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        deleteLocalFile(download.localPath)
        dao.updateProgress(download.id, DownloadState.CANCELLED.wire, 0, 0, null, null)
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
                    inspectApk(applicationContext, file, expectedPackage = firstPass.packageName, previousVersionCode = previous?.versionCode, previousPermissions = previous?.permissions?.split("\n")?.filter(String::isNotBlank).orEmpty(), previousCertificateSha256 = previous?.certificateSha256)
                }
            }
            callback(result)
        }
    }

    private suspend fun markForRecovery(download: DownloadEntity) {
        val partial = download.localPath?.let(::File)?.takeIf(::isSafePartial)
        if (partial != null) {
            dao.updateProgress(download.id, DownloadState.FAILED.wire, partial.length(), download.totalBytes, partial.absolutePath, null)
        } else {
            dao.updateProgress(download.id, DownloadState.FAILED.wire, 0, 0, null, null)
        }
    }

    private suspend fun reconcileDownload(download: DownloadEntity) {
        val local = download.localPath?.let(::File)
        if (download.status == DownloadState.COMPLETED.wire && (local == null || !local.isFile)) {
            dao.updateProgress(download.id, DownloadState.FAILED.wire, 0, download.totalBytes, null, null)
            return
        }
        if (download.status !in RECOVERABLE_STATUSES) return
        val infos = runCatching { workManager.getWorkInfosForUniqueWork(DownloadWorker.workName(download.id)).await() }.getOrDefault(emptyList())
        val hasActiveWork = infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        if (!hasActiveWork) schedule(download)
    }

    private fun schedule(download: DownloadEntity) {
        val input = Data.Builder()
            .putLong(DownloadWorker.KEY_ID, download.id)
            .putString(DownloadWorker.KEY_URL, download.sourceUrl)
            .putString(DownloadWorker.KEY_NAME, download.fileName)
            .apply {
                download.packageName?.takeIf(String::isNotBlank)?.let { putString(DownloadWorker.KEY_EXPECTED_PACKAGE, it) }
                download.localPath?.takeIf { it.endsWith(".part") && isSafePartial(File(it)) }?.let { putString(DownloadWorker.KEY_PARTIAL_PATH, it) }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(DownloadWorker.workName(download.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun isSafePartial(file: File): Boolean = runCatching {
        file.canonicalFile.parentFile == downloadsDirectory.canonicalFile && file.name.endsWith(".part")
    }.getOrDefault(false)

    private fun deleteLocalFile(path: String?) {
        path?.let(::File)?.takeIf { runCatching { it.canonicalFile.parentFile == downloadsDirectory.canonicalFile }.getOrDefault(false) }?.delete()
    }

    private fun DownloadEntity.isApkDownload(): Boolean = fileName.endsWith(".apk", ignoreCase = true)

    companion object {
        private val ACTIVE_STATUSES = setOf(DownloadState.QUEUED.wire, DownloadState.DOWNLOADING.wire, DownloadState.RETRYING.wire)
        private val RESUMABLE_STATUSES = setOf(DownloadState.PAUSED.wire, DownloadState.FAILED.wire, DownloadState.CANCELLED.wire, DownloadState.RETRYING.wire)
        private val RECOVERABLE_STATUSES = setOf(DownloadState.QUEUED.wire, DownloadState.STARTING.wire, DownloadState.DOWNLOADING.wire, DownloadState.RETRYING.wire)
    }
}
