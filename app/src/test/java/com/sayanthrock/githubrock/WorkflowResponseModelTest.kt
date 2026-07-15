package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.WorkflowArtifactsResponse
import com.sayanthrock.githubrock.core.model.WorkflowJobsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkflowResponseModelTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun jobsEnvelopeAcceptsNumericTotalCount() {
        val response = json.decodeFromString<WorkflowJobsResponse>(
            """{"total_count":1,"jobs":[{"id":42,"name":"verify","status":"completed","conclusion":"success","steps":[]}]}"""
        )

        assertEquals(1, response.totalCount)
        assertEquals("verify", response.jobs.single().name)
    }

    @Test
    fun artifactsEnvelopeAcceptsNumericTotalCount() {
        val response = json.decodeFromString<WorkflowArtifactsResponse>(
            """{"total_count":1,"artifacts":[{"id":7,"name":"debug-apk","archive_download_url":"https://api.github.com/artifacts/7","expired":false,"size_in_bytes":2048}]}"""
        )

        assertEquals(1, response.totalCount)
        assertEquals("debug-apk", response.artifacts.single().name)
    }
}
