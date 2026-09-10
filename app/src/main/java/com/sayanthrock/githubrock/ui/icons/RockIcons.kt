package com.sayanthrock.githubrock.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Account_circle as AccountCircleRegular
import com.composables.icons.materialsymbols.rounded.Announcement as AnnouncementRegular
import com.composables.icons.materialsymbols.rounded.Auto_awesome as AutoAwesomeRegular
import com.composables.icons.materialsymbols.rounded.Archive as ArchiveRegular
import com.composables.icons.materialsymbols.rounded.Arrow_back as ArrowBackRegular
import com.composables.icons.materialsymbols.rounded.Build as BuildRegular
import com.composables.icons.materialsymbols.rounded.Chat_bubble as ChatBubbleRegular
import com.composables.icons.materialsymbols.rounded.Check_circle as CheckCircleRegular
import com.composables.icons.materialsymbols.rounded.Chevron_right as ChevronRightRegular
import com.composables.icons.materialsymbols.rounded.Code as CodeRegular
import com.composables.icons.materialsymbols.rounded.Download as DownloadRegular
import com.composables.icons.materialsymbols.rounded.Error as ErrorRegular
import com.composables.icons.materialsymbols.rounded.Explore as ExploreRegular
import com.composables.icons.materialsymbols.rounded.Favorite as FavoriteRegular
import com.composables.icons.materialsymbols.rounded.Folder as FolderRegular
import com.composables.icons.materialsymbols.rounded.History as HistoryRegular
import com.composables.icons.materialsymbols.rounded.Home as HomeRegular
import com.composables.icons.materialsymbols.rounded.Info as InfoRegular
import com.composables.icons.materialsymbols.rounded.Logout as LogoutRegular
import com.composables.icons.materialsymbols.rounded.Settings as SettingsRegular
import com.composables.icons.materialsymbols.rounded.Star as StarRegular
import com.composables.icons.materialsymbols.rounded.Tune as TuneRegular
import com.composables.icons.materialsymbols.roundedfilled.Account_circle
import com.composables.icons.materialsymbols.roundedfilled.Announcement
import com.composables.icons.materialsymbols.roundedfilled.Archive
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Auto_awesome
import com.composables.icons.materialsymbols.roundedfilled.Build
import com.composables.icons.materialsymbols.roundedfilled.Chat_bubble
import com.composables.icons.materialsymbols.roundedfilled.Check_circle
import com.composables.icons.materialsymbols.roundedfilled.Chevron_right
import com.composables.icons.materialsymbols.roundedfilled.Code
import com.composables.icons.materialsymbols.roundedfilled.Download
import com.composables.icons.materialsymbols.roundedfilled.Error
import com.composables.icons.materialsymbols.roundedfilled.Explore
import com.composables.icons.materialsymbols.roundedfilled.Favorite
import com.composables.icons.materialsymbols.roundedfilled.Folder
import com.composables.icons.materialsymbols.roundedfilled.History
import com.composables.icons.materialsymbols.roundedfilled.Home
import com.composables.icons.materialsymbols.roundedfilled.Info
import com.composables.icons.materialsymbols.roundedfilled.Logout
import com.composables.icons.materialsymbols.roundedfilled.Settings
import com.composables.icons.materialsymbols.roundedfilled.Star
import com.composables.icons.materialsymbols.roundedfilled.Tune

enum class RockIcon {
    Home, Explore, Repositories, Builds, Downloads, Profile, Settings,
    Back, Chat, Check, Error, AccountCircle, Announcement, AutoAwesome,
    ChevronRight, Favorite, History, Info, Logout, Star, Tune, Archive, Code
}

fun RockIcon.vector(selected: Boolean = false): ImageVector = when (this) {
    RockIcon.Home -> if (selected) MaterialSymbols.RoundedFilled.Home else MaterialSymbols.Rounded.HomeRegular
    RockIcon.Explore -> if (selected) MaterialSymbols.RoundedFilled.Explore else MaterialSymbols.Rounded.ExploreRegular
    RockIcon.Repositories -> if (selected) MaterialSymbols.RoundedFilled.Folder else MaterialSymbols.Rounded.FolderRegular
    RockIcon.Builds -> if (selected) MaterialSymbols.RoundedFilled.Build else MaterialSymbols.Rounded.BuildRegular
    RockIcon.Downloads -> if (selected) MaterialSymbols.RoundedFilled.Download else MaterialSymbols.Rounded.DownloadRegular
    RockIcon.Profile, RockIcon.AccountCircle -> if (selected) MaterialSymbols.RoundedFilled.Account_circle else MaterialSymbols.Rounded.AccountCircleRegular
    RockIcon.Settings -> if (selected) MaterialSymbols.RoundedFilled.Settings else MaterialSymbols.Rounded.SettingsRegular
    RockIcon.Back -> if (selected) MaterialSymbols.RoundedFilled.Arrow_back else MaterialSymbols.Rounded.ArrowBackRegular
    RockIcon.Chat -> if (selected) MaterialSymbols.RoundedFilled.Chat_bubble else MaterialSymbols.Rounded.ChatBubbleRegular
    RockIcon.Check -> if (selected) MaterialSymbols.RoundedFilled.Check_circle else MaterialSymbols.Rounded.CheckCircleRegular
    RockIcon.Error -> if (selected) MaterialSymbols.RoundedFilled.Error else MaterialSymbols.Rounded.ErrorRegular
    RockIcon.Announcement -> if (selected) MaterialSymbols.RoundedFilled.Announcement else MaterialSymbols.Rounded.AnnouncementRegular
    RockIcon.AutoAwesome -> if (selected) MaterialSymbols.RoundedFilled.Auto_awesome else MaterialSymbols.Rounded.AutoAwesomeRegular
    RockIcon.ChevronRight -> if (selected) MaterialSymbols.RoundedFilled.Chevron_right else MaterialSymbols.Rounded.ChevronRightRegular
    RockIcon.Favorite -> if (selected) MaterialSymbols.RoundedFilled.Favorite else MaterialSymbols.Rounded.FavoriteRegular
    RockIcon.History -> if (selected) MaterialSymbols.RoundedFilled.History else MaterialSymbols.Rounded.HistoryRegular
    RockIcon.Info -> if (selected) MaterialSymbols.RoundedFilled.Info else MaterialSymbols.Rounded.InfoRegular
    RockIcon.Logout -> if (selected) MaterialSymbols.RoundedFilled.Logout else MaterialSymbols.Rounded.LogoutRegular
    RockIcon.Star -> if (selected) MaterialSymbols.RoundedFilled.Star else MaterialSymbols.Rounded.StarRegular
    RockIcon.Tune -> if (selected) MaterialSymbols.RoundedFilled.Tune else MaterialSymbols.Rounded.TuneRegular
    RockIcon.Archive -> if (selected) MaterialSymbols.RoundedFilled.Archive else MaterialSymbols.Rounded.ArchiveRegular
    RockIcon.Code -> if (selected) MaterialSymbols.RoundedFilled.Code else MaterialSymbols.Rounded.CodeRegular
}
