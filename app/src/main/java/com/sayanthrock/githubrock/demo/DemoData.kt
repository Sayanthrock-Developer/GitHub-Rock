package com.sayanthrock.githubrock.demo

import com.sayanthrock.githubrock.core.model.RepositorySummary
import com.sayanthrock.githubrock.core.model.WorkflowVisualState

data class DemoWorkflow(
    val name: String,
    val repository: String,
    val branch: String,
    val state: WorkflowVisualState,
    val detail: String
)

object DemoData {
    val repositories = listOf(
        RepositorySummary(
            id = 1001,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = "SayanthRock",
            avatarUrl = null,
            description = "Premium mobile developer control centre for GitHub.",
            isPrivate = false,
            isFork = false,
            htmlUrl = "https://github.com/SayanthRock/GitHub-Rock",
            defaultBranch = "main",
            stars = 128,
            forks = 14,
            openIssues = 6,
            language = "Kotlin",
            updatedAt = "2026-07-10T18:00:00Z"
        ),
        RepositorySummary(
            id = 1002,
            name = "Rock-Wedding",
            fullName = "SayanthRock/Rock-Wedding",
            owner = "SayanthRock",
            avatarUrl = null,
            description = "Digital wedding invitation studio with RSVP and QR sharing.",
            isPrivate = false,
            isFork = false,
            htmlUrl = "https://github.com/SayanthRock/Rock-Wedding",
            defaultBranch = "main",
            stars = 64,
            forks = 8,
            openIssues = 3,
            language = "TypeScript",
            updatedAt = "2026-07-09T16:30:00Z"
        ),
        RepositorySummary(
            id = 1003,
            name = "Battery-Rock",
            fullName = "SayanthRock/Battery-Rock",
            owner = "SayanthRock",
            avatarUrl = null,
            description = "Battery monitoring and device customization experiments.",
            isPrivate = true,
            isFork = false,
            htmlUrl = "https://github.com/SayanthRock/Battery-Rock",
            defaultBranch = "main",
            stars = 22,
            forks = 2,
            openIssues = 9,
            language = "Kotlin",
            updatedAt = "2026-07-08T12:15:00Z"
        )
    )

    val workflows = listOf(
        DemoWorkflow("Android CI", "SayanthRock/GitHub-Rock", "agent/github-rock-v1", WorkflowVisualState.RUNNING, "assembleDebug · 3 of 6 steps"),
        DemoWorkflow("Release APK", "SayanthRock/Battery-Rock", "main", WorkflowVisualState.FAILED, "Unit tests failed"),
        DemoWorkflow("Pages", "SayanthRock/Rock-Wedding", "main", WorkflowVisualState.SUCCESS, "Deployed successfully")
    )
}
