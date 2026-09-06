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
import com.sayanthrock.githubrock.ui.icons.vector
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