package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.ManualDownloadType
import com.sayanthrock.githubrock.core.model.validateManualDownload
import com.sayanthrock.githubrock.ui.screens.downloadProgressLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ManualDownloadTest {
    @Test fun imageDownloadDerivesSafeName() {
        val result = validateManualDownload(
            ManualDownloadType.Image,
            "https://raw.githubusercontent.com/octocat/Hello-World/main/photo.png",
            ""
        )

        assertNull(result.error)
        assertEquals("photo.png", result.request?.fileName)
        assertEquals(ManualDownloadType.Image, result.request?.type)
    }

    @Test fun fileDownloadAddsFallbackExtension() {
        val result = validateManualDownload(
            ManualDownloadType.File,
            "https://github.com/octocat/Hello-World/releases/download/v1/archive",
            "release archive"
        )

        assertNull(result.error)
        assertEquals("release-archive.bin", result.request?.fileName)
    }

    @Test fun nonGitHubAndNonHttpsLinksAreRejected() {
        val external = validateManualDownload(ManualDownloadType.File, "https://example.com/file.zip", "file.zip")
        val insecure = validateManualDownload(ManualDownloadType.File, "http://github.com/file.zip", "file.zip")

        assertNotNull(external.error)
        assertNotNull(insecure.error)
    }

    @Test fun imageTypeRejectsNonImageExtension() {
        val result = validateManualDownload(
            ManualDownloadType.Image,
            "https://github.com/octocat/Hello-World/releases/download/v1/app.apk",
            "app.apk"
        )

        assertEquals("Image downloads must use PNG, JPG, JPEG, WebP, or GIF.", result.error)
    }

    @Test fun downloadLevelsUseTheSameZeroToOneHundredScale() {
        assertEquals(0, downloadProgressLevel(0, 0, "queued"))
        assertEquals(50, downloadProgressLevel(500, 1_000, "downloading"))
        assertEquals(100, downloadProgressLevel(0, 0, "completed"))
        assertEquals(100, downloadProgressLevel(2_000, 1_000, "downloading"))
    }
}
