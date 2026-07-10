package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkflowStatusTest {
    @Test fun mapsRunningAndTerminalStates() {
        assertEquals(WorkflowDisplayState.Running, WorkflowRun(1, status = "in_progress").displayState())
        assertEquals(WorkflowDisplayState.Success, WorkflowRun(2, status = "completed", conclusion = "success").displayState())
        assertEquals(WorkflowDisplayState.Failed, WorkflowRun(3, status = "completed", conclusion = "timed_out").displayState())
        assertEquals(WorkflowDisplayState.Cancelled, WorkflowRun(4, status = "completed", conclusion = "cancelled").displayState())
    }
}

