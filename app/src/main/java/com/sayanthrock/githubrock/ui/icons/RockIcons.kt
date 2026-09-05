package com.sayanthrock.githubrock.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Account_circle
import com.composables.icons.materialsymbols.rounded.Build
import com.composables.icons.materialsymbols.rounded.Download
import com.composables.icons.materialsymbols.rounded.Explore
import com.composables.icons.materialsymbols.rounded.Folder
import com.composables.icons.materialsymbols.rounded.Home
import com.composables.icons.materialsymbols.rounded.Settings
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
 * The visual system is deliberately isolated from individual screens so future
 * icon updates do not require changing every call site. GitHub-specific brand
 * marks remain outside this layer.
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
    RockIcon.Home -> if (selected) MaterialSymbols.RoundedFilled.Home else MaterialSymbols.Rounded.Home
    RockIcon.Explore -> if (selected) MaterialSymbols.RoundedFilled.Explore else MaterialSymbols.Rounded.Explore
    RockIcon.Repositories -> if (selected) MaterialSymbols.RoundedFilled.Folder else MaterialSymbols.Rounded.Folder
    RockIcon.Builds -> if (selected) MaterialSymbols.RoundedFilled.Build else MaterialSymbols.Rounded.Build
    RockIcon.Downloads -> if (selected) MaterialSymbols.RoundedFilled.Download else MaterialSymbols.Rounded.Download
    RockIcon.Profile -> if (selected) MaterialSymbols.RoundedFilled.Account_circle else MaterialSymbols.Rounded.Account_circle
    RockIcon.Settings -> if (selected) MaterialSymbols.RoundedFilled.Settings else MaterialSymbols.Rounded.Settings
}
