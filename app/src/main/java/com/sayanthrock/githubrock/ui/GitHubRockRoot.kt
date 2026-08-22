package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.NativeProfileDestination
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.ui.components.LocalOpenGitHubProfile
import com.sayanthrock.githubrock.ui.components.rockBackground
import com.sayanthrock.githubrock.ui.navigation.AdaptiveNavigationOverlay
import com.sayanthrock.githubrock.ui.navigation.MainNavigation
import com.sayanthrock.githubrock.ui.screens.AppearanceViewModel
import com.sayanthrock.githubrock.ui.screens.LoginScreen
import com.sayanthrock.githubrock.ui.screens.SetupGuardScreen
import kotlinx.coroutines.launch

@Composable
fun GitHubRockRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appearanceViewModel: AppearanceViewModel = hiltViewModel()
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setupPreferences = remember(context) {
        context.getSharedPreferences("github_rock_setup", android.content.Context.MODE_PRIVATE)
    }
    var setupComplete by rememberSaveable {
        mutableStateOf(setupPreferences.getBoolean("setup_complete", false))
    }

    if (!setupComplete) {
        SetupGuardScreen(
            onSetupComplete = {
                setupPreferences.edit().putBoolean("setup_complete", true).apply()
                setupComplete = true
            }
        )
        return
    }

    val verificationUri = state.auth.code?.verificationUri
    var awaitingVerificationBrowserReturn by rememberSaveable { mutableStateOf(false) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(state.auth.code == null) {
        if (state.auth.code == null) awaitingVerificationBrowserReturn = false
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

    LaunchedEffect(Unit) {
        AccountContextRefreshBus.events.collect { viewModel.refresh() }
    }

    val openGitHubUrl = remember(context, snackbar, scope, verificationUri) {
        { url: String ->
            val opened = GitHubExternalLinkLauncher.open(context, url)
            if (opened && url == verificationUri) awaitingVerificationBrowserReturn = true
            if (!opened) {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "No browser could open GitHub. Install or enable a browser and try again.",
                        actionLabel = "Retry"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val reopened = GitHubExternalLinkLauncher.open(context, url)
                        if (reopened && url == verificationUri) awaitingVerificationBrowserReturn = true
                        else if (!reopened) snackbar.showSnackbar("GitHub still could not be opened. Check your browser settings.")
                    }
                }
            }
        }
    }

    val openNativeProfile = remember(navController) {
        { login: String ->
            navController.navigate(NativeProfileDestination(login, NativeProfileSection.Repositories).route) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().rockBackground()) {
        val navigationChromePadding = when {
            state.mode == null -> 20.dp
            maxWidth < 600.dp -> 80.dp
            else -> 20.dp
        }
        val currentRoute = currentEntry?.destination?.route
        val showAdaptiveNavigation = state.mode != null && maxWidth < 600.dp && currentRoute in setOf(
            "home", "repositories", "builds", "downloads", "profile"
        )

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
            CompositionLocalProvider(LocalOpenGitHubProfile provides openNativeProfile) {
                MainNavigation(
                    navController = navController,
                    state = state,
                    onSearch = viewModel::searchRepositories,
                    onInspectProfile = viewModel::inspectProfile,
                    onRememberRepository = viewModel::rememberRepository,
                    onOpenGitHubUrl = openGitHubUrl,
                    onRefresh = viewModel::refresh,
                    onLogout = viewModel::logout
                )
            }
        }

        if (showAdaptiveNavigation) {
            AdaptiveNavigationOverlay(
                selectedRoute = currentRoute,
                style = appearance.navigationStyle,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = navigationBarPadding + navigationChromePadding)
        )
    }
}
