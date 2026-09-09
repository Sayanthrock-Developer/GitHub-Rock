package com.sayanthrock.githubrock.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayanthrock.githubrock.data.repository.findExistingDownload
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDaoDeduplicationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val dao = database.downloadDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun findsExistingReleaseAssetByAssetId() = runBlocking {
        val download = DownloadEntity(
            fileName = "app.apk",
            sourceUrl = "https://github.com/example/repo/releases/download/v1/app.apk",
            status = DownloadState.DOWNLOADING.wireValue,
            repositoryFullName = "example/repo",
            releaseName = "v1",
            releaseUrl = "https://github.com/example/repo/releases/tag/v1",
            assetId = 123L
        )
        val id = dao.upsert(download)

        val existing = dao.findByAssetId(123L)

        assertEquals(id, existing?.id)
        assertEquals(123L, existing?.assetId)
    }

    @Test
    fun findsExistingDownloadByResolvedBrowserUrl() = runBlocking {
        val browserUrl = "https://github.com/example/repo/releases/download/v1/app.apk"
        val download = DownloadEntity(
            fileName = "app.apk",
            sourceUrl = browserUrl,
            status = DownloadState.DOWNLOADING.wireValue
        )
        val id = dao.upsert(download)

        val existing = dao.findBySourceUrl(browserUrl)

        assertEquals(id, existing?.id)
        assertEquals(browserUrl, existing?.sourceUrl)
    }

    @Test
    fun canonicalAssetIdPreventsDuplicateWhenUrlChanges() = runBlocking {
        val originalUrl = "https://github.com/example/repo/releases/download/v1/app.apk"
        val alternateUrl = "https://api.github.com/repos/example/repo/releases/assets/123"
        val download = DownloadEntity(
            fileName = "app.apk",
            sourceUrl = originalUrl,
            status = DownloadState.DOWNLOADING.wireValue,
            repositoryFullName = "example/repo",
            releaseName = "v1",
            releaseUrl = "https://github.com/example/repo/releases/tag/v1",
            assetId = 123L
        )
        val id = dao.upsert(download)

        val existing = findExistingDownload(dao, resolvedAssetId = 123L, resolvedUrl = alternateUrl)

        assertEquals(id, existing?.id)
        assertEquals(123L, existing?.assetId)
        assertEquals(originalUrl, existing?.sourceUrl)
    }
}
