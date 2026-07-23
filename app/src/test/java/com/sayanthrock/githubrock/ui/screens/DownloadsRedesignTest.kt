package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.data.local.DownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadsRedesignTest {
    private val downloads = listOf(
        DownloadEntity(
            id = 1,
            fileName = "GitHub-Rock.apk",
            sourceUrl = "https://github.com/Sayanthrock-Developer/GitHub-Rock/releases/download/v1/app.apk",
            status = "completed"
        ),
        DownloadEntity(
            id = 2,
            fileName = "screenshots.zip",
            sourceUrl = "https://github.com/example/project/releases/download/v1/screenshots.zip",
            status = "downloading"
        ),
        DownloadEntity(
            id = 3,
            fileName = "notes.md",
            sourceUrl = "https://raw.githubusercontent.com/example/project/main/notes.md",
            status = "paused"
        )
    )

    @Test
    fun filtersKeepApplicationsFilesAndActiveTransfersSeparate() {
        assertEquals(listOf(1L), filterDownloads(downloads, DownloadListFilter.Applications).map { it.id })
        assertEquals(listOf(2L, 3L), filterDownloads(downloads, DownloadListFilter.Files).map { it.id })
        assertEquals(listOf(2L, 3L), filterDownloads(downloads, DownloadListFilter.Active).map { it.id })
        assertEquals(listOf(1L), filterDownloads(downloads, DownloadListFilter.Completed).map { it.id })
        assertEquals(downloads, filterDownloads(downloads, DownloadListFilter.All))
    }

    @Test
    fun extractedApplicationLabelReplacesTheFilename() {
        assertEquals("GitHub Rock", preferredApplicationName("GitHub-Rock.apk", "GitHub Rock"))
        assertEquals("GitHub Rock", preferredApplicationName("GitHub-Rock.apk", null))
        assertEquals("Komi Store", preferredApplicationName("Komi_Store.apk", "  "))
    }
}
