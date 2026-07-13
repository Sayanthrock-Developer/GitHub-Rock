package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.MainUiState
import com.sayanthrock.githubrock.ui.screens.*

sealed class TopDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : TopDestination("home", "Home", Icons.Default.Home)
    data object Repositories : TopDestination("repositories", "Repositories", Icons.Default.Folder)
    data object Builds : TopDestination("builds", "Builds", Icons.Default.Build)
    data object Downloads : TopDestination("downloads", "Downloads", Icons.Default.Download)
    data object Profile : TopDestination("profile", "Profile", Icons.Default.AccountCircle)
}

private val topDestinations = listOf(TopDestination.Home, TopDestination.Repositories, TopDestination.Builds, TopDestination.Downloads, TopDestination.Profile)

@Composable
fun MainNavigation(
    navController: NavHostController,
    state: MainUiState,
    onSearch: (String) -> Unit,
    onRememberRepository: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit,
    onRefreshSocial: () -> Unit,
    onLogout: () -> Unit
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showNavigation = topDestinations.any { it.route == route }
    val mode = requireNotNull(state.mode)
    val openRepo: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit = { repo ->
        onRememberRepository(repo)
        navController.navigate("repo/${repo.owner.login}/${repo.name}?demo=${mode == AppMode.Demo}")
    }

    Scaffold(
        bottomBar = {
            if (showNavigation) NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
                topDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = TopDestination.Home.route, modifier = Modifier.padding(if (showNavigation) padding else androidx.compose.foundation.layout.PaddingValues())) {
            composable(TopDestination.Home.route) {
                HomeScreen(mode, state.profile, state.rateLimit, state.repositories, state.workflowRuns, openRepo) {
                    navController.navigate(TopDestination.Builds.route)
                }
            }
            composable(TopDestination.Repositories.route) {
                RepositoriesScreen(state.repositories, state.isLoading, onSearch, openRepo)
            }
            composable(TopDestination.Builds.route) {
                BuildsScreen(mode, state.repositories, state.workflowRuns, openRepo)
            }
            composable(TopDestination.Downloads.route) { DownloadsScreen() }
            composable(TopDestination.Profile.route) {
                ProfileScreen(
                    mode = mode,
                    profile = state.profile,
                    followers = state.followers,
                    following = state.following,
                    socialLoading = state.socialLoading,
                    socialError = state.socialError,
                    onRetrySocial = onRefreshSocial,
                    onLogout = onLogout
                )
            }
            composable(
                route = "repo/{owner}/{repo}?demo={demo}",
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("repo") { type = NavType.StringType },
                    navArgument("demo") { type = NavType.BoolType; defaultValue = false }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "githubrock://repo/{owner}/{repo}" },
                    navDeepLink { uriPattern = "https://github.com/{owner}/{repo}" }
                )
            ) { backStackEntry ->
                val owner = backStackEntry.arguments?.getString("owner")
                val repoName = backStackEntry.arguments?.getString("repo")
                val repository = state.repositories.firstOrNull { it.owner.login == owner && it.name == repoName }
                RepositoryDetailScreen(repository = repository, onBack = navController::navigateUp)
            }
            composable(
                route = "build/{owner}/{repo}/{runId}",
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("repo") { type = NavType.StringType },
                    navArgument("runId") { type = NavType.LongType }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "githubrock://build/{owner}/{repo}/{runId}" })
            ) { backStackEntry ->
                val owner = backStackEntry.arguments?.getString("owner")
                val repoName = backStackEntry.arguments?.getString("repo")
                val runId = backStackEntry.arguments?.getLong("runId")
                val repository = state.repositories.firstOrNull { it.owner.login == owner && it.name == repoName }
                BuildsScreen(
                    mode = mode,
                    repositories = state.repositories,
                    runs = state.workflowRuns,
                    onSelectRepository = openRepo,
                    initialRepository = repository,
                    initialRunId = runId
                )
            }
            composable(
                route = "release/{owner}/{repo}/{tag}",
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("repo") { type = NavType.StringType },
                    navArgument("tag") { type = NavType.StringType }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "githubrock://release/{owner}/{repo}/{tag}" })
            ) { backStackEntry ->
                val owner = backStackEntry.arguments?.getString("owner")
                val repoName = backStackEntry.arguments?.getString("repo")
                val repository = state.repositories.firstOrNull { it.owner.login == owner && it.name == repoName }
                RepositoryDetailScreen(repository = repository, onBack = navController::navigateUp)
            }
        }
    }
}
