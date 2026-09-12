package com.sayanthrock.githubrock.ui

import androidx.compose.foundation.background
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
import com.sayanthrock.githubrock.ui.navigation.MainNavigationV2
import com.sayanthrock.githubrock.ui.navigation.RockNavigationChrome
import com.sayanthrock.githubrock.ui.screens.AppearanceViewModel
import com.sayanthrock.githubrock.ui.screens.LoginScreenV2
import com.sayanthrock.githubrock.ui.screens.PremiumSetupScreen
import com.sayanthrock.githubrock.data.settings.NavigationBarStyle
import kotlinx.coroutines.launch

@Composable
fun GitHubRockRoot(viewModel: MainViewModel = hiltViewModel(), appearanceViewModel: AppearanceViewModel = hiltViewModel(), setupViewModel: GitHubRockSetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appearanceState by appearanceViewModel.state.collectAsStateWithLifecycle()
    val blurState by appearanceViewModel.blurState.collectAsStateWithLifecycle()
    val setupComplete by setupViewModel.setupComplete.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (!setupComplete) {
        PremiumSetupScreen(
            appearance = appearanceState,
            onThemeMode = appearanceViewModel::setThemeMode,
            onThemeStyle = appearanceViewModel::setThemeStyle,
            onTrueBlack = appearanceViewModel::setTrueBlack,
            onSetupComplete = setupViewModel::completeSetup
        )
        return
    }
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars)) {
        if (state.mode == null) {
            LoginScreenV2(configured = viewModel.loginConfigured, loading = state.isLoading, auth = state.auth, onLogin = viewModel::startLogin, onOpenGitHubUrl = openGitHubUrl, onCheckAuthorization = viewModel::checkLoginStatus, onGuest = viewModel::continueAsGuest)
        } else {
            CompositionLocalProvider(LocalOpenGitHubProfile provides openNativeProfile) {
                Box(Modifier.fillMaxSize()) {
                    NavigationContent(navController = navController, bottomContentPadding = navigationContentInset(appearanceState.navigationBarStyle)) {
                        MainNavigationV2(navController, state, viewModel::searchRepositories, viewModel::inspectProfile, viewModel::rememberRepository, openGitHubUrl, viewModel::refresh, viewModel::logout)
                    }
                    RockNavigationChrome(navController = navController, style = appearanceState.navigationBarStyle, animationStyle = appearanceState.animationStyle, reduceMotion = appearanceState.reduceMotion, blurSettings = blurState, modifier = Modifier.fillMaxSize())
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navigationBarPadding))
    }
}

/** Reserve space for the fixed navigation chrome without changing the scroll direction or page content gestures. */
private fun navigationContentInset(style: NavigationBarStyle): androidx.compose.ui.unit.Dp = when (style) {
    NavigationBarStyle.FloatingCapsule -> 102.dp
    NavigationBarStyle.Classic -> 102.dp
    NavigationBarStyle.Glass -> 100.dp
    NavigationBarStyle.Minimal -> 72.dp
    NavigationBarStyle.Compact -> 74.dp
    NavigationBarStyle.Ios -> 94.dp
}

@Composable
private fun NavigationContent(navController: androidx.navigation.NavHostController, bottomContentPadding: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(bottom = bottomContentPadding)) { content() }
}
