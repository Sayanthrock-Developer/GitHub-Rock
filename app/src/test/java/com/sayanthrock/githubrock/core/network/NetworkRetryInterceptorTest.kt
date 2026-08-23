package com.sayanthrock.githubrock.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NetworkRetryInterceptorTest {
    private lateinit var server: MockWebServer

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
    fun retriesTransientServerFailureForGet() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkRetryInterceptor(maxRetries = 1, retryDelayMillis = 1))
            .build()

        client.newCall(Request.Builder().url(server.url("/health")).get().build()).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun doesNotRetryPostAfterServerFailure() {
        server.enqueue(MockResponse().setResponseCode(503))

        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkRetryInterceptor(maxRetries = 2, retryDelayMillis = 1))
            .build()

        client.newCall(
            Request.Builder()
                .url(server.url("/write"))
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
        ).execute().use { response ->
            assertEquals(503, response.code)
        }
        assertEquals(1, server.requestCount)
    }
}
