package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.WorkflowStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowPreviewInspectorTest {
    @Test
    fun `valid workflow and successful run are healthy`() {
        val report = WorkflowPreviewInspector.inspect(
            source = """
                name: Android CI
                on: [push]
                jobs:
                  verify:
                    runs-on: ubuntu-latest
            """.trimIndent(),
            run = WorkflowRun(id = 1, status = "completed", conclusion = "success"),
            jobs = listOf(
                WorkflowJob(
                    id = 2,
                    name = "verify",
                    status = "completed",
                    conclusion = "success",
                    steps = listOf(WorkflowStep("Build", "completed", "success"))
                )
            )
        )

        assertEquals(WorkflowPreviewHealth.Healthy, report.health)
        assertEquals(1, report.completedSteps)
        assertEquals(0, report.failedSteps)
    }

    @Test
    fun `quoted and indented root keys are accepted`() {
        val report = WorkflowPreviewInspector.inspect(
            source = """
                  "name": Android CI
                  'on': [push]
                  "jobs":
                    verify:
                      runs-on: ubuntu-latest
            """.trimIndent(),
            run = null,
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Healthy, report.health)
        assertTrue(report.sourceProblems.isEmpty())
    }

    @Test
    fun `failed step is reported as a problem`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\njobs:\n  test:\n    runs-on: ubuntu-latest",
            run = WorkflowRun(id = 1, status = "completed", conclusion = "failure"),
            jobs = listOf(
                WorkflowJob(
                    id = 2,
                    name = "test",
                    status = "completed",
                    conclusion = "failure",
                    steps = listOf(WorkflowStep("Unit tests", "completed", "failure"))
                )
            )
        )

        assertEquals(WorkflowPreviewHealth.Problem, report.health)
        assertEquals(1, report.failedSteps)
    }

    @Test
    fun `cancelled run is not reported as healthy`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\njobs:\n  test:\n    runs-on: ubuntu-latest",
            run = WorkflowRun(id = 1, status = "completed", conclusion = "cancelled"),
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Unknown, report.health)
        assertEquals("Workflow was cancelled", report.title)
    }

    @Test
    fun `missing jobs and tab indentation are visible source problems`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\n\tbad: true",
            run = null,
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Problem, report.health)
        assertTrue(report.sourceProblems.contains("Jobs section is missing"))
        assertTrue(report.sourceProblems.contains("Tab indentation can break YAML parsing"))
    }

    @Test
    fun `sourceError returns problem health and includes error`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\njobs:\n  test:\n    runs-on: ubuntu-latest",
            run = null,
            jobs = emptyList(),
            sourceError = "Network error fetching workflow file"
        )

        assertEquals(WorkflowPreviewHealth.Problem, report.health)
        assertTrue(report.sourceProblems.contains("Network error fetching workflow file"))
        assertEquals("Network error fetching workflow file", report.detail)
    }

    @Test
    fun `missing name and trigger in YAML reports problem`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "jobs:\n  test:\n    runs-on: ubuntu-latest",
            run = null,
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Problem, report.health)
        assertTrue(report.sourceProblems.contains("Workflow name is missing"))
        assertTrue(report.sourceProblems.contains("Workflow trigger is missing"))
    }

    @Test
    fun `active step returns running health`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\njobs:\n  test:\n    runs-on: ubuntu-latest",
            run = null,
            jobs = listOf(
                WorkflowJob(
                    id = 2,
                    name = "test",
                    status = "in_progress",
                    steps = listOf(WorkflowStep("Unit tests", "in_progress"))
                )
            )
        )

        assertEquals(WorkflowPreviewHealth.Running, report.health)
        assertEquals("Workflow is running", report.title)
        assertEquals("GitHub is still processing the current run", report.detail)
    }

    @Test
    fun `queued run returns running health`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "name: CI\non: [push]\njobs:\n  test:\n    runs-on: ubuntu-latest",
            run = WorkflowRun(id = 1, status = "queued", conclusion = null, displayTitle = "", event = "", htmlUrl = "", createdAt = ""),
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Running, report.health)
        assertEquals("Workflow is running", report.title)
        assertEquals("Basic YAML structure checks passed", report.detail)
    }

    @Test
    fun `empty source and no run returns unknown with specific detail`() {
        val report = WorkflowPreviewInspector.inspect(
            source = "",
            run = null,
            jobs = emptyList()
        )

        // This hits the default case for title ("No completed run yet") and detail ("Select a repository with an active workflow")
        assertEquals(WorkflowPreviewHealth.Unknown, report.health)
        assertEquals("No completed run yet", report.title)
        assertEquals("Select a repository with an active workflow", report.detail)
    }

    @Test
    fun `null source and no run returns unknown with specific detail`() {
        val report = WorkflowPreviewInspector.inspect(
            source = null,
            run = null,
            jobs = emptyList()
        )

        assertEquals(WorkflowPreviewHealth.Unknown, report.health)
        assertEquals("No completed run yet", report.title)
        assertEquals("Select a repository with an active workflow", report.detail)
    }
}
