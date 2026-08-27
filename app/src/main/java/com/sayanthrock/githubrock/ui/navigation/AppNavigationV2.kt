package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sayanthrock.githubrock.core.navigation.NativeProfileDestination
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.core.navigation.nativeProfileDestination
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.MainUiState
import com.sayanthrock.githubrock.ui.screens.*

sealed class TopDestinationV2(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val accessibilityLabel: String = label) {
    data object Home : TopDestinationV2("home", "Home", Icons.Default.Home)
    data object Repositories : TopDestinationV2("repositories", "Repos", Icons.Default.Folder, "Repositories")
    data object Builds : TopDestinationV2("builds", "Builds", Icons.Default.Build)
    data object Downloads : TopDestinationV2("downloads", "Downloads", Icons.Default.Download)
    data object Profile : TopDestinationV2("profile", "Profile", Icons.Default.AccountCircle)
}

private const val FEATURES_PREVIEW_ROUTE = "features-preview"
private const val SETTINGS_ROUTE = "settings"
private const val APP_CUSTOMIZATION_ROUTE = "app-customization"
private const val APP_INFORMATION_ROUTE = "app-information"
private const val ACCOUNT_SWITCHER_ROUTE = "accounts-organizations"
private const val BUILD_DETAILS_ROUTE = "build-details/{owner}/{repo}/{runId}"
private const val NATIVE_PROFILE_ROUTE = "native-profile/{login}/{section}"

@Composable
fun MainNavigationV2(
    navController: NavHostController, state: MainUiState,
    onSearch: (com.sayanthrock.githubrock.core.model.RepositorySearchOptions) -> Unit,
    onInspectProfile: (String) -> Unit,
    onRememberRepository: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit,
    onOpenGitHubUrl: (String) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit
) {
    val mode = requireNotNull(state.mode)
    val openRepo: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit = { repo -> onRememberRepository(repo); navController.navigate("repo/${repo.owner.login}/${repo.name}") }
    val openNativeProfile: (String, NativeProfileSection) -> Unit = { login, section -> navController.navigate(NativeProfileDestination(login, section).route) { launchSingleTop = true } }
    val openAccountProfile: (String) -> Unit = { login -> openNativeProfile(login, NativeProfileSection.Repositories) }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController, TopDestinationV2.Home.route, Modifier.fillMaxSize().widthIn(max = 1200.dp)) {
            composable(TopDestinationV2.Home.route) { HomeScreen(state.repositories, openRepo, state.isLoading, state.isRefreshing, onRefresh) }
            composable(TopDestinationV2.Repositories.route) { RepositoriesScreen(state.repositories, state.isLoading, onSearch, mode == AppMode.Connected, openRepo, state.profile?.login) }
            composable(TopDestinationV2.Builds.route) { BuildsScreen(mode = mode, repositories = state.repositories, runs = state.workflowRuns, onSelectRepository = openRepo) { repo, run -> navController.navigate("build-details/${repo.owner.login}/${repo.name}/${run.id}") } }
            composable(BUILD_DETAILS_ROUTE, arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType })) { e ->
                val owner = e.arguments?.getString("owner").orEmpty(); val repo = e.arguments?.getString("repo").orEmpty(); val runId = e.arguments?.getLong("runId") ?: 0L
                val repository = state.repositories.firstOrNull { it.owner.login.equals(owner, true) && it.name == repo }
                if (repository != null && runId > 0) BuildDetailsScreen(mode, repository, runId, navController::navigateUp) else Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Build unavailable"); Button(onClick = navController::navigateUp) { Text("Back to Builds") } }
            }
            composable(TopDestinationV2.Downloads.route) { DownloadsHubScreen() }
            composable(TopDestinationV2.Profile.route) { ProfileScreen(mode = mode, profile = state.profile, explorerState = state.profileExplorer, onInspectProfile = onInspectProfile, onOpenDownloads = { navController.navigate(TopDestinationV2.Downloads.route) { launchSingleTop = true } }, onOpenFeatures = { navController.navigate(FEATURES_PREVIEW_ROUTE) }, onOpenAccounts = { navController.navigate(ACCOUNT_SWITCHER_ROUTE) }, onOpenSettings = { navController.navigate(SETTINGS_ROUTE) }, onOpenAppInfo = { navController.navigate(APP_INFORMATION_ROUTE) }, onOpenGitHubUrl = { url -> nativeProfileDestination(url)?.let { openNativeProfile(it.login, it.section) } ?: onOpenGitHubUrl(url) }, onOpenRepository = openRepo, onLogout = onLogout) }
            composable(ACCOUNT_SWITCHER_ROUTE) { AccountSwitcherScreen(mode = mode, connectedProfile = state.profile, onBack = navController::navigateUp, onOpenProfile = openAccountProfile, onReplaceConnectedAccount = onLogout) }
            composable(NATIVE_PROFILE_ROUTE, arguments = listOf(navArgument("login") { type = NavType.StringType }, navArgument("section") { type = NavType.StringType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://profile/{login}/{section}" })) {
                NativeProfileScreen(mode = mode, ownLogin = state.profile?.login, onBack = navController::navigateUp, onOpenRepository = openRepo, onOpenProfile = { login -> openNativeProfile(login, NativeProfileSection.Repositories) })
            }
            composable(SETTINGS_ROUTE) { GitHubSettingsScreen(state.profile?.login, { navController.navigate(APP_CUSTOMIZATION_ROUTE) { launchSingleTop = true } }, onOpenGitHubUrl, navController::navigateUp) }
            composable(APP_CUSTOMIZATION_ROUTE) { AppearanceScreen(navController::navigateUp) }
            composable(APP_INFORMATION_ROUTE) { AppInformationScreen(navController::navigateUp) }
            composable(FEATURES_PREVIEW_ROUTE) { FeaturePreviewScreen(state.profile?.login, onOpenGitHubUrl, navController::navigateUp) }
            composable("repo/{owner}/{repo}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://repo/{owner}/{repo}" }, navDeepLink { uriPattern = "https://github.com/{owner}/{repo}" })) { e -> RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, navController::navigateUp) }
            composable("build/{owner}/{repo}/{runId}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://build/{owner}/{repo}/{runId}" })) { e -> BuildsScreen(mode = mode, repositories = state.repositories, runs = state.workflowRuns, onSelectRepository = openRepo, initialRepository = state.repositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, initialRunId = e.arguments?.getLong("runId")) }
            composable("release/{owner}/{repo}/{tag}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("tag") { type = NavType.StringType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://release/{owner}/{repo}/{tag}" })) { e -> RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, navController::navigateUp, e.arguments?.getString("tag")) }
        }
    }
}
