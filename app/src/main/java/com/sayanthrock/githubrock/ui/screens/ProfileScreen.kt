package com.sayanthrock.githubrock.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubProfileSnapshot
import com.sayanthrock.githubrock.core.navigation.GITHUB_ACCOUNT_SECURITY_URL
import com.sayanthrock.githubrock.core.navigation.normalizedGitHubLogin
import com.sayanthrock.githubrock.core.util.ProfileExportFormatter
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.ProfileExplorerState
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CREATOR_PROFILE_URL = "https://github.com/SayanthRock"

@Composable
fun ProfileScreen(
    mode: AppMode,
    profile: GitHubUser?,
    explorerState: ProfileExplorerState = ProfileExplorerState(),
    onInspectProfile: (String) -> Unit = {},
    onFollowProfile: (Boolean) -> Unit = {},
    onOpenRepositories: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenGitHubUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val displayedProfile = explorerState.snapshot?.profile ?: profile
    val profileDetails = explorerState.snapshot?.details
    val connectedLogin = normalizedGitHubLogin(displayedProfile?.login)
    val profileUrl = connectedLogin?.let { "https://github.com/$it" }
    val ownLogin = normalizedGitHubLogin(profile?.login)
    val viewingOwnProfile = connectedLogin != null && connectedLogin.equals(ownLogin, ignoreCase = true)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember(displayedProfile?.id) { mutableStateOf<String?>(null) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var requestedOwnProfile by rememberSaveable(ownLogin) { mutableStateOf(false) }

    LaunchedEffect(mode, ownLogin) {
        if (!requestedOwnProfile && ownLogin != null) {
            requestedOwnProfile = true
            onInspectProfile(ownLogin)
        }
    }

    val profileExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val user = displayedProfile
        if (user == null) {
            exportMessage = "Profile data is no longer available. Refresh and try again."
            return@rememberLauncherForActivityResult
        }
        val content = ProfileExportFormatter.toJson(user)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(content)
                    } ?: error("Unable to create the profile file")
                }
            }.onSuccess {
                exportMessage = "Profile downloaded successfully"
            }.onFailure { error ->
                exportMessage = error.message ?: "Unable to download this profile"
            }
        }
    }

    val downloadProfile: () -> Unit = {
        displayedProfile?.let { user ->
            exportMessage = null
            profileExportLauncher.launch(ProfileExportFormatter.fileName(user))
        } ?: run {
            exportMessage = "Profile data is unavailable. Refresh and try again."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = StandardScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "Profile",
                subtitle = when (mode) {
                    AppMode.Connected -> "Your account, library, and app settings"
                    AppMode.Guest -> "Public browsing session"
                    AppMode.Demo -> "Isolated demonstration workspace"
                }
            )
        }

        item {
            ProfileSearchCard(
                currentLogin = connectedLogin,
                ownLogin = ownLogin,
                loading = explorerState.loading,
                onSearch = onInspectProfile
            )
        }

        explorerState.error?.let { message ->
            item {
                GlassCard { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            }
        }

        if (explorerState.loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        item {
            ProfileHero(
                mode = mode,
                profile = displayedProfile,
                onOpenRepositories = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=repositories") }
                },
                onOpenFollowers = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=followers") }
                },
                onOpenFollowing = connectedLogin?.let { login ->
                    { onOpenGitHubUrl("https://github.com/$login?tab=following") }
                }
            )
        }

        if (mode == AppMode.Connected && !viewingOwnProfile && displayedProfile != null) {
            item {
                Button(
                    onClick = { onFollowProfile(explorerState.snapshot?.isFollowing != true) },
                    enabled = !explorerState.followUpdating,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (explorerState.snapshot?.isFollowing == true) "Unfollow @${displayedProfile.login}" else "Follow @${displayedProfile.login}")
                }
            }
        }

        if (profileUrl != null) {
            item {
                Button(
                    onClick = { onOpenGitHubUrl(profileUrl) },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("View profile on GitHub", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                }
            }
        }

        if (displayedProfile != null) {
            item {
                ProfileInformationCard(
                    profile = displayedProfile,
                    details = profileDetails,
                    onOpenLink = { url ->
                        if (url.startsWith("https://github.com/") || url.startsWith("https://gist.github.com/")) {
                            onOpenGitHubUrl(url)
                        } else {
                            runCatching { uriHandler.openUri(url) }
                        }
                    }
                )
            }
        }

        profileDetails?.contributionsLastYear?.let { total ->
            item {
                ContributionCard(
                    total = total,
                    details = profileDetails,
                    onOpen = { profileUrl?.let(onOpenGitHubUrl) }
                )
            }
        }

        if (profileDetails != null) {
            item {
                ProfileCommunityCard(
                    login = displayedProfile?.login.orEmpty(),
                    details = profileDetails,
                    onOpenGitHubUrl = onOpenGitHubUrl,
                    onOpenLink = { url -> runCatching { uriHandler.openUri(url) } }
                )
            }
        }

        exportMessage?.let { message ->
            item {
                GlassCard {
                    Text(
                        message,
                        color = if (message.contains("success", ignoreCase = true)) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item { StandardSectionHeader("Essentials") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Repository library",
                    subtitle = "Browse connected projects and source code",
                    onClick = onOpenRepositories
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "Artifacts, releases, progress, and APK inspection",
                    onClick = onOpenDownloads
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Theme, accent, images, card density, and motion",
                    onClick = onOpenAppearance
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Info,
                    title = "App & SDK information",
                    subtitle = "Version, Android APIs, device, and installation details",
                    onClick = onOpenAppInfo
                )
                StandardSettingsDivider()
                StandardSettingsRow(
                    icon = Icons.Default.Code,
                    title = "All GitHub services",
                    subtitle = "Profile, stars, Gists, organizations, Copilot, settings, and plans",
                    onClick = onOpenFeatures
                )
            }
        }

        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("More tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Advanced GitHub and account controls",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide" else "Show")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    if (showAdvanced) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StandardSettingsGroup {
                            StandardSettingsRow(
                                icon = Icons.Default.Lock,
                                title = "GitHub security",
                                subtitle = "Passkeys, two-factor authentication, and sessions",
                                onClick = { onOpenGitHubUrl(GITHUB_ACCOUNT_SECURITY_URL) }
                            )
                            if (displayedProfile != null) {
                                StandardSettingsDivider()
                                StandardSettingsRow(
                                    icon = Icons.Default.Download,
                                    title = "Download profile",
                                    subtitle = "Save public account details and statistics as JSON",
                                    onClick = downloadProfile
                                )
                            }
                            StandardSettingsDivider()
                            StandardSettingsRow(
                                icon = Icons.Default.Info,
                                title = "Feature status",
                                subtitle = "Native coverage, web tools, and roadmap",
                                onClick = onOpenFeatures
                            )
                        }
                    }
                }
            }
        }

        item { StandardSectionHeader("About") }
        item {
            AboutCreatorCard(onOpenGitHubUrl = onOpenGitHubUrl)
        }

        item { StandardSectionHeader("Account") }
        item {
            StandardSettingsGroup {
                StandardSettingsRow(
                    icon = Icons.Default.Logout,
                    title = if (mode == AppMode.Connected) {
                        "Log out and delete token"
                    } else {
                        "Exit ${mode.name.lowercase()} mode"
                    },
                    subtitle = "Remove this session from the device",
                    destructive = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun ProfileSearchCard(
    currentLogin: String?,
    ownLogin: String?,
    loading: Boolean,
    onSearch: (String) -> Unit
) {
    var query by rememberSaveable(currentLogin) { mutableStateOf(currentLogin.orEmpty()) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Find a GitHub profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Search by username to inspect public profile information on your phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(39) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("GitHub username") },
                placeholder = { Text("octocat") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onSearch(query.trim()) }, enabled = !loading) {
                        Icon(Icons.Default.Search, contentDescription = "Search GitHub profile")
                    }
                }
            )
            if (ownLogin != null && !currentLogin.equals(ownLogin, ignoreCase = true)) {
                TextButton(onClick = { query = ownLogin; onSearch(ownLogin) }) { Text("Back to my profile") }
            }
        }
    }
}

@Composable
private fun ProfileInformationCard(
    profile: GitHubUser,
    details: GitHubProfileDetails?,
    onOpenLink: (String) -> Unit
) {
    val website = remember(profile.blog) { normalizedProfileLink(profile.blog) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            details?.pronouns?.let { ProfileInformationRow("Pronouns", it) }
            profile.company?.takeIf(String::isNotBlank)?.let { ProfileInformationRow("Company", it) }
            profile.location?.takeIf(String::isNotBlank)?.let { ProfileInformationRow("Location", it) }
            ProfileInformationRow("Current local time", CurrentLocalTime())
            profile.createdAt?.takeIf(String::isNotBlank)?.let { ProfileInformationRow("Joined GitHub", it.substringBefore('T')) }
            profile.email?.takeIf(String::isNotBlank)?.let { ProfileInformationRow("Public email", it) }
            website?.let { link ->
                TextButton(onClick = { onOpenLink(link) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open website · ${profile.blog}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ProfileInformationRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(118.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CurrentLocalTime(): String {
    var value by remember { mutableStateOf(formatLocalTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            value = formatLocalTime()
            delay(30_000)
        }
    }
    return value
}

private fun formatLocalTime(): String = ZonedDateTime.now().format(
    DateTimeFormatter.ofPattern("EEE, dd MMM · HH:mm z", Locale.getDefault())
)

@Composable
private fun ContributionCard(
    total: Int,
    details: GitHubProfileDetails,
    onOpen: () -> Unit
) {
    val days = details.contributionDays.takeLast(371)
    val weeks = remember(days) { days.chunked(7) }
    val max = remember(days) { days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Contributions in the last year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$total contributions", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            if (weeks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(initial = Int.MAX_VALUE)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { day ->
                                val intensity = if (day.count == 0) .07f else (.22f + (.78f * day.count / max)).coerceAtMost(1f)
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                                ) {}
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onOpen) {
                Text("Open contribution graph on GitHub")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ProfileCommunityCard(
    login: String,
    details: GitHubProfileDetails,
    onOpenGitHubUrl: (String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Highlights & organizations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (details.highlights.isEmpty()) {
                Text("No public profile highlights were returned by GitHub.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.highlights.forEach { highlight -> AssistChip(onClick = {}, label = { Text(highlight) }) }
                }
            }
            Text("Organizations · ${details.organizationCount}", fontWeight = FontWeight.SemiBold)
            details.organizations.forEach { organization ->
                TextButton(
                    onClick = { onOpenGitHubUrl(organization.url.ifBlank { "https://github.com/${organization.login}" }) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(organization.name ?: organization.login, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Text("@${organization.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (details.socialAccounts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Links", fontWeight = FontWeight.SemiBold)
                details.socialAccounts.forEach { account ->
                    val label = if (account.url.contains("orcid.org/", ignoreCase = true)) {
                        "ORCID · ${account.displayName}"
                    } else {
                        "${account.provider.lowercase().replaceFirstChar(Char::titlecase)} · ${account.displayName}"
                    }
                    TextButton(onClick = { onOpenLink(account.url) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                    }
                }
            }
            OutlinedButton(
                onClick = { onOpenGitHubUrl("https://github.com/$login?tab=achievements") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View achievements on GitHub")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
    }
}

internal fun normalizedProfileLink(value: String?): String? {
    val trimmed = value?.trim()?.takeIf(String::isNotBlank) ?: return null
    val normalized = when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.substringAfter("://")}"
        else -> "https://$trimmed"
    }
    return normalized.takeIf { runCatching { java.net.URI(it).host?.isNotBlank() == true }.getOrDefault(false) }
}

@Composable
private fun AboutCreatorCard(onOpenGitHubUrl: (String) -> Unit) {
    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "GitHub Rock application logo",
                        modifier = Modifier.padding(5.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("GitHub Rock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Visual developer control centre",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("By Sayanth Rock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    CREATOR_PROFILE_URL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { onOpenGitHubUrl(CREATOR_PROFILE_URL) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Follow me", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ProfileHero(
    mode: AppMode,
    profile: GitHubUser?,
    onOpenRepositories: (() -> Unit)?,
    onOpenFollowers: (() -> Unit)?,
    onOpenFollowing: (() -> Unit)?
) {
    GlassCard(contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(profile)
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.name ?: when (mode) {
                            AppMode.Guest -> "Guest"
                            AppMode.Demo -> "Demo profile"
                            AppMode.Connected -> profile?.login.orEmpty()
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile?.login?.let { "@$it" } ?: "Public access only",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            profile?.bio?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (profile != null && (!profile.location.isNullOrBlank() || !profile.blog.isNullOrBlank())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    profile.location?.takeIf(String::isNotBlank)?.let {
                        ProfileFact("Location", it, Modifier.weight(1f))
                    }
                    profile.blog?.takeIf(String::isNotBlank)?.let {
                        ProfileFact("Website", it, Modifier.weight(1f))
                    }
                }
            }

            if (profile != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(profile.publicRepos, "Repositories", Modifier.weight(1f), onOpenRepositories)
                    StatDivider()
                    ProfileStat(profile.followers, "Followers", Modifier.weight(1f), onOpenFollowers)
                    StatDivider()
                    ProfileStat(profile.following, "Following", Modifier.weight(1f), onOpenFollowing)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: GitHubUser?) {
    val showImages = LocalRemoteImagesEnabled.current
    Surface(
        modifier = Modifier.size(82.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (showImages && !profile?.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = profile?.avatarUrl,
                contentDescription = "GitHub avatar",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    profile?.login?.take(2)?.uppercase() ?: "GR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onClick)
    }
    Column(
        modifier = modifier.then(interactionModifier).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatDivider() {
    Surface(
        modifier = Modifier.width(1.dp).height(38.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    ) {}
}

@Composable
private fun ProfileFact(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}
