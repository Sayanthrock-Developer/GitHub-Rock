package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRunTrackerTest {
    @Test
    fun `generated workflow path wins over a similarly named workflow`() {
        val expected = Workflow(2, "Custom build", ".github/workflows/android-build.yml", "active")
        val workflows = listOf(
            Workflow(1, "Android Build", ".github/workflows/legacy.yml", "active"),
            expected
        )

        assertEquals(expected, BuildRunTracker.findAndroidWorkflow(workflows))
    }

    @Test
    fun `new dispatch is matched by id event and ref`() {
        val matching = run(id = 12, branch = "main")
        val runs = listOf(
            run(id = 13, branch = "feature"),
            matching,
            run(id = 10, branch = "main")
        )

        assertEquals(matching, BuildRunTracker.findDispatchedRun(runs, setOf(10), "main"))
        assertNull(BuildRunTracker.findDispatchedRun(runs, setOf(10, 12), "main"))
    }

    @Test
    fun `disabled generated workflow is not dispatchable`() {
        val disabled = Workflow(2, "Custom build", ".github/workflows/android-build.yml", "disabled_manually")

        assertNull(BuildRunTracker.findAndroidWorkflow(listOf(disabled)))
    }

    @Test
    fun `tracking and ref safety reject terminal or unsafe values`() {
        assertTrue(BuildRunTracker.isActive(run(id = 1, status = "queued")))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "completed", conclusion = "success")))
        assertTrue(BuildRunTracker.isSafeRef("release/1.2.0"))
        assertFalse(BuildRunTracker.isSafeRef("release/../main"))
        assertFalse(BuildRunTracker.isSafeRef("refs heads/main"))
    }

    private fun run(
        id: Long,
        branch: String = "main",
        status: String = "queued",
        conclusion: String? = null
    ) = WorkflowRun(
        id = id,
        name = "Android Build",
        status = status,
        conclusion = conclusion,
        event = "workflow_dispatch",
        headBranch = branch
    )
}
