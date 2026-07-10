package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.WorkflowStatusMapper
import com.sayanthrock.githubrock.core.model.WorkflowVisualState
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkflowStatusTest {
    @Test
    fun `in progress maps to running`() {
        assertEquals(WorkflowVisualState.RUNNING, WorkflowStatusMapper.map("in_progress", null))
    }

    @Test
    fun `failure conclusions map to failed`() {
        assertEquals(WorkflowVisualState.FAILED, WorkflowStatusMapper.map("completed", "failure"))
        assertEquals(WorkflowVisualState.FAILED, WorkflowStatusMapper.map("completed", "timed_out"))
    }

    @Test
    fun `success conclusion maps to success`() {
        assertEquals(WorkflowVisualState.SUCCESS, WorkflowStatusMapper.map("completed", "success"))
    }
}
