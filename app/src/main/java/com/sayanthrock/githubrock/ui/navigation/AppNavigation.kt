package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.sayanthrock.githubrock.core.navigation.NativeProfileDestination
import com.sayanthrock.githubrock.core.navigation.NativeProfileSection
import com.sayanthrock.githubrock.core.navigation.nativeProfileDestination
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.MainUiState
import com.sayanthrock.githubrock.ui.screens.AccountSwitcherScreen
import com.sayanthrock.githubrock.ui.screens.AppearanceScreen
import com.sayanthrock.githubrock.ui.screens.AppInformationScreen
import com.sayanthrock.githubrock.ui.screens.BuildsScreen
import com.sayanthrock.githubrock.ui.screens.DownloadsHubScreen
import com.sayanthrock.githubrock.ui.screens.FeaturePreviewScreen
import com.sayanthrock.githubrock.ui.screens.GitHubSettingsScreen
import com.sayanthrock.githubrock.ui.screens.HomeScreen
import com.sayanthrock.githubrock.ui.screens.NativeProfileScreen
import com.sayanthrock.githubrock.ui.screens.ProfileScreen
import com.sayanthrock.githubrock.ui.screens.RepositoriesScreen
import com.sayanthrock.githubrock.ui.screens.RepositoryHubScreen

sealed class TopDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accessibilityLabel: String = label
) {
    data object Home : TopDestination("home", "Home", Icons.Default.Home)
    data object Repositories : TopDestination("repositories", "Repos", Icons.Default.Folder, "Repositories")
    data object Builds : TopDestination("builds", "Builds", Icons.Default.Build)
    data object Downloads : TopDestination("downloads", "Downloads", Icons.Default.Download)
    data object Profile : TopDestination("profile", "Profile", Icons.Default.AccountCircle)
}

private const val FEATURES_PREVIEW_ROUTE = "features-preview"
private const val SETTINGS_ROUTE = "settings"
private const val APP_CUSTOMIZATION_ROUTE = "app-customization"
private const val APP_INFORMATION_ROUTE = "app-information"
private const val ACCOUNT_SWITCHER_ROUTE = "accounts-organizations"
private const val NATIVE_PROFILE_ROUTE = "native-profile/{login}/{section}"
private val MobileDockHeight = 78.dp
private val MobileDockContentClearance = 94.dp

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
    val openNativeProfile: (String, NativeProfileSection) -> Unit = { login, section ->
        navController.navigate(NativeProfileDestination(login, section).route) {
            launchSingleTop = true
        }
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
        val scrollCaptureInProgress = LocalScrollCaptureInProgress.current
        val showMobileDock = showNavigation && !useNavigationRail && !scrollCaptureInProgress

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { scaffoldPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(bottom = if (showMobileDock) MobileDockContentClearance else 0.dp)
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
                                onOpen = openRepo,
                                connectedLogin = state.profile?.login
                            )
                        }
                        composable(TopDestination.Builds.route) {
                            BuildsScreen(mode, state.repositories, state.workflowRuns, openRepo)
                        }
                        composable(TopDestination.Downloads.route) { DownloadsHubScreen() }
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
                                onOpenFeatures = { navController.navigate(ACCOUNT_SWITCHER_ROUTE) },
                                onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                                onOpenAppInfo = { navController.navigate(APP_INFORMATION_ROUTE) },
                                onOpenGitHubUrl = { url ->
                                    val destination = nativeProfileDestination(url)
                                    if (destination != null) {
                                        openNativeProfile(destination.login, destination.section)
                                    } else {
                                        onOpenGitHubUrl(url)
                                    }
                                },
                                onLogout = onLogout
                            )
                        }
                        composable(ACCOUNT_SWITCHER_ROUTE) {
                            AccountSwitcherScreen(
                                mode = mode,
                                connectedProfile = state.profile,
                                onBack = navController::navigateUp,
                                onOpenProfile = { login ->
                                    openNativeProfile(login, NativeProfileSection.Repositories)
                                },
                                onReplaceConnectedAccount = onLogout
                            )
                        }
                        composable(
                            route = NATIVE_PROFILE_ROUTE,
                            arguments = listOf(
                                navArgument("login") { type = NavType.StringType },
                                navArgument("section") { type = NavType.StringType }
                            ),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "githubrock://profile/{login}/{section}" }
                            )
                        ) {
                            NativeProfileScreen(
                                mode = mode,
                                ownLogin = state.profile?.login,
                                onBack = navController::navigateUp,
                                onOpenRepository = openRepo,
                                onOpenProfile = { login ->
                                    openNativeProfile(login, NativeProfileSection.Repositories)
                                }
                            )
                        }
                        composable(SETTINGS_ROUTE) {
                            GitHubSettingsScreen(
                                login = state.profile?.login,
                                onOpenAppSettings = {
                                    navController.navigate(APP_CUSTOMIZATION_ROUTE) {
                                        launchSingleTop = true
                                    }
                                },
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

        if (showMobileDock) {
            AppNavigationBar(
                selectedRoute = route,
                onDestinationSelected = navigateToTopDestination,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
internal fun AppNavigationBar(
    selectedRoute: String?,
    onDestinationSelected: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(MobileDockHeight),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .66f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(6.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                topDestinations.forEach { destination ->
                    NavigationDockItem(
                        destination = destination,
                        selected = selectedRoute == destination.route,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationDockItem(
    destination: TopDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = destination.accessibilityLabel
            },
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .98f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .26f)) else null,
        shadowElevation = if (selected) 8.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier.size(if (selected) 26.dp else 23.dp),
                tint = iconColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = destination.label,
                color = labelColor,
                maxLines = 1,
                fontSize = if (selected) 11.sp else 10.5.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
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
                        destination.accessibilityLabel,
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
