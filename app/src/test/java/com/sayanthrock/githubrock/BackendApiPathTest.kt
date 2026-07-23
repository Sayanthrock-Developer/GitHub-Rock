package com.sayanthrock.githubrock

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sayanthrock.githubrock.core.network.BackendDevicePollRequest
import com.sayanthrock.githubrock.core.network.BackendTokenRefreshRequest
import com.sayanthrock.githubrock.core.network.GitHubRockBackendApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class BackendApiPathTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GitHubRockBackendApi

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubRockBackendApi::class.java)
    }

    @After fun tearDown() {
        server.shutdown()
    }

    @Test fun mobileClientUsesTheBackendV1Contract() = runBlocking {
        server.enqueue(jsonResponse("""{
            "status":"healthy","version":"0.1.0","postgres":"up",
            "redis":"up","meilisearch":"up","timestamp":"2026-07-23T00:00:00Z"
        }"""))
        server.enqueue(jsonResponse("""{
            "apiVersion":"v1","minSupportedAppVersion":"0.1.0",
            "latestAppVersion":"0.1.0","maintenanceMode":false,
            "features":{"oauthDeviceProxy":true,"oauthRefreshProxy":true}
        }"""))
        server.enqueue(jsonResponse("""{
            "device_code":"device","user_code":"ABCD-EFGH",
            "verification_uri":"https://github.com/login/device",
            "expires_in":900,"interval":5
        }"""))
        server.enqueue(jsonResponse("""{"state":"pending"}"""))
        server.enqueue(jsonResponse("""{
            "state":"authorized","access_token":"access","token_type":"bearer",
            "refresh_token":"refresh","expires_in":28800
        }"""))

        api.health()
        api.config()
        api.startDeviceFlow()
        api.pollDeviceFlow(BackendDevicePollRequest("device"))
        api.refreshToken(BackendTokenRefreshRequest("refresh"))

        assertEquals("/v1/health", server.takeRequest().path)
        assertEquals("/v1/config", server.takeRequest().path)
        assertEquals("/v1/auth/device/start", server.takeRequest().path)
        assertEquals("/v1/auth/device/poll", server.takeRequest().path)
        assertEquals("/v1/auth/device/refresh", server.takeRequest().path)
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
