package com.sayanthrock.githubrock

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sayanthrock.githubrock.core.model.CreateRepositoryRequest
import com.sayanthrock.githubrock.core.model.RenameBranchRequest
import com.sayanthrock.githubrock.core.network.RepositoryCreationApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RepositoryCreationApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: RepositoryCreationApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RepositoryCreationApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun personalRepositoryUsesGitHubWriteEndpointAndBody() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"id":1,"name":"native-repo","full_name":"SayanthRock/native-repo","owner":{"login":"SayanthRock"},"private":true,"default_branch":"main"}"""
                )
        )

        val result = api.createUserRepository(
            CreateRepositoryRequest(
                name = "native-repo",
                description = "Native Android repository tools",
                privateRepository = true,
                initializeReadme = true,
                gitignoreTemplate = "Android",
                licenseTemplate = "mit"
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/user/repos", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("native-repo", body.getValue("name").jsonPrimitive.content)
        assertEquals("Native Android repository tools", body.getValue("description").jsonPrimitive.content)
        assertTrue(body.getValue("private").jsonPrimitive.boolean)
        assertTrue(body.getValue("auto_init").jsonPrimitive.boolean)
        assertEquals("Android", body.getValue("gitignore_template").jsonPrimitive.content)
        assertEquals("mit", body.getValue("license_template").jsonPrimitive.content)
        assertEquals("SayanthRock/native-repo", result.fullName)
    }

    @Test
    fun branchWithSlashIsEncodedAsSinglePathSegment() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        val response = api.renameBranch(
            owner = "SayanthRock",
            repository = "native-repo",
            branch = "release/1.0",
            request = RenameBranchRequest("main")
        )

        val request = server.takeRequest()
        assertTrue(response.isSuccessful)
        assertEquals(
            "/repos/SayanthRock/native-repo/branches/release%2F1.0/rename",
            request.path
        )
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("main", body.getValue("new_name").jsonPrimitive.content)
    }
}
