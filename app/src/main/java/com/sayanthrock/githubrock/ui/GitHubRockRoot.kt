package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.ui.components.rockBackground
import com.sayanthrock.githubrock.ui.navigation.MainNavigation
import com.sayanthrock.githubrock.ui.screens.LoginScreen
import kotlinx.coroutines.launch

@Composable
fun GitHubRockRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verificationUri = state.auth.code?.verificationUri
    var awaitingVerificationBrowserReturn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.auth.code == null) {
        if (state.auth.code == null) {
            awaitingVerificationBrowserReturn = false
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (AuthReturnPolicy.shouldCheckAuthorization(
                awaitingVerificationBrowserReturn = awaitingVerificationBrowserReturn,
                hasPendingDeviceCode = state.auth.code != null
            )
        ) {
            awaitingVerificationBrowserReturn = false
            viewModel.checkLoginStatus()
        }
    }

    val openGitHubUrl = remember(context, snackbar, scope, verificationUri) {
        { url: String ->
            val opened = GitHubExternalLinkLauncher.open(context, url)
            if (opened && url == verificationUri) {
                awaitingVerificationBrowserReturn = true
            }
            if (!opened) {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "No browser could open GitHub. Install or enable a browser and try again.",
                        actionLabel = "Retry"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val reopened = GitHubExternalLinkLauncher.open(context, url)
                        if (reopened && url == verificationUri) {
                            awaitingVerificationBrowserReturn = true
                        } else if (!reopened) {
                            snackbar.showSnackbar("GitHub still could not be opened. Check your browser settings.")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().rockBackground()
    ) {
        if (state.mode == null) {
            LoginScreen(
                configured = viewModel.loginConfigured,
                loading = state.isLoading,
                auth = state.auth,
                onLogin = viewModel::startLogin,
                onOpenGitHubUrl = openGitHubUrl,
                onCheckAuthorization = viewModel::checkLoginStatus,
                onGuest = viewModel::continueAsGuest,
                onDemo = viewModel::enterDemo
            )
        } else {
            MainNavigation(
                navController = navController,
                state = state,
                onSearch = viewModel::searchRepositories,
                onRememberRepository = viewModel::rememberRepository,
                onRefreshSocial = viewModel::refreshSocialConnections,
                onLogout = viewModel::logout
            )
        }
        SnackbarHost(snackbar)
    }
}

