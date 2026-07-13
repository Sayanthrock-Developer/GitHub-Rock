package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
    val openGitHubUrl = remember(context, snackbar, scope) {
        { url: String ->
            if (!GitHubExternalLinkLauncher.open(context, url)) {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "No browser could open GitHub. Install or enable a browser and try again.",
                        actionLabel = "Retry"
                    )
                    if (result == SnackbarResult.ActionPerformed &&
                        !GitHubExternalLinkLauncher.open(context, url)
                    ) {
                        snackbar.showSnackbar("GitHub still could not be opened. Check your browser settings.")
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
                onGuest = viewModel::continueAsGuest,
                onDemo = viewModel::enterDemo
            )
        } else {
            MainNavigation(
                navController = navController,
                state = state,
                onSearch = viewModel::searchRepositories,
                onRememberRepository = viewModel::rememberRepository,
                onLogout = viewModel::logout
            )
        }
        SnackbarHost(snackbar)
    }
}

