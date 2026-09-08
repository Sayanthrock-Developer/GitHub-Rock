package com.sayanthrock.githubrock.core.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkflowRunTimingTest {
    @Test
    fun completedRun_reportsExecutionTimeFromRunStartedAtToUpdatedAt() {
        val run = WorkflowRun(
            id = 241L,
            status = "completed",
            conclusion = "success",
            runStartedAt = "2026-09-08T04:00:00Z",
            updatedAt = "2026-09-08T04:00:35Z"
        )

        assertEquals(35L, run.runTime(Instant.parse("2026-09-08T04:01:00Z"))?.seconds)
        assertEquals("35s", run.runTime(Instant.parse("2026-09-08T04:01:00Z"))?.formatRunTime())
    }

    @Test
    fun runningRun_reportsLiveExecutionTime() {
        val run = WorkflowRun(
            id = 242L,
            status = "in_progress",
            runStartedAt = "2026-09-08T04:00:00Z"
        )

        assertEquals(90L, run.runTime(Instant.parse("2026-09-08T04:01:30Z"))?.seconds)
        assertEquals("1m 30s", run.runTime(Instant.parse("2026-09-08T04:01:30Z"))?.formatRunTime())
    }

    @Test
    fun queuedRun_withoutStartTime_isNotReportedAsRunning() {
        val run = WorkflowRun(id = 243L, status = "queued")

        assertEquals(null, run.runTime(Instant.parse("2026-09-08T04:01:30Z")))
    }

    @Test
    fun queuedRun_withTimestamp_stillReportsNotStarted() {
        val run = WorkflowRun(
            id = 244L,
            status = "queued",
            runStartedAt = "2026-09-08T04:00:00Z",
            updatedAt = "2026-09-08T04:00:20Z"
        )

        assertEquals(null, run.runTime(Instant.parse("2026-09-08T04:01:30Z")))
    }
}
