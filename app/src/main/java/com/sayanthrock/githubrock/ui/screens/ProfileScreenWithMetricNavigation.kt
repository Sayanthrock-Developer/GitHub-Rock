package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState

/**
 * Keeps the existing ProfileScreen intact while making the three profile metrics
 * actionable. The metric hit targets sit over the existing metric row and route
 * into the native profile screen.
 */
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
            onLogout = onLogout
        )

        if (profile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .height(64.dp)
                    .padding(top = 0.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                MetricHitTarget(
                    label = "Repositories",
                    onClick = onOpenRepositories,
                    modifier = Modifier.weight(1f)
                )
                MetricHitTarget(
                    label = "Followers",
                    onClick = onOpenFollowers,
                    modifier = Modifier.weight(1f)
                )
                MetricHitTarget(
                    label = "Following",
                    onClick = onOpenFollowing,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricHitTarget(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick)
            .then(Modifier),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Spacer(
            Modifier.fillMaxSize()
        )
    }
}
