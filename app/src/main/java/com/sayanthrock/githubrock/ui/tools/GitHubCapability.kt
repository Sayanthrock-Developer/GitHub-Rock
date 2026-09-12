package com.sayanthrock.githubrock.ui.tools

/**
 * Capability state is intentionally separate from whether a raw GitHub endpoint exists.
 * A capability is only usable when GitHub Rock has a real UI/action path for it.
 */
enum class GitHubCapabilityState {
    Available,
    PermissionRequired,
    Partial,
    Unsupported,
    Unauthenticated,
    Error
}

enum class GitHubCapability(
    val title: String,
    val group: Group,
    val state: GitHubCapabilityState,
    val reason: String? = null
) {
    Code("Code", Group.Repository, GitHubCapabilityState.Available),
    Branches("Branches", Group.Repository, GitHubCapabilityState.Partial, "Branch browsing is available; branch-management coverage is still being completed."),
    Commits("Commits", Group.Repository, GitHubCapabilityState.Partial, "Commit data is available through the repository layer, but the dedicated commit workspace is incomplete."),
    Tags("Tags", Group.Repository, GitHubCapabilityState.Partial, "Tag support exists in the API layer but does not yet have a complete native workspace."),
    Releases("Releases", Group.Repository, GitHubCapabilityState.Available),
    Issues("Issues", Group.Repository, GitHubCapabilityState.Available),
    PullRequests("Pull requests", Group.Repository, GitHubCapabilityState.Available),
    Discussions("Discussions", Group.Repository, GitHubCapabilityState.Unsupported, "No native Discussions implementation is currently present."),
    Projects("Projects", Group.Repository, GitHubCapabilityState.Unsupported, "No native Projects implementation is currently present."),

    Workflows("Workflows", Group.Actions, GitHubCapabilityState.Available),
    Runs("Runs", Group.Actions, GitHubCapabilityState.Available),
    Jobs("Jobs", Group.Actions, GitHubCapabilityState.Available),
    Logs("Logs", Group.Actions, GitHubCapabilityState.Available),
    Artifacts("Artifacts", Group.Actions, GitHubCapabilityState.Available),
    Dispatch("Dispatch", Group.Actions, GitHubCapabilityState.PermissionRequired, "Requires Actions write permission and a workflow that supports dispatch."),
    Rerun("Rerun", Group.Actions, GitHubCapabilityState.PermissionRequired, "Requires Actions write permission."),
    Cancel("Cancel", Group.Actions, GitHubCapabilityState.PermissionRequired, "Requires Actions write permission."),

    Comments("Comments", Group.Collaboration, GitHubCapabilityState.Available),
    Reviews("Reviews", Group.Collaboration, GitHubCapabilityState.Available),
    ReviewThreads("Review threads", Group.Collaboration, GitHubCapabilityState.Partial, "Review-thread data and actions exist, but the full native thread workspace is incomplete."),
    Reactions("Reactions", Group.Collaboration, GitHubCapabilityState.Partial, "Supported for existing issue/PR surfaces; not yet exposed as one unified collaboration workspace."),
    Labels("Labels", Group.Collaboration, GitHubCapabilityState.Available),
    Milestones("Milestones", Group.Collaboration, GitHubCapabilityState.Partial, "Repository API support exists but a dedicated native milestone workspace is incomplete."),

    RepositorySearch("Repository search", Group.Discovery, GitHubCapabilityState.Available),
    CodeSearch("Code search", Group.Discovery, GitHubCapabilityState.Partial, "Search infrastructure exists; complete native code-search UX is still being verified."),
    UserSearch("User search", Group.Discovery, GitHubCapabilityState.Available),
    Organizations("Organizations", Group.Discovery, GitHubCapabilityState.Partial, "Organization membership is available; full organization workspace coverage is incomplete."),

    Profile("Profile", Group.Account, GitHubCapabilityState.Available),
    Notifications("Notifications", Group.Account, GitHubCapabilityState.Available),
    Stars("Stars", Group.Account, GitHubCapabilityState.Partial, "Starred repositories are available through the profile library; full star-management UX is still being verified."),
    Watching("Watching", Group.Account, GitHubCapabilityState.Partial, "Watching support is not yet complete as a native workspace."),
    AccountsOrganizations("Accounts & organizations", Group.Account, GitHubCapabilityState.Available),

    Dependabot("Dependabot", Group.Security, GitHubCapabilityState.PermissionRequired, "Requires repository security permissions and a supported GitHub API/authentication context."),
    CodeScanning("Code scanning", Group.Security, GitHubCapabilityState.PermissionRequired, "Requires repository security permissions and a supported GitHub API/authentication context."),
    SecretScanning("Secret scanning", Group.Security, GitHubCapabilityState.PermissionRequired, "Requires repository security permissions and a supported GitHub API/authentication context."),
    BranchRules("Branch rules", Group.Security, GitHubCapabilityState.Partial, "Branch-protection reads exist, but complete rules administration is not yet implemented."),
    Administration("Administration", Group.Security, GitHubCapabilityState.PermissionRequired, "Requires repository administration permission and supported API access.");

    enum class Group(val title: String) {
        Repository("Repository"),
        Actions("Actions"),
        Collaboration("Collaboration"),
        Discovery("Discovery"),
        Account("Account"),
        Security("Developer & Security")
    }
}

object GitHubCapabilityRegistry {
    val all: List<GitHubCapability> = GitHubCapability.entries
}
