package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sayanthrock.githubrock.data.local.DownloadDao
import com.sayanthrock.githubrock.data.local.DownloadEntity
import com.sayanthrock.githubrock.download.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val dao: DownloadDao,
    @ApplicationContext context: Context
) : ViewModel() {
    private val workManager = WorkManager.getInstance(context)
    val downloads: StateFlow<List<DownloadEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun enqueue(url: String, fileName: String) = viewModelScope.launch {
        val id = dao.upsert(DownloadEntity(fileName = fileName, sourceUrl = url, status = "queued"))
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putLong(DownloadWorker.KEY_ID, id).putString(DownloadWorker.KEY_URL, url).putString(DownloadWorker.KEY_NAME, fileName).build())
            .build()
        workManager.enqueue(request)
    }

    fun delete(id: Long) = viewModelScope.launch { dao.delete(id) }

    fun retry(download: DownloadEntity) = enqueue(download.sourceUrl, download.fileName)
}
