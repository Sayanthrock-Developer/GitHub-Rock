package com.sayanthrock.githubrock.core.util

enum class RepositoryHealthState {
    Healthy,
    Problem
}

object RepositoryWorkspacePolicy {
    fun loadProgress(
        repositoryReady: Boolean,
        releasesLoading: Boolean,
        readmeLoading: Boolean
    ): Int {
        val completed = listOf(
            repositoryReady,
            !releasesLoading,
            !readmeLoading
        ).count { it }
        return (completed * 100 / 3).coerceIn(0, 100)
    }

    fun issueHealth(openIssues: Int): RepositoryHealthState =
        if (openIssues > 0) RepositoryHealthState.Problem else RepositoryHealthState.Healthy
}
