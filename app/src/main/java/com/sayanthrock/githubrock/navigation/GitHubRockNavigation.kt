package com.sayanthrock.githubrock.navigation

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sayanthrock.githubrock.feature.auth.AuthViewModel
import com.sayanthrock.githubrock.feature.auth.LoginScreen
import com.sayanthrock.githubrock.feature.builds.BuildsScreen
import com.sayanthrock.githubrock.feature.downloads.DownloadsScreen
import com.sayanthrock.githubrock.feature.home.HomeScreen
import com.sayanthrock.githubrock.feature.profile.ProfileScreen
import com.sayanthrock.githubrock.feature.repositories.RepositoriesScreen
import com.sayanthrock.githubrock.feature.repositories.RepositoryDetailsScreen
import com.sayanthrock.githubrock.ui.theme.LiquidBackground

private enum class SessionMode { LOGIN, CONNECTED, GUEST, DEMO }

data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDestination("home", "Home", Icons.Default.Home),
    BottomDestination("repositories", "Repositories", Icons.Default.Folder),
    BottomDestination("builds", "Builds", Icons.Default.Build),
    BottomDestination("downloads", "Downloads", Icons.Default.Download),
    BottomDestination("profile", "Profile", Icons.Default.AccountCircle)
)

@Composable
fun GitHubRockRoot(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    var modeName by rememberSaveable { mutableStateOf(SessionMode.LOGIN.name) }
    val mode = SessionMode.valueOf(modeName)

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) modeName = SessionMode.CONNECTED.name
        else if (mode == SessionMode.CONNECTED) modeName = SessionMode.LOGIN.name
    }

    if (authState.initializing) {
        LiquidBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    when (SessionMode.valueOf(modeName)) {
        SessionMode.LOGIN -> LoginScreen(
            state = authState,
            onLogin = authViewModel::startDeviceFlow,
            onGuest = { modeName = SessionMode.GUEST.name },
            onDemo = { modeName = SessionMode.DEMO.name }
        )

        else -> GitHubRockNavigation(
            mode = SessionMode.valueOf(modeName),
            onLogout = {
                authViewModel.logout()
                modeName = SessionMode.LOGIN.name
            }
        )
    }
}

@Composable
private fun GitHubRockNavigation(mode: SessionMode, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val demoMode = mode == SessionMode.DEMO
    val guestMode = mode == SessionMode.GUEST

    LiquidBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = { BottomNavigationBar(navController) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        demoMode = demoMode,
                        guestMode = guestMode,
                        onRepositories = { navController.navigate("repositories") },
                        onBuilds = { navController.navigate("builds") }
                    )
                }
                composable("repositories") {
                    RepositoriesScreen(
                        demoMode = demoMode,
                        onRepository = { repository ->
                            navController.navigate("repo/${repository.owner}/${repository.name}")
                        }
                    )
                }
                composable("builds") { BuildsScreen(demoMode) }
                composable("downloads") { DownloadsScreen() }
                composable("profile") {
                    ProfileScreen(demoMode, guestMode, onLogout)
                }
                composable(
                    route = "repo/{owner}/{repository}",
                    arguments = listOf(
                        navArgument("owner") { type = NavType.StringType },
                        navArgument("repository") { type = NavType.StringType }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "githubrock://repo/{owner}/{repository}" },
                        navDeepLink { uriPattern = "https://github.com/{owner}/{repository}" }
                    )
                ) { entry ->
                    RepositoryDetailsScreen(
                        owner = entry.arguments?.getString("owner").orEmpty(),
                        repository = entry.arguments?.getString("repository").orEmpty()
                    )
                }
                composable(
                    route = "build/{owner}/{repository}/{runId}",
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "githubrock://build/{owner}/{repository}/{runId}"
                        }
                    )
                ) { entry ->
                    DeepLinkScreen(
                        title = "Workflow run ${entry.arguments?.getString("runId").orEmpty()}",
                        url = "https://github.com/${entry.arguments?.getString("owner")}/${entry.arguments?.getString("repository")}/actions/runs/${entry.arguments?.getString("runId")}"
                    )
                }
                composable(
                    route = "release/{owner}/{repository}/{tag}",
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "githubrock://release/{owner}/{repository}/{tag}"
                        }
                    )
                ) { entry ->
                    DeepLinkScreen(
                        title = "Release ${entry.arguments?.getString("tag").orEmpty()}",
                        url = "https://github.com/${entry.arguments?.getString("owner")}/${entry.arguments?.getString("repository")}/releases/tag/${entry.arguments?.getString("tag")}"
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    NavigationBar(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        bottomDestinations.forEach { item ->
            NavigationBarItem(
                selected = destination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun DeepLinkScreen(title: String, url: String) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Button(onClick = { openUrl(context, url) }) {
            Text("Open $title on GitHub")
        }
    }
}

private fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
}
