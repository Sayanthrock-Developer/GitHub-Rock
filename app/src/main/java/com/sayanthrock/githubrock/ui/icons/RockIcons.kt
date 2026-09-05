package com.sayanthrock.githubrock.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Account_circle as AccountCircleRegular
import com.composables.icons.materialsymbols.rounded.Build as BuildRegular
import com.composables.icons.materialsymbols.rounded.Download as DownloadRegular
import com.composables.icons.materialsymbols.rounded.Explore as ExploreRegular
import com.composables.icons.materialsymbols.rounded.Folder as FolderRegular
import com.composables.icons.materialsymbols.rounded.Home as HomeRegular
import com.composables.icons.materialsymbols.rounded.Settings as SettingsRegular
import com.composables.icons.materialsymbols.roundedfilled.Account_circle
import com.composables.icons.materialsymbols.roundedfilled.Build
import com.composables.icons.materialsymbols.roundedfilled.Download
import com.composables.icons.materialsymbols.roundedfilled.Explore
import com.composables.icons.materialsymbols.roundedfilled.Folder
import com.composables.icons.materialsymbols.roundedfilled.Home
import com.composables.icons.materialsymbols.roundedfilled.Settings

/**
 * GitHub Rock's single source of truth for general-purpose application icons.
 *
 * GitHub-specific brand marks remain outside this layer. Screens should use
 * these semantic names instead of importing icon-library symbols directly.
 */
enum class RockIcon {
    Home,
    Explore,
    Repositories,
    Builds,
    Downloads,
    Profile,
    Settings,
}

fun RockIcon.vector(selected: Boolean = false): ImageVector = when (this) {
    RockIcon.Home -> if (selected) MaterialSymbols.RoundedFilled.Home else MaterialSymbols.Rounded.HomeRegular
    RockIcon.Explore -> if (selected) MaterialSymbols.RoundedFilled.Explore else MaterialSymbols.Rounded.ExploreRegular
    RockIcon.Repositories -> if (selected) MaterialSymbols.RoundedFilled.Folder else MaterialSymbols.Rounded.FolderRegular
    RockIcon.Builds -> if (selected) MaterialSymbols.RoundedFilled.Build else MaterialSymbols.Rounded.BuildRegular
    RockIcon.Downloads -> if (selected) MaterialSymbols.RoundedFilled.Download else MaterialSymbols.Rounded.DownloadRegular
    RockIcon.Profile -> if (selected) MaterialSymbols.RoundedFilled.Account_circle else MaterialSymbols.Rounded.AccountCircleRegular
    RockIcon.Settings -> if (selected) MaterialSymbols.RoundedFilled.Settings else MaterialSymbols.Rounded.SettingsRegular
}
