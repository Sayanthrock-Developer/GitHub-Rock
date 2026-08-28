package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState

/** Compatibility wrapper: metric navigation is implemented by the Profile card itself. */
@Composable
fun ProfileScreenWithMetricNavigation(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState,
    onInspectProfile: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    onLogout: () -> Unit,
    onOpenRepositories: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        ProfileScreen(
            mode = mode,
            profile = profile,
            explorerState = explorerState,
            onInspectProfile = onInspectProfile,
            onOpenDownloads = onOpenDownloads,
            onOpenFeatures = onOpenFeatures,
            onOpenAccounts = onOpenAccounts,
            onOpenSettings = onOpenSettings,
            onOpenAppInfo = onOpenAppInfo,
            onOpenGitHubUrl = onOpenGitHubUrl,
            onOpenRepository = onOpenRepository,
            onLogout = onLogout,
            onOpenRepositories = onOpenRepositories,
            onOpenFollowers = onOpenFollowers,
            onOpenFollowing = onOpenFollowing
        )
    }
}
