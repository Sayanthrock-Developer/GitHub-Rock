package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val dao: DownloadDao,
    @ApplicationContext context: Context
) : ViewModel() {
    private val workManager = WorkManager.getInstance(context)
    private val downloadsDirectory = File(context.filesDir, "downloads")
    val downloads: StateFlow<List<DownloadEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Adds a download to the queue and schedules it for background execution.
     *
     * @param url The source URL of the download.
     * @param fileName The name of the local file.
     */
    fun enqueue(url: String, fileName: String) = viewModelScope.launch {
        val queued = DownloadEntity(fileName = fileName, sourceUrl = url, status = "queued")
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    /**
     * Pauses an active download.
     *
     * @param download The download to pause.
     */
    fun pause(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES) return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        dao.updateStatus(download.id, "paused")
    }

    /**
     * Resumes a paused, failed, or cancelled download.
     *
     * @param download The download to resume.
     */
    fun resume(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in setOf("paused", "failed", "cancelled")) return@launch
        dao.updateStatus(download.id, "queued")
        schedule(download.copy(status = "queued"))
    }

    /**
     * Cancels an active or paused download and removes its local partial file.
     *
     * @param download The download to cancel.
     */
    fun cancel(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES && download.status != "paused") return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        download.localPath?.let(::File)?.takeIf { it.parentFile == downloadsDirectory }?.delete()
        dao.updateProgress(download.id, "cancelled", 0, 0, null, null)
    }

    /**
     * Cancels the download, removes its local file when it is in the downloads directory, and deletes its record.
     *
     * @param download The download to delete.
     */
    fun delete(download: DownloadEntity) = viewModelScope.launch {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        download.localPath?.let(::File)?.takeIf { it.parentFile == downloadsDirectory }?.delete()
        dao.delete(download.id)
    }

    /**
 * Retries a paused, failed, or cancelled download.
 *
 * @param download The download to retry.
 */
fun retry(download: DownloadEntity) = resume(download)

    /**
     * Schedules a download for background execution.
     *
     * Replaces any existing work associated with the download and includes its partial file path when available.
     *
     * @param download The download to schedule.
     */
    private fun schedule(download: DownloadEntity) {
        val input = Data.Builder()
            .putLong(DownloadWorker.KEY_ID, download.id)
            .putString(DownloadWorker.KEY_URL, download.sourceUrl)
            .putString(DownloadWorker.KEY_NAME, download.fileName)
            .apply {
                download.localPath
                    ?.takeIf { it.endsWith(".part") }
                    ?.let { putString(DownloadWorker.KEY_PARTIAL_PATH, it) }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .build()
        workManager.enqueueUniqueWork(
            DownloadWorker.workName(download.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private val ACTIVE_STATUSES = setOf("queued", "downloading", "retrying")
    }
}
