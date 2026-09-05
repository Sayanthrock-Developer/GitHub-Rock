package com.sayanthrock.githubrock.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.screens.*

sealed class TopDestinationV2(
    val route: String,
    val label: String,
    val rockIcon: RockIcon,
    val accessibilityLabel: String = label
) {
    val icon: ImageVector get() = rockIcon.vector()
    val selectedIcon: ImageVector get() = rockIcon.vector(selected = true)

    data object Home : TopDestinationV2("home", "Home", RockIcon.Home)
    data object Explore : TopDestinationV2("explore", "Explore", RockIcon.Explore)
    data object Repositories : TopDestinationV2("repositories", "Repos", RockIcon.Repositories, "Repositories")
    data object Builds : TopDestinationV2("builds", "Builds", RockIcon.Builds)
    data object Downloads : TopDestinationV2("downloads", "Downloads", RockIcon.Downloads)
    data object Profile : TopDestinationV2("profile", "Profile", RockIcon.Profile)
    data object Options : TopDestinationV2("settings", "Options", RockIcon.Settings)
}

private const val FEATURES_PREVIEW_ROUTE = "features-preview"
private const val SETTINGS_ROUTE = "settings"
private const val APP_CUSTOMIZATION_ROUTE = "app-customization"
private const val APP_INFORMATION_ROUTE = "app-information"
private const val ACCOUNT_SWITCHER_ROUTE = "accounts-organizations"
private const val BUILD_DETAILS_ROUTE = "build-details/{owner}/{repo}/{runId}"
private const val BUILD_JOB_DETAILS_ROUTE = "build-details/{owner}/{repo}/{runId}/job/{jobId}"
private const val BUILD_ARTIFACT_DETAILS_ROUTE = "build-details/{owner}/{repo}/{runId}/artifact/{artifactId}"
private const val BUILD_STATUS_ROUTE = "builds/status/{filter}"
private const val NATIVE_PROFILE_ROUTE = "native-profile/{login}/{section}"

@Composable
fun MainNavigationV2(navController: NavHostController, state: MainUiState, onSearch: (com.sayanthrock.githubrock.core.model.RepositorySearchOptions) -> Unit, onInspectProfile: (String) -> Unit, onRememberRepository: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit, onOpenGitHubUrl: (String) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit) {
    val mode = requireNotNull(state.mode)
    val buildRepositories = state.profile?.login?.let { login -> state.repositories.filter { it.owner.login.equals(login, ignoreCase = true) } }.orEmpty()
    val openRepo: (com.sayanthrock.githubrock.core.model.GitHubRepositoryModel) -> Unit = { repo -> onRememberRepository(repo); navController.navigate("repo/${repo.owner.login}/${repo.name}") }
    val openNativeProfile: (String, NativeProfileSection) -> Unit = { login, section -> navController.navigate(NativeProfileDestination(login, section).route) { launchSingleTop = true } }
    val openAccountProfile: (String) -> Unit = { login -> openNativeProfile(login, NativeProfileSection.Repositories) }
    val openSettings: () -> Unit = { navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true } }
    Box(Modifier.fillMaxSize()) {
        NavHost(navController, TopDestinationV2.Home.route, Modifier.fillMaxSize().widthIn(max = 1200.dp)) {
            composable(TopDestinationV2.Home.route) { HomeScreen(state.repositories, openRepo, state.isLoading, state.isRefreshing, onRefresh) }
            composable(TopDestinationV2.Explore.route) { ExploreScreen(onOpenRepo = openRepo, onOpenProfile = { login -> openNativeProfile(login, NativeProfileSection.Repositories) }) }
            composable(TopDestinationV2.Repositories.route) { RepositoriesScreen(state.repositories, state.isLoading, onSearch, mode == AppMode.Connected, openRepo, state.profile?.login) }
            composable(TopDestinationV2.Builds.route) { BuildsScreen(mode, buildRepositories, state.workflowRuns, openRepo, { repo, run -> navController.navigate("build-details/${repo.owner.login}/${repo.name}/${run.id}") }) }
            composable(BUILD_STATUS_ROUTE, arguments = listOf(navArgument("filter") { type = NavType.StringType })) { e -> BuildStatusPage(mode, state.repositories, state.workflowRuns, e.arguments?.getString("filter").orEmpty(), openRepo, { repo, run -> navController.navigate("build-details/${repo.owner.login}/${repo.name}/${run.id}") }, state.profile?.login) }
            composable(BUILD_DETAILS_ROUTE, arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType })) { e ->
                val owner = e.arguments?.getString("owner").orEmpty(); val repo = e.arguments?.getString("repo").orEmpty(); val runId = e.arguments?.getLong("runId") ?: 0L
                val repository = buildRepositories.firstOrNull { it.owner.login.equals(owner, true) && it.name == repo }
                if (repository != null && runId > 0) BuildDetailsScreen(mode, repository, runId, navController::navigateUp, { id, jobId -> navController.navigate("build-details/$owner/$repo/$id/job/$jobId") }, { id, artifactId -> navController.navigate("build-details/$owner/$repo/$id/artifact/$artifactId") }) else Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Build unavailable"); Button(onClick = navController::navigateUp) { Text("Back to Builds") } }
            }
            composable(BUILD_JOB_DETAILS_ROUTE, arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType }, navArgument("jobId") { type = NavType.LongType })) { e ->
                val owner = e.arguments?.getString("owner").orEmpty(); val repo = e.arguments?.getString("repo").orEmpty(); val runId = e.arguments?.getLong("runId") ?: 0L; val jobId = e.arguments?.getLong("jobId") ?: 0L
                buildRepositories.firstOrNull { it.owner.login.equals(owner, true) && it.name == repo }?.let { BuildJobDetailsScreen(mode, it, runId, jobId, navController::navigateUp) } ?: Text("Repository unavailable")
            }
            composable(BUILD_ARTIFACT_DETAILS_ROUTE, arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType }, navArgument("artifactId") { type = NavType.LongType })) { e ->
                val owner = e.arguments?.getString("owner").orEmpty(); val repo = e.arguments?.getString("repo").orEmpty(); val runId = e.arguments?.getLong("runId") ?: 0L; val artifactId = e.arguments?.getLong("artifactId") ?: 0L
                buildRepositories.firstOrNull { it.owner.login.equals(owner, true) && it.name == repo }?.let { BuildArtifactDetailsScreen(it, runId, artifactId, navController::navigateUp) } ?: Text("Repository unavailable")
            }
            composable(TopDestinationV2.Downloads.route) { DownloadsHubScreen() }
            composable(TopDestinationV2.Profile.route) { ProfileScreenWithMetricNavigation(mode, state.profile, state.profileExplorer, onInspectProfile, { navController.navigate(TopDestinationV2.Downloads.route) { launchSingleTop = true } }, { navController.navigate(FEATURES_PREVIEW_ROUTE) }, { navController.navigate(ACCOUNT_SWITCHER_ROUTE) }, openSettings, { navController.navigate(APP_INFORMATION_ROUTE) }, { url -> nativeProfileDestination(url)?.let { openNativeProfile(it.login, it.section) } ?: onOpenGitHubUrl(url) }, openRepo, onLogout, { state.profile?.login?.let { openNativeProfile(it, NativeProfileSection.Repositories) } }, { state.profile?.login?.let { openNativeProfile(it, NativeProfileSection.Followers) } }, { state.profile?.login?.let { openNativeProfile(it, NativeProfileSection.Following) } }) }
            composable(TopDestinationV2.Options.route) { GitHubSettingsScreen(state.profile, { login -> openNativeProfile(login, NativeProfileSection.Repositories) }, { navController.navigate(ACCOUNT_SWITCHER_ROUTE) { launchSingleTop = true } }, { navController.navigate(APP_CUSTOMIZATION_ROUTE) { launchSingleTop = true } }, { navController.navigate(TopDestinationV2.Downloads.route) { launchSingleTop = true } }, { navController.navigate(APP_INFORMATION_ROUTE) { launchSingleTop = true } }, onOpenGitHubUrl, navController::navigateUp) }
            composable(ACCOUNT_SWITCHER_ROUTE) { AccountSwitcherScreen(mode, state.profile, navController::navigateUp, openAccountProfile, onLogout, onLogout, onOpenGitHubUrl) }
            composable(NATIVE_PROFILE_ROUTE, arguments = listOf(navArgument("login") { type = NavType.StringType }, navArgument("section") { type = NavType.StringType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://profile/{login}/{section}" })) { NativeProfileScreen(mode, state.profile?.login, navController::navigateUp, openRepo, { login -> openNativeProfile(login, NativeProfileSection.Repositories) }) }
            composable(SETTINGS_ROUTE) { GitHubSettingsScreen(state.profile, { login -> openNativeProfile(login, NativeProfileSection.Repositories) }, { navController.navigate(ACCOUNT_SWITCHER_ROUTE) { launchSingleTop = true } }, { navController.navigate(APP_CUSTOMIZATION_ROUTE) { launchSingleTop = true } }, { navController.navigate(TopDestinationV2.Downloads.route) { launchSingleTop = true } }, { navController.navigate(APP_INFORMATION_ROUTE) { launchSingleTop = true } }, onOpenGitHubUrl, navController::navigateUp) }
            composable(APP_CUSTOMIZATION_ROUTE) { AppearanceScreen(navController::navigateUp) }
            composable(APP_INFORMATION_ROUTE) { AppInformationScreen(navController::navigateUp) }
            composable(FEATURES_PREVIEW_ROUTE) { FeaturePreviewScreen(state.profile?.login, onOpenGitHubUrl, navController::navigateUp) }
            composable("repo/{owner}/{repo}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://repo/{owner}/{repo}" }, navDeepLink { uriPattern = "https://github.com/{owner}/{repo}" })) { e -> RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, navController::navigateUp) }
            composable("build/{owner}/{repo}/{runId}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("runId") { type = NavType.LongType }), deepLinks = listOf(navDeepLink { uriPattern = "githubrock://build/{owner}/{repo}/{runId}" })) { e -> BuildsScreen(mode, buildRepositories, state.workflowRuns, openRepo, initialRepository = buildRepositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, initialRunId = e.arguments?.getLong("runId")) }
            composable("release/{owner}/{repo}/{tag}", arguments = listOf(navArgument("owner") { type = NavType.StringType }, navArgument("repo") { type = NavType.StringType }, navArgument("tag") { type = NavType.StringType })) { e -> RepositoryHubScreen(state.repositories.firstOrNull { it.owner.login == e.arguments?.getString("owner") && it.name == e.arguments?.getString("repo") }, navController::navigateUp, e.arguments?.getString("tag")) }
        }
    }
}
