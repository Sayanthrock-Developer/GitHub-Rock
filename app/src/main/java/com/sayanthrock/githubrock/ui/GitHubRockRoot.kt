package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.sayanthrock.githubrock.ui.components.rockBackground
import com.sayanthrock.githubrock.ui.navigation.MainNavigation
import com.sayanthrock.githubrock.ui.screens.LoginScreen

@Composable
fun GitHubRockRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()

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

