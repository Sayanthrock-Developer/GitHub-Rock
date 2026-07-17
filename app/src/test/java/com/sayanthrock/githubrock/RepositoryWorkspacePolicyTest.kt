package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.util.RepositoryHealthState
import com.sayanthrock.githubrock.core.util.RepositoryWorkspacePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositoryWorkspacePolicyTest {
    @Test
    fun `loading progress moves from zero to one hundred by completed phase`() {
        assertEquals(
            0,
            RepositoryWorkspacePolicy.loadProgress(
                repositoryReady = false,
                releasesLoading = true,
                readmeLoading = true
            )
        )
        assertEquals(
            33,
            RepositoryWorkspacePolicy.loadProgress(
                repositoryReady = true,
                releasesLoading = true,
                readmeLoading = true
            )
        )
        assertEquals(
            66,
            RepositoryWorkspacePolicy.loadProgress(
                repositoryReady = true,
                releasesLoading = false,
                readmeLoading = true
            )
        )
        assertEquals(
            100,
            RepositoryWorkspacePolicy.loadProgress(
                repositoryReady = true,
                releasesLoading = false,
                readmeLoading = false
            )
        )
    }

    @Test
    fun `open issues are shown as problems and clean repositories are healthy`() {
        assertEquals(RepositoryHealthState.Healthy, RepositoryWorkspacePolicy.issueHealth(0))
        assertEquals(RepositoryHealthState.Problem, RepositoryWorkspacePolicy.issueHealth(1))
        assertEquals(RepositoryHealthState.Problem, RepositoryWorkspacePolicy.issueHealth(42))
    }
}
