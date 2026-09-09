package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.ChecksumVerifier
import com.sayanthrock.githubrock.download.ReleaseChecksumFetcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ReleaseChecksumFetcherTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun checksumEndpointFailure_isNonBlockingAndReturnsNoChecksum() {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = ReleaseChecksumFetcher.tryFetch(
            client = client,
            checksumUrl = server.url("/checksums").toString(),
            targetName = "release.apk"
        )

        assertNull(result)
        assertEquals("/checksums", server.takeRequest().path)
    }

    @Test
    fun validChecksumEntry_isParsedForTargetAsset() {
        val sha = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("$sha  release.apk\n")
        )

        val result = ReleaseChecksumFetcher.tryFetch(
            client = client,
            checksumUrl = server.url("/checksums").toString(),
            targetName = "release.apk"
        )

        assertEquals(sha, result)
    }

    @Test
    fun checksumMismatch_isRejectedByVerifier() {
        val actual = ChecksumVerifier.sha256("github-rock-binary".byteInputStream())
        val incorrect = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        assertFalse(ChecksumVerifier.matches(actual, incorrect))
    }
}
