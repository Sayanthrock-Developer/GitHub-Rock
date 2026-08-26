package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import androidx.navigation.compose.rememberNavController
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.NativeProfileDestination
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.ui.components.LocalOpenGitHubProfile
import com.sayanthrock.githubrock.ui.components.rockBackground
import com.sayanthrock.githubrock.ui.navigation.MainNavigation
import com.sayanthrock.githubrock.ui.navigation.ModernNavigationChrome
import com.sayanthrock.githubrock.ui.screens.LoginScreenV2
import com.sayanthrock.githubrock.ui.screens.SetupGuardScreen
import kotlinx.coroutines.launch

@Composable
fun GitHubRockRoot(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setupPreferences = remember(context) { context.getSharedPreferences("github_rock_setup", android.content.Context.MODE_PRIVATE) }
    var setupComplete by rememberSaveable { mutableStateOf(setupPreferences.getBoolean("setup_complete", false)) }
    if (!setupComplete) { SetupGuardScreen(onSetupComplete = { setupPreferences.edit().putBoolean("setup_complete", true).apply(); setupComplete = true }); return }
    val verificationUri = state.auth.code?.verificationUri
    val authorizationUrl = state.auth.authorizationUrl
    var awaitingVerificationBrowserReturn by rememberSaveable { mutableStateOf(false) }
    var authorizationUrlConsumed by rememberSaveable { mutableStateOf<String?>(null) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(authorizationUrl) {
        val url = authorizationUrl ?: return@LaunchedEffect
        if (authorizationUrlConsumed == url) return@LaunchedEffect
        authorizationUrlConsumed = url
        val opened = GitHubExternalLinkLauncher.openOAuthUrl(context, url)
        if (!opened) snackbar.showSnackbar("Unable to open GitHub sign-in in your browser. Check your browser and try again.")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (AuthReturnPolicy.shouldCheckAuthorization(awaitingVerificationBrowserReturn, state.auth.code != null)) { awaitingVerificationBrowserReturn = false; viewModel.checkLoginStatus() } }
    LaunchedEffect(Unit) { AccountContextRefreshBus.events.collect { viewModel.refresh() } }

    val openGitHubUrl = remember(context, snackbar, scope, verificationUri) { { url: String ->
        val opened = GitHubExternalLinkLauncher.open(context, url)
        if (opened && url == verificationUri) awaitingVerificationBrowserReturn = true
        if (!opened) scope.launch { val result = snackbar.showSnackbar("Unable to open GitHub.", actionLabel = "Retry"); if (result == SnackbarResult.ActionPerformed) GitHubExternalLinkLauncher.open(context, url) }
    } }
    val openNativeProfile = remember(navController) { { login: String -> navController.navigate(NativeProfileDestination(login, NativeProfileSection.Repositories).route) { launchSingleTop = true } } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() } }

    BoxWithConstraints(Modifier.fillMaxSize().rockBackground()) {
        val navigationChromePadding = if (state.mode == null) 20.dp else if (maxWidth < 600.dp) 92.dp else 20.dp
        if (state.mode == null) LoginScreenV2(configured = viewModel.loginConfigured, loading = state.isLoading, auth = state.auth, onLogin = viewModel::startLogin, onOpenGitHubUrl = openGitHubUrl, onCheckAuthorization = viewModel::checkLoginStatus, onGuest = viewModel::continueAsGuest)
        else CompositionLocalProvider(LocalOpenGitHubProfile provides openNativeProfile) { Box(Modifier.fillMaxSize()) { MainNavigation(navController, state, viewModel::searchRepositories, viewModel::inspectProfile, viewModel::rememberRepository, openGitHubUrl, viewModel::refresh, viewModel::logout); ModernNavigationChrome(navController, Modifier.fillMaxSize()) } }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navigationBarPadding + navigationChromePadding))
    }
}
