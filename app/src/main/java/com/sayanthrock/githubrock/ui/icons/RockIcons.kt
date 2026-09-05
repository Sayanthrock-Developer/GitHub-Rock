package com.sayanthrock.githubrock.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Account_circle as AccountCircleRegular
import com.composables.icons.materialsymbols.rounded.Archive as ArchiveRegular
import com.composables.icons.materialsymbols.rounded.Arrow_back as ArrowBackRegular
import com.composables.icons.materialsymbols.rounded.Chat_bubble_outline as ChatBubbleOutlineRegular
import com.composables.icons.materialsymbols.rounded.Check_circle as CheckCircleRegular
import com.composables.icons.materialsymbols.rounded.Chevron_right as ChevronRightRegular
import com.composables.icons.materialsymbols.rounded.Code as CodeRegular
import com.composables.icons.materialsymbols.rounded.Error_outline as ErrorOutlineRegular
import com.composables.icons.materialsymbols.rounded.Favorite as FavoriteRegular
import com.composables.icons.materialsymbols.rounded.Search as SearchRegular
import com.composables.icons.materialsymbols.rounded.Star as StarRegular
import com.composables.icons.materialsymbols.rounded.Announcement as AnnouncementRegular
import com.composables.icons.materialsymbols.rounded.Auto_awesome as AutoAwesomeRegular
import com.composables.icons.materialsymbols.rounded.Build as BuildRegular
import com.composables.icons.materialsymbols.rounded.Download as DownloadRegular
import com.composables.icons.materialsymbols.rounded.Explore as ExploreRegular
import com.composables.icons.materialsymbols.rounded.Folder as FolderRegular
import com.composables.icons.materialsymbols.rounded.Home as HomeRegular
import com.composables.icons.materialsymbols.rounded.Settings as SettingsRegular
import com.composables.icons.materialsymbols.roundedfilled.Account_circle
import com.composables.icons.materialsymbols.roundedfilled.Archive
import com.composables.icons.materialsymbols.roundedfilled.Build
import com.composables.icons.materialsymbols.roundedfilled.Download
import com.composables.icons.materialsymbols.roundedfilled.Explore
import com.composables.icons.materialsymbols.roundedfilled.Folder
import com.composables.icons.materialsymbols.roundedfilled.Home
import com.composables.icons.materialsymbols.roundedfilled.Settings

/** Single source of truth for GitHub Rock's general-purpose Material Symbols Rounded icons. */
enum class RockIcon {
    Home, Explore, Repositories, Builds, Downloads, Profile, Settings,
    Back, Search, ChevronRight, Error, CheckCircle, ChatBubbleOutline,
    Archive, Code, Star, Favorite, Announcement, AutoAwesome
}

fun RockIcon.vector(selected: Boolean = false): ImageVector = when (this) {
    RockIcon.Home -> if (selected) MaterialSymbols.RoundedFilled.Home else MaterialSymbols.Rounded.HomeRegular
    RockIcon.Explore -> if (selected) MaterialSymbols.RoundedFilled.Explore else MaterialSymbols.Rounded.ExploreRegular
    RockIcon.Repositories -> if (selected) MaterialSymbols.RoundedFilled.Folder else MaterialSymbols.Rounded.FolderRegular
    RockIcon.Builds -> if (selected) MaterialSymbols.RoundedFilled.Build else MaterialSymbols.Rounded.BuildRegular
    RockIcon.Downloads -> if (selected) MaterialSymbols.RoundedFilled.Download else MaterialSymbols.Rounded.DownloadRegular
    RockIcon.Profile -> if (selected) MaterialSymbols.RoundedFilled.Account_circle else MaterialSymbols.Rounded.AccountCircleRegular
    RockIcon.Settings -> if (selected) MaterialSymbols.RoundedFilled.Settings else MaterialSymbols.Rounded.SettingsRegular
    RockIcon.Back -> MaterialSymbols.Rounded.ArrowBackRegular
    RockIcon.Search -> MaterialSymbols.Rounded.SearchRegular
    RockIcon.ChevronRight -> MaterialSymbols.Rounded.ChevronRightRegular
    RockIcon.Error -> MaterialSymbols.Rounded.ErrorOutlineRegular
    RockIcon.CheckCircle -> MaterialSymbols.Rounded.CheckCircleRegular
    RockIcon.ChatBubbleOutline -> MaterialSymbols.Rounded.ChatBubbleOutlineRegular
    RockIcon.Archive -> if (selected) MaterialSymbols.RoundedFilled.Archive else MaterialSymbols.Rounded.ArchiveRegular
    RockIcon.Code -> MaterialSymbols.Rounded.CodeRegular
    RockIcon.Star -> MaterialSymbols.Rounded.StarRegular
    RockIcon.Favorite -> MaterialSymbols.Rounded.FavoriteRegular
    RockIcon.Announcement -> MaterialSymbols.Rounded.AnnouncementRegular
    RockIcon.AutoAwesome -> MaterialSymbols.Rounded.AutoAwesomeRegular
}
