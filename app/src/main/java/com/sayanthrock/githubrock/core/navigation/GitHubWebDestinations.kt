package com.sayanthrock.githubrock.core.navigation

import java.util.Locale

data class GitHubWebDestination(
    val id: String,
    val title: String,
    val description: String,
    val url: String
)

data class GitHubWebSection(
    val id: String,
    val title: String,
    val description: String,
    val destinations: List<GitHubWebDestination>
)

const val GITHUB_HOME_URL = "https://github.com/"

private val githubLoginPattern =
    Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?")

internal fun normalizedGitHubLogin(login: String?): String? =
    login?.trim()?.takeIf { githubLoginPattern.matches(it) }

fun githubWebSections(login: String?): List<GitHubWebSection> {
    val safeLogin = normalizedGitHubLogin(login)
    val profileUrl = safeLogin?.let { "https://github.com/$it" }
        ?: "https://github.com/settings/profile"
    val repositoriesUrl = safeLogin?.let { "https://github.com/$it?tab=repositories" }
        ?: "https://github.com/dashboard?tab=repositories"
    val projectsUrl = safeLogin?.let { "https://github.com/$it?tab=projects" }
        ?: "https://github.com/features/issues"
    val packagesUrl = safeLogin?.let { "https://github.com/$it?tab=packages" }
        ?: "https://github.com/features/packages"
    val gistsUrl = safeLogin?.let { "https://gist.github.com/$it" }
        ?: "https://gist.github.com/"

    return listOf(
        GitHubWebSection(
            id = "your-github",
            title = "Your GitHub",
            description = "The account-wide work queues and pages you use every day.",
            destinations = listOf(
                destination("dashboard", "Dashboard", "Feed, recent activity, and repository shortcuts.", GITHUB_HOME_URL),
                destination("notifications", "Notifications", "Mentions, assignments, reviews, and subscribed activity.", "https://github.com/notifications"),
                destination("pull-requests", "Pull requests", "Pull requests created by you, assigned to you, or awaiting review.", "https://github.com/pulls"),
                destination("issues", "Issues", "Your assigned, created, and mentioned issues.", "https://github.com/issues"),
                destination("profile", "Profile", "Your contribution activity and public profile.", profileUrl),
                destination("repositories", "Repositories", "All repositories available to your account.", repositoriesUrl),
                destination("stars", "Stars", "Saved repositories, topics, and curated lists.", "https://github.com/stars")
            )
        ),
        GitHubWebSection(
            id = "create-code",
            title = "Create & code",
            description = "Create repositories, search code, and use GitHub's cloud development tools.",
            destinations = listOf(
                destination("new-repository", "New repository", "Create a public, private, or template-based repository.", "https://github.com/new"),
                destination("code-search", "Code search", "Search code, repositories, issues, pull requests, and users.", "https://github.com/search?type=code"),
                destination("codespaces", "Codespaces", "Create and manage browser-based cloud development environments.", "https://github.com/codespaces"),
                destination("copilot", "GitHub Copilot", "Open Copilot chat and coding-agent experiences available to your plan.", "https://github.com/copilot"),
                destination("copilot-settings", "Copilot settings", "Manage suggestions, coding agents, policies, and Copilot preferences.", "https://github.com/settings/copilot"),
                destination("models", "GitHub Models", "Explore and compare AI models available through GitHub.", "https://github.com/marketplace/models"),
                destination("gists", "Gists", "Create and manage shareable code snippets and notes.", gistsUrl),
                destination("packages", "Packages", "View packages published by your account.", packagesUrl)
            )
        ),
        GitHubWebSection(
            id = "plan-collaborate",
            title = "Plan & collaborate",
            description = "Coordinate work across projects, communities, and organizations.",
            destinations = listOf(
                destination("projects", "Projects", "Plan work in tables, boards, roadmaps, and custom views.", projectsUrl),
                destination("discussions", "Discussions", "Participate in community conversations across GitHub.", "https://github.com/discussions"),
                destination("organizations", "Organizations", "Open organizations and switch between shared workspaces.", "https://github.com/settings/organizations"),
                destination("enterprises", "Enterprises", "Open enterprise accounts and managed organization workspaces.", "https://github.com/settings/enterprises"),
                destination("installations", "GitHub App installations", "Review apps installed for personal and organization repositories.", "https://github.com/settings/installations"),
                destination("sponsors", "Sponsors dashboard", "Manage sponsorships and supported maintainers.", "https://github.com/sponsors/dashboard")
            )
        ),
        GitHubWebSection(
            id = "automate-extend",
            title = "Automate & extend",
            description = "Discover Actions, apps, integrations, and developer configuration.",
            destinations = listOf(
                destination("actions-marketplace", "Actions Marketplace", "Find reusable automation for CI, release, and deployment workflows.", "https://github.com/marketplace?type=actions"),
                destination("apps-marketplace", "Apps Marketplace", "Install integrations for project management, quality, and delivery.", "https://github.com/marketplace?type=apps"),
                destination("marketplace", "Marketplace", "Browse all GitHub apps, Actions, and developer tools.", "https://github.com/marketplace"),
                destination("developer-apps", "Developer settings", "Create and manage GitHub Apps and OAuth apps.", "https://github.com/settings/apps"),
                destination("tokens", "Access tokens", "Create and revoke personal access tokens on GitHub's secure settings page.", "https://github.com/settings/tokens")
            )
        ),
        GitHubWebSection(
            id = "security",
            title = "Security",
            description = "Keep account credentials, sessions, applications, and vulnerability data on GitHub.",
            destinations = listOf(
                destination("security-overview", "Security", "Explore GitHub security products and guidance.", "https://github.com/security"),
                destination("advisories", "Advisory database", "Search CVEs and GitHub-reviewed security advisories.", "https://github.com/advisories"),
                destination("account-security", "Password and authentication", "Manage password, passkeys, two-factor authentication, and recovery.", "https://github.com/settings/security"),
                destination("ssh-keys", "SSH and GPG keys", "Manage trusted signing and repository-access keys.", "https://github.com/settings/keys"),
                destination("applications", "Authorized applications", "Review OAuth apps, GitHub Apps, and authorized integrations.", "https://github.com/settings/applications"),
                destination("sessions", "Sessions", "Review and revoke active browser and device sessions.", "https://github.com/settings/sessions")
            )
        ),
        GitHubWebSection(
            id = "account-community",
            title = "Account & community",
            description = "Personalize GitHub, manage plan details, and discover the wider community.",
            destinations = listOf(
                destination("profile-settings", "Settings", "Edit public profile details, account preferences, and contribution visibility.", "https://github.com/settings/profile"),
                destination("email-settings", "Email settings", "Manage addresses and commit-email privacy.", "https://github.com/settings/emails"),
                destination("notification-settings", "Notification settings", "Choose email, web, and mobile notification delivery.", "https://github.com/settings/notifications"),
                destination("billing", "Billing and plans", "Manage plan, usage, payment, and spending limits directly on GitHub.", "https://github.com/settings/billing"),
                destination("appearance", "Appearance", "Choose GitHub theme, tab size, and display preferences.", "https://github.com/settings/appearance"),
                destination("accessibility", "Accessibility", "Configure motion, keyboard shortcuts, and accessible GitHub experiences.", "https://github.com/settings/accessibility"),
                destination("feature-preview", "Feature preview", "Review and opt into GitHub features available for preview.", "https://github.com/settings/feature_preview"),
                destination("enterprise-trial", "Try Enterprise", "Start an official GitHub Enterprise trial.", "https://github.com/enterprise/trial/start"),
                destination("github-free", "GitHub Free", "Compare Free, Team, and Enterprise plans on GitHub.", "https://github.com/pricing"),
                destination("explore", "Explore", "Discover recommended repositories and developers.", "https://github.com/explore"),
                destination("trending", "Trending", "See repositories and developers gaining attention now.", "https://github.com/trending"),
                destination("topics", "Topics", "Browse repositories grouped by technology or interest.", "https://github.com/topics"),
                destination("education", "GitHub Education", "Open student, teacher, and classroom programs.", "https://github.com/education")
            )
        )
    )
}

fun allGitHubWebDestinations(login: String?): List<GitHubWebDestination> =
    githubWebSections(login).flatMap(GitHubWebSection::destinations)

internal fun filterGitHubWebSections(
    sections: List<GitHubWebSection>,
    query: String
): List<GitHubWebSection> {
    val terms = query.trim()
        .lowercase(Locale.ROOT)
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
    if (terms.isEmpty()) return sections

    return sections.mapNotNull { section ->
        if (matchesEveryTerm(terms, section.title, section.description)) {
            section
        } else {
            val matches = section.destinations.filter { destination ->
                matchesEveryTerm(
                    terms,
                    destination.title,
                    destination.description,
                    destination.id
                )
            }
            section.copy(destinations = matches).takeIf { matches.isNotEmpty() }
        }
    }
}

private fun matchesEveryTerm(terms: List<String>, vararg values: String): Boolean {
    val searchable = values.joinToString(separator = "\n").lowercase(Locale.ROOT)
    return terms.all(searchable::contains)
}

private fun destination(
    id: String,
    title: String,
    description: String,
    url: String
) = GitHubWebDestination(id, title, description, url)
