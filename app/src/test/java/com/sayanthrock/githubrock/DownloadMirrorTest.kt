package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.DownloadMirror
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadMirrorTest {
    private val releaseUrl = "https://github.com/owner/repository/releases/download/v1.0/app.apk"

    @Test
    fun directGitHubKeepsOriginalUrl() {
        assertEquals(releaseUrl, DownloadMirror.Direct.resolve(releaseUrl))
    }

    @Test
    fun communityPrefixMirrorsReceiveTrustedGitHubUrl() {
        assertEquals("https://ghfast.top/$releaseUrl", DownloadMirror.GhFast.resolve(releaseUrl))
        assertEquals("https://github.moeyy.xyz/$releaseUrl", DownloadMirror.GitHubMoeyy.resolve(releaseUrl))
        assertEquals("https://gh-proxy.com/$releaseUrl", DownloadMirror.GhProxy.resolve(releaseUrl))
        assertEquals("https://ghps.cc/$releaseUrl", DownloadMirror.Ghps.resolve(releaseUrl))
        assertEquals("https://gh.api.99988866.xyz/$releaseUrl", DownloadMirror.Api99988866.resolve(releaseUrl))
    }

    @Test
    fun jsDelivrConvertsRawRepositoryFile() {
        val resolved = DownloadMirror.JsDelivr.resolve(
            "https://raw.githubusercontent.com/owner/repository/main/path/image.png"
        )
        assertEquals(
            "https://fastly.jsdelivr.net/gh/owner/repository@main/path/image.png",
            resolved
        )
    }

    @Test(expected = IllegalStateException::class)
    fun jsDelivrRejectsReleaseAsset() {
        DownloadMirror.JsDelivr.resolve(releaseUrl)
    }

    @Test
    fun unknownSavedValueFallsBackToOfficialEndpoint() {
        assertTrue(DownloadMirror.fromId("unknown") == DownloadMirror.Direct)
    }
}
