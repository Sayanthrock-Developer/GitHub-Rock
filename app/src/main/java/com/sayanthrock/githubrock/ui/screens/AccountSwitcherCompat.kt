package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sayanthrock.githubrock.ui.AppMode

/**
 * Compatibility overload for the existing navigation graph.
 * The new screen owns account/session management; existing navigation can keep
 * its current callback contract while the user can manually refresh after a context change.
 */
@Composable
fun AccountSwitcherScreen(
    mode: AppMode,
    connectedProfile: com.sayanthrock.githubrock.core.model.GitHubUser?,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onReplaceConnectedAccount: () -> Unit
) {
    val context = LocalContext.current
    AccountSwitcherScreen(
        mode = mode,
        connectedProfile = connectedProfile,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
        onContextChanged = {},
        onLogout = onReplaceConnectedAccount,
        onOpenGitHubUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    )
}
