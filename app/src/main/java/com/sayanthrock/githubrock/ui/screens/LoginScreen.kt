package com.sayanthrock.githubrock.ui.screens

import androidx.compose.runtime.Composable
import com.sayanthrock.githubrock.ui.DeviceAuthState

/**
 * Compatibility entry point for callers that still reference the original login screen.
 * The signup flow has been intentionally removed; authentication is handled by LoginScreenV2.
 */
@Composable
fun LoginScreen(
    configured: Boolean,
    loading: Boolean,
    auth: DeviceAuthState,
    onLogin: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onCheckAuthorization: () -> Unit,
    onGuest: () -> Unit,
) {
    LoginScreenV2(
        configured = configured,
        loading = loading,
        auth = auth,
        onLogin = onLogin,
        onOpenGitHubUrl = onOpenGitHubUrl,
        onCheckAuthorization = onCheckAuthorization,
        onGuest = onGuest,
    )
}
