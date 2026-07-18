package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.WorkflowLogHighlighter
import com.sayanthrock.githubrock.core.util.WorkflowLogTokenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowLogHighlighterTest {
    @Test fun actionsLogTokensExposeStatusAndTimestampSemantics() {
        val line = "2026-07-18T15:31:45Z ##[error] BUILD FAILED with exception"
        val kinds = WorkflowLogHighlighter.highlight(line).map { it.kind }

        assertTrue(WorkflowLogTokenKind.Timestamp in kinds)
        assertTrue(WorkflowLogTokenKind.Error in kinds)
    }

    @Test fun summaryCountsLinesWarningsAndErrors() {
        val summary = WorkflowLogHighlighter.summary("ok success\n##[warning] slow\n##[error] failed")

        assertEquals(3, summary.lines)
        assertEquals(1, summary.warnings)
        assertEquals(1, summary.errors)
    }
}
