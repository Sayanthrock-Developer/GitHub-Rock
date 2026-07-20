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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sayanthrock.githubrock.ui.screens.AppearanceScreen
import com.sayanthrock.githubrock.ui.screens.AppInformationScreen
import com.sayanthrock.githubrock.ui.screens.BuildsScreen
import com.sayanthrock.githubrock.ui.screens.DownloadsScreen
import com.sayanthrock.githubrock.ui.screens.FeaturePreviewScreen
import com.sayanthrock.githubrock.ui.screens.GitHubSettingsScreen
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.screens.RepositoriesScreen
import com.sayanthrock.githubrock.ui.screens.RepositoryHubScreen

sealed class TopDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : TopDestination("home", "Home", Icons.Default.Home)
    data object Repositories : TopDestination("repositories", "Repos", Icons.Default.Folder)
    data object Builds : TopDestination("builds", "Builds", Icons.Default.Build)
    data object Downloads : TopDestination("downloads", "Files", Icons.Default.Download)
    data object Profile : TopDestination("profile", "Profile", Icons.Default.AccountCircle)
}

private const val FEATURES_PREVIEW_ROUTE = "features-preview"
private const val SETTINGS_ROUTE = "settings"
private const val APP_CUSTOMIZATION_ROUTE = "app-customization"
private const val APP_INFORMATION_ROUTE = "app-information"

private val topDestinations = listOf(
    TopDestination.Home,
    TopDestination.Repositories,
    TopDestination.Builds,
    TopDestination.Downloads,
    TopDestination.Profile
)

internal enum class MainNavigationLayout { BottomBar, NavigationRail }

internal fun mainNavigationLayout(widthDp: Float): MainNavigationLayout =
    if (widthDp >= 600f) MainNavigationLayout.NavigationRail else MainNavigationLayout.BottomBar

@Composable
fun MainNavigation(
    navController: NavHostController,
    state: MainUiState,
    onSearch: (com.sayanthrock.githubrock.core.model.RepositorySearchOptions) -> Unit,
    onInspectProfile: (String) -> Unit,
    onRememberRepository: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onRefresh: () -> Unit,
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
    val navigateToTopDestination: (TopDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val navigationLayout = mainNavigationLayout(maxWidth.value)
        val useNavigationRail = navigationLayout == MainNavigationLayout.NavigationRail

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showNavigation && !useNavigationRail) {
                    AppNavigationBar(
                        selectedRoute = route,
                        onDestinationSelected = navigateToTopDestination
                    )
                }
            }
        ) { scaffoldPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (showNavigation) scaffoldPadding else PaddingValues())
            ) {
                if (showNavigation && useNavigationRail) {
                    AppNavigationRail(
                        selectedRoute = route,
                        onDestinationSelected = navigateToTopDestination
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = TopDestination.Home.route,
                        modifier = Modifier.widthIn(max = 1200.dp).fillMaxSize()
                    ) {
                        composable(TopDestination.Home.route) {
                            HomeScreen(
                                profile = state.profile,
                                rateLimit = state.rateLimit,
                                repositories = state.repositories,
                                runs = state.workflowRuns,
                                onOpenRepo = openRepo,
                                onOpenBuilds = { navController.navigate(TopDestination.Builds.route) },
                                isLoading = state.isLoading,
                                isRefreshing = state.isRefreshing,
                                onRefresh = onRefresh
                            )
                        }
                        composable(TopDestination.Repositories.route) {
                            RepositoriesScreen(
                                repositories = state.repositories,
                                loading = state.isLoading,
                                onSearch = onSearch,
                                creationEnabled = mode == AppMode.Connected,
                                onOpen = openRepo
                            )
                        }
                        composable(TopDestination.Builds.route) {
                            BuildsScreen(mode, state.repositories, state.workflowRuns, openRepo)
                        }
                        composable(TopDestination.Downloads.route) { DownloadsScreen() }
                        composable(TopDestination.Profile.route) {
                            ProfileScreen(
                                mode = mode,
                                profile = state.profile,
                                explorerState = state.profileExplorer,
                                onInspectProfile = onInspectProfile,
                                onOpenDownloads = {
                                    navController.navigate(TopDestination.Downloads.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenFeatures = { navController.navigate(FEATURES_PREVIEW_ROUTE) },
                                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                                onOpenAppInfo = { navController.navigate(APP_INFORMATION_ROUTE) },
                                onOpenGitHubUrl = onOpenGitHubUrl,
                                onLogout = onLogout
                            )
                        }
                        composable(SETTINGS_ROUTE) {
                            GitHubSettingsScreen(
                                login = state.profile?.login,
                                onOpenAppSettings = { navController.navigate(APP_CUSTOMIZATION_ROUTE) },
                                onOpenGitHubUrl = onOpenGitHubUrl,
                                onBack = navController::navigateUp
                            )
                        }
                        composable(APP_CUSTOMIZATION_ROUTE) {
                            AppearanceScreen(onBack = navController::navigateUp)
                        }
                        composable(APP_INFORMATION_ROUTE) {
                            AppInformationScreen(onBack = navController::navigateUp)
                        }
                        composable(FEATURES_PREVIEW_ROUTE) {
                            FeaturePreviewScreen(
                                login = state.profile?.login,
                                onOpenGitHubUrl = onOpenGitHubUrl,
                                onBack = navController::navigateUp
                            )
                        }
                        composable(
                            route = "repo/{owner}/{repo}?demo={demo}",
                            arguments = listOf(
                                navArgument("owner") { type = NavType.StringType },
                                navArgument("repo") { type = NavType.StringType },
                                navArgument("demo") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            ),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "githubrock://repo/{owner}/{repo}" },
                                navDeepLink { uriPattern = "https://github.com/{owner}/{repo}" }
                            )
                        ) { backStackEntry ->
                            val owner = backStackEntry.arguments?.getString("owner")
                            val repoName = backStackEntry.arguments?.getString("repo")
                            val repository = state.repositories.firstOrNull {
                                it.owner.login == owner && it.name == repoName
                            }
                            RepositoryHubScreen(
                                repository = repository,
                                onBack = navController::navigateUp
                            )
                        }
                        composable(
                            route = "build/{owner}/{repo}/{runId}",
                            arguments = listOf(
                                navArgument("owner") { type = NavType.StringType },
                                navArgument("repo") { type = NavType.StringType },
                                navArgument("runId") { type = NavType.LongType }
                            ),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "githubrock://build/{owner}/{repo}/{runId}" }
                            )
                        ) { backStackEntry ->
                            val owner = backStackEntry.arguments?.getString("owner")
                            val repoName = backStackEntry.arguments?.getString("repo")
                            val runId = backStackEntry.arguments?.getLong("runId")
                            val repository = state.repositories.firstOrNull {
                                it.owner.login == owner && it.name == repoName
                            }
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
                            route = "release/{owner}/{repo}/{tag}?demo={demo}",
                            arguments = listOf(
                                navArgument("owner") { type = NavType.StringType },
                                navArgument("repo") { type = NavType.StringType },
                                navArgument("tag") { type = NavType.StringType },
                                navArgument("demo") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            ),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "githubrock://release/{owner}/{repo}/{tag}" }
                            )
                        ) { backStackEntry ->
                            val owner = backStackEntry.arguments?.getString("owner")
                            val repoName = backStackEntry.arguments?.getString("repo")
                            val tag = backStackEntry.arguments?.getString("tag")
                            val repository = state.repositories.firstOrNull {
                                it.owner.login == owner && it.name == repoName
                            }
                            RepositoryHubScreen(
                                repository = repository,
                                onBack = navController::navigateUp,
                                initialTag = tag
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    NavigationBar(
        modifier = Modifier.drawBehind {
            drawLine(
                color = borderColor,
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        topDestinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        destination.icon,
                        destination.label,
                        modifier = Modifier.size(23.dp)
                    )
                },
                label = {
                    NavigationLabel(destination.label, selected)
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    NavigationRail(
        modifier = Modifier.fillMaxHeight().drawBehind {
            drawLine(
                color = borderColor,
                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        },
        header = {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "GR",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Spacer(Modifier.height(8.dp))
        topDestinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        destination.icon,
                        destination.label,
                        modifier = Modifier.size(23.dp)
                    )
                },
                label = { NavigationLabel(destination.label, selected) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun NavigationLabel(label: String, selected: Boolean) {
    Text(
        label,
        maxLines = 1,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    )
}
