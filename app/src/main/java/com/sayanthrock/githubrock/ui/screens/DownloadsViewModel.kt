package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.sayanthrock.githubrock.core.model.DownloadMirror
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _selectedMirror = MutableStateFlow(DownloadMirror.fromId(preferences.getString(KEY_DOWNLOAD_MIRROR, null)))

    val selectedMirror: StateFlow<DownloadMirror> = _selectedMirror.asStateFlow()
    val downloads: StateFlow<List<DownloadEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMirror(mirror: DownloadMirror) {
        _selectedMirror.value = mirror
        preferences.edit().putString(KEY_DOWNLOAD_MIRROR, mirror.id).apply()
    }

    fun enqueue(url: String, fileName: String, expectedPackage: String? = null) = viewModelScope.launch {
        val resolvedUrl = runCatching { _selectedMirror.value.resolve(url) }.getOrElse { url }
        val queued = DownloadEntity(fileName = fileName, sourceUrl = resolvedUrl, status = "queued", packageName = expectedPackage)
        val id = dao.upsert(queued)
        schedule(queued.copy(id = id))
    }

    fun pause(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES) return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        dao.updateStatus(download.id, "paused")
    }

    fun resume(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in setOf("paused", "failed", "cancelled")) return@launch
        dao.updateStatus(download.id, "queued")
        schedule(download.copy(status = "queued"))
    }

    fun cancel(download: DownloadEntity) = viewModelScope.launch {
        if (download.status !in ACTIVE_STATUSES && download.status != "paused") return@launch
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        download.localPath?.let(::File)?.takeIf { it.parentFile == downloadsDirectory }?.delete()
        dao.updateProgress(download.id, "cancelled", 0, 0, null, null)
    }

    fun delete(download: DownloadEntity) = viewModelScope.launch {
        workManager.cancelUniqueWork(DownloadWorker.workName(download.id)).await()
        download.localPath?.let(::File)?.takeIf { it.parentFile == downloadsDirectory }?.delete()
        dao.delete(download.id)
    }

    fun retry(download: DownloadEntity) = resume(download)

    fun inspectApk(file: File, callback: (Result<ApkInspection>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val current = dao.latestCompletedForPackage(file.nameWithoutExtension)
                    inspectApk(
                        applicationContext,
                        file,
                        previousVersionCode = current?.versionCode,
                        previousPermissions = current?.permissions?.split("\n")?.filter(String::isNotBlank).orEmpty(),
                        previousCertificateSha256 = current?.certificateSha256
                    )
                }
            }
            callback(result)
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
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(input).build()
        workManager.enqueueUniqueWork(DownloadWorker.workName(download.id), ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val PREFERENCES_NAME = "github_rock_downloads"
        private const val KEY_DOWNLOAD_MIRROR = "download_mirror"
        private val ACTIVE_STATUSES = setOf("queued", "downloading", "retrying")
    }
}
