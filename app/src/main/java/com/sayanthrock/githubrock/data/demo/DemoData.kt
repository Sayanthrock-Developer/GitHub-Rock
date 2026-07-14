package com.sayanthrock.githubrock.data.demo

import com.sayanthrock.githubrock.core.model.*

object DemoData {
    val profile = GitHubUser(
        login = "sayanth-demo",
        id = -1,
        name = "Sayanth Rock",
        bio = "Demo workspace — no GitHub account data is used.",
        publicRepos = 12
    )

    val repositories = listOf(
        repository(1, "GitHub-Rock", "Premium GitHub developer control centre", "Kotlin", 386),
        repository(2, "Rock-Wedding", "Digital wedding invitation studio", "TypeScript", 174, isTemplate = true),
        repository(3, "OTA-ROCK", "Android OTA update explorer", "Kotlin", 92),
        repository(4, "Rock-Screen", "Device mockup and screenshot studio", "Kotlin", 61)
    )

    val workflows = listOf(
        WorkflowRun(101, "Android CI", "Build debug APK", "completed", "success", "push", "main"),
        WorkflowRun(102, "Android Release", "Release AAB", "in_progress", null, "workflow_dispatch", "release/0.1"),
        WorkflowRun(103, "Checks", "Lint and tests", "completed", "failure", "pull_request", "feature/editor")
    )

    val issues = listOf(
        GitHubIssue(1, 24, "Add workflow input presets", "open", user = Owner("octo-demo"), labels = listOf(GitHubLabel("enhancement", "2F81F7"))),
        GitHubIssue(2, 19, "Improve tablet repository layout", "open", user = Owner("sayanth-demo"), labels = listOf(GitHubLabel("ui", "A371F7")))
    )

    val pulls = listOf(
        PullRequestSummary(1, 31, "Add APK certificate comparison", "open", draft = false, user = Owner("octo-demo")),
        PullRequestSummary(2, 28, "Refine device-flow error states", "open", draft = true, user = Owner("sayanth-demo"))
    )

    val releases = listOf(
        Release(1, "v0.1.0", "GitHub Rock Alpha", "First functional Android build", assets = listOf(
            ReleaseAsset(1, "github-rock-debug.apk", 18_450_000, "https://example.invalid/github-rock-debug.apk")
        ))
    )

    val contents = listOf(
        ContentEntry(".github", ".github", "demo-1", type = "dir"),
        ContentEntry("app", "app", "demo-2", type = "dir"),
        ContentEntry("build.gradle.kts", "build.gradle.kts", "demo-3", 850, "file"),
        ContentEntry("README.md", "README.md", "demo-4", 4_250, "file")
    )

    private fun repository(
        id: Long,
        name: String,
        description: String,
        language: String,
        stars: Int,
        isTemplate: Boolean = false
    ) = GitHubRepositoryModel(
        id = -id,
        name = name,
        fullName = "SayanthRock/$name",
        owner = Owner(
            login = "SayanthRock",
            avatarUrl = "https://avatars.githubusercontent.com/u/202829406?v=4"
        ),
        description = description,
        isTemplate = isTemplate,
        htmlUrl = "https://github.com/SayanthRock/$name",
        cloneUrl = "https://github.com/SayanthRock/$name.git",
        language = language,
        stars = stars,
        forks = stars / 9,
        openIssues = id.toInt() * 3,
        updatedAt = "2026-07-11T10:00:00Z",
        topics = listOf("android", "github", if (isTemplate) "template" else "open-source")
    )
}
