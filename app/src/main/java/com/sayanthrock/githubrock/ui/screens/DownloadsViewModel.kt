package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.util.ApkInspection
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository
) : ViewModel() {
    val downloads: StateFlow<List<DownloadEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeAll().collect { items -> repository.recoverInvalidCompletedDownloads(items) }
        }
    }

    fun enqueue(
        url: String,
        fileName: String,
        expectedPackage: String? = null,
        repositoryFullName: String? = null,
        releaseName: String? = null,
        releaseUrl: String? = null,
        assetId: Long? = null,
        expectedSha256: String? = null,
        fallbackUrl: String? = null
    ) = viewModelScope.launch {
        repository.enqueue(url, fileName, expectedPackage, repositoryFullName, releaseName, releaseUrl, assetId, expectedSha256, fallbackUrl)
    }

    fun downloadAgain(download: DownloadEntity) = viewModelScope.launch { repository.downloadAgain(download) }
    fun pause(download: DownloadEntity) = viewModelScope.launch { repository.pause(download) }
    fun resume(download: DownloadEntity) = viewModelScope.launch { repository.resume(download) }
    fun cancel(download: DownloadEntity) = viewModelScope.launch { repository.cancel(download) }
    fun delete(download: DownloadEntity) = viewModelScope.launch { repository.delete(download) }
    fun retry(download: DownloadEntity) = viewModelScope.launch { repository.downloadAgain(download) }

    fun inspectApk(file: File, callback: (Result<ApkInspection>) -> Unit) {
        viewModelScope.launch { callback(repository.inspectApk(file)) }
    }
}
