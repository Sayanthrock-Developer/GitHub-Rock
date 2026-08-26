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
import com.sayanthrock.githubrock.ui.screens.AccountSwitcherScreen
import com.sayanthrock.githubrock.ui.screens.AppearanceScreen
import com.sayanthrock.githubrock.ui.screens.AppInformationScreen
import com.sayanthrock.githubrock.ui.screens.BuildDetailsScreen
import com.sayanthrock.githubrock.ui.screens.BuildsScreen
import com.sayanthrock.githubrock.ui.screens.DownloadsHubScreen
import com.sayanthrock.githubrock.ui.screens.FeaturePreviewScreen
import com.sayanthrock.githubrock.ui.screens.GitHubSettingsScreen
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.screens.NativeProfileScreen
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.screens.RepositoriesScreen
import com.sayanthrock.githubrock.ui.screens.RepositoryHubScreen

sealed class TopDestinationV2(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accessibilityLabel: String = label
) {
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
    navController: NavHostController,
    state: MainUiState,
    onSearch: (com.sayanthrock.githubrock.core.model.RepositorySearchOptions) -> Unit,
    onInspectProfile: (String) -> Unit,
    onRememberRepository: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val mode = requireNotNull(state.mode)
    val openRepo: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit = { repo ->
        onRememberRepository(repo)
        navController.navigate("repo/${repo.owner.login}/${repo.name}")
    }
    val openNativeProfile: (String, NativeProfileSection) -> Unit = { login, section ->
        navController.navigate(NativeProfileDestination(login, section).route) { launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TopDestinationV2.Home.route,
            modifier = Modifier.fillMaxSize().widthIn(max = 1200.dp)
        ) {
            composable(TopDestinationV2.Home.route) {
                HomeScreen(state.repositories, openRepo, state.isLoading, state.isRefreshing, onRefresh)
            }
            composable(TopDestinationV2.Repositories.route) {
                RepositoriesScreen(state.repositories, state.isLoading, onSearch, mode == AppMode.Connected, openRepo, state.profile?.login)
            }
            composable(TopDestinationV2.Builds.route) {
                BuildsScreen(
                    mode = mode,
                    repositories = state.repositories,
                    runs = state.workflowRuns,
                    onSelectRepository = openRepo,
                    onOpenRun = { repo, run ->
                        navController.navigate("build-details/${repo.owner.login}/${repo.name}/${run.id}")
                    }
                )
            }
            composable(BUILD_DETAILS_ROUTE, arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType }
            )) { entry ->
                val owner = entry.arguments?.getString("owner").orEmpty()
                val repo = entry.arguments?.getString("repo").orEmpty()
                val runId = entry.arguments?.getLong("runId") ?: 0L
                val repository = state.repositories.firstOrNull { it.owner.login.equals(owner, true) && it.name == repo }
                if (repository != null && runId > 0) {
                    BuildDetailsScreen(mode, repository, runId, navController::navigateUp)
                } else {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Build unavailable", style = MaterialTheme.typography.headlineSmall)
                        Text("The repository or workflow run is no longer available.")
                        Button(onClick = navController::navigateUp) { Text("Back to Builds") }
                    }
                }
            }
            composable(TopDestinationV2.Downloads.route) { DownloadsHubScreen() }
            composable(TopDestinationV2.Profile.route) {
                ProfileScreen(
                    mode = mode,
                    profile = state.profile,
                    explorerState = state.profileExplorer,
                    onInspectProfile = onInspectProfile,
                    onOpenDownloads = { navController.navigate(TopDestinationV2.Downloads.route) { launchSingleTop = true } },
                    onOpenFeatures = { navController.navigate(FEATURES_PREVIEW_ROUTE) },
                    onOpenAccounts = { navController.navigate(ACCOUNT_SWITCHER_ROUTE) },
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenAppInfo = { navController.navigate(APP_INFORMATION_ROUTE) },
                    onOpenGitHubUrl = { url ->
                        nativeProfileDestination(url)?.let { openNativeProfile(it.login, it.section) } ?: onOpenGitHubUrl(url)
                    },
                    onOpenRepository = openRepo,
                    onLogout = onLogout
                )
            }
            composable(ACCOUNT_SWITCHER_ROUTE) {
                AccountSwitcherScreen(mode, state.profile, navController::navigateUp, { login -> openNativeProfile(login, NativeProfileSection.Repositories) }, onLogout)
            }
            composable(NATIVE_PROFILE_ROUTE, arguments = listOf(
                navArgument("login") { type = NavType.StringType },
                navArgument("section") { type = NavType.StringType }
            ), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://profile/{login}/{section}" })) {
                NativeProfileScreen(mode, state.profile?.login, navController::navigateUp, openRepo) { login ->
                    openNativeProfile(login, NativeProfileSection.Repositories)
                }
            }
            composable(SETTINGS_ROUTE) {
                GitHubSettingsScreen(state.profile?.login, {
                    navController.navigate(APP_CUSTOMIZATION_ROUTE) { launchSingleTop = true }
                }, onOpenGitHubUrl, navController::navigateUp)
            }
            composable(APP_CUSTOMIZATION_ROUTE) { AppearanceScreen(navController::navigateUp) }
            composable(APP_INFORMATION_ROUTE) { AppInformationScreen(navController::navigateUp) }
            composable(FEATURES_PREVIEW_ROUTE) {
                FeaturePreviewScreen(state.profile?.login, onOpenGitHubUrl, navController::navigateUp)
            }
            composable("repo/{owner}/{repo}", arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            ), deepLinks = listOf(
                navDeepLink { uriPattern = "githubrock://repo/{owner}/{repo}" },
                navDeepLink { uriPattern = "https://github.com/{owner}/{repo}" }
            )) { entry ->
                val owner = entry.arguments?.getString("owner")
                val repo = entry.arguments?.getString("repo")
                RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == owner && it.name == repo }, navController::navigateUp)
            }
            composable("build/{owner}/{repo}/{runId}", arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("runId") { type = NavType.LongType }
            ), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://build/{owner}/{repo}/{runId}" })) { entry ->
                val owner = entry.arguments?.getString("owner")
                val repo = entry.arguments?.getString("repo")
                val runId = entry.arguments?.getLong("runId")
                BuildsScreen(
                    mode = mode,
                    repositories = state.repositories,
                    runs = state.workflowRuns,
                    onSelectRepository = openRepo,
                    initialRepository = state.repositories.firstOrNull { it.owner.login == owner && it.name == repo },
                    initialRunId = runId
                )
            }
            composable("release/{owner}/{repo}/{tag}", arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("tag") { type = NavType.StringType }
            ), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://release/{owner}/{repo}/{tag}" })) { entry ->
                val owner = entry.arguments?.getString("owner")
                val repo = entry.arguments?.getString("repo")
                val tag = entry.arguments?.getString("tag")
                RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == owner && it.name == repo }, navController::navigateUp, tag)
            }
        }
    }
}
