package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.Workflow
import com.sayanthrock.githubrock.core.model.WorkflowRun
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
    fun `fallback to workflow name ignores case`() {
        val expected1 = Workflow(2, "android build", ".github/workflows/other.yml", "active")
        val workflows1 = listOf(expected1)
        assertEquals(expected1, BuildRunTracker.findAndroidWorkflow(workflows1))

        val expected2 = Workflow(2, "aNdRoId BuIlD", ".github/workflows/other.yml", "active")
        val workflows2 = listOf(expected2)
        assertEquals(expected2, BuildRunTracker.findAndroidWorkflow(workflows2))
    }

    @Test
    fun `generated workflow path matches with leading slashes`() {
        val expected1 = Workflow(2, "Custom build", "/.github/workflows/android-build.yml", "active")
        val workflows1 = listOf(expected1)
        assertEquals(expected1, BuildRunTracker.findAndroidWorkflow(workflows1))

        val expected2 = Workflow(2, "Custom build", "//.github/workflows/android-build.yml", "active")
        val workflows2 = listOf(expected2)
        assertEquals(expected2, BuildRunTracker.findAndroidWorkflow(workflows2))
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
    fun `isActive handles different status and conclusion combinations`() {
        assertTrue(BuildRunTracker.isActive(run(id = 1, status = "queued", conclusion = null)))
        assertTrue(BuildRunTracker.isActive(run(id = 1, status = "in_progress", conclusion = null)))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "completed", conclusion = "success")))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "completed", conclusion = "failure")))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "completed", conclusion = "cancelled")))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "in_progress", conclusion = "success"))) // even if status is not completed, if conclusion is set, it's not active based on current logic (though unlikely from GitHub API)
    }

    @Test
    fun `tracking and ref safety follow Git ref rules`() {
        assertTrue(BuildRunTracker.isActive(run(id = 1, status = "queued")))
        assertFalse(BuildRunTracker.isActive(run(id = 1, status = "completed", conclusion = "success")))
        assertTrue(BuildRunTracker.isSafeRef("release/1.2.0"))
        assertTrue(BuildRunTracker.isSafeRef("feature/foo+bar"))
        assertTrue(BuildRunTracker.isSafeRef("feature/foo@bar"))
        assertFalse(BuildRunTracker.isSafeRef("release/../main"))
        assertFalse(BuildRunTracker.isSafeRef("refs heads/main"))
        assertFalse(BuildRunTracker.isSafeRef("feature/.hidden"))
        assertFalse(BuildRunTracker.isSafeRef("feature/trailing."))
        assertFalse(BuildRunTracker.isSafeRef("feature/build.lock"))
        assertFalse(BuildRunTracker.isSafeRef("feature/build.LOCK"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo@{bar"))
        assertFalse(BuildRunTracker.isSafeRef("@"))
        assertFalse(BuildRunTracker.isSafeRef(""))
        assertFalse(BuildRunTracker.isSafeRef("   "))

        // Forbidden characters
        assertFalse(BuildRunTracker.isSafeRef("feature/foo~bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo^bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo:bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo?bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo*bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo[bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo\\bar"))

        // Control characters
        assertFalse(BuildRunTracker.isSafeRef("feature/foo\u0000bar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo\u001Fbar"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo\u007Fbar"))

        // Leading/trailing slashes
        assertFalse(BuildRunTracker.isSafeRef("/feature/foo"))
        assertFalse(BuildRunTracker.isSafeRef("feature/foo/"))
        assertFalse(BuildRunTracker.isSafeRef("//feature/foo"))
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
