package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.build.WorkflowMonitorPolicy
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkflowMonitorPolicyTest {
    @Test
    fun `active builds do not produce completion notifications`() {
        assertNull(WorkflowMonitorPolicy.completion(run(status = "in_progress")))
    }

    @Test
    fun `terminal states produce honest notification titles`() {
        val success = WorkflowMonitorPolicy.completion(run(status = "completed", conclusion = "success"))
        val failure = WorkflowMonitorPolicy.completion(run(status = "completed", conclusion = "failure"))
        val cancelled = WorkflowMonitorPolicy.completion(run(status = "completed", conclusion = "cancelled"))

        assertEquals(WorkflowDisplayState.Success, success?.state)
        assertEquals("Android build succeeded", success?.title)
        assertEquals("Android build failed", failure?.title)
        assertEquals("Android build cancelled", cancelled?.title)
    }

    @Test
    fun `a run saved by an older dispatch is ignored`() {
        assertNull(WorkflowMonitorPolicy.persistedRunId(41, setOf(40, 41)))
        assertEquals(42L, WorkflowMonitorPolicy.persistedRunId(42, setOf(40, 41)))
    }

    private fun run(status: String, conclusion: String? = null) = WorkflowRun(
        id = 42,
        name = "Android Build",
        status = status,
        conclusion = conclusion,
        event = "workflow_dispatch",
        headBranch = "main"
    )
}
