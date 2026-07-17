package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.WorkflowJob
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.WorkflowStep
import com.sayanthrock.githubrock.core.util.WorkflowPreviewHealth
import com.sayanthrock.githubrock.core.util.WorkflowPreviewInspector
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
}
