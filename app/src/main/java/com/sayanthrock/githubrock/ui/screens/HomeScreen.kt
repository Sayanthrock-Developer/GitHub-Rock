package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled
import java.time.Duration
import java.time.Instant
import java.util.Locale

internal enum class HomePlatform(
    val label: String,
    val icon: ImageVector,
) {
    All("All Platforms", Icons.Default.Devices),
    Android("Android", Icons.Default.Android),
    MacOS("macOS", Icons.Default.LaptopMac),
    Windows("Windows", Icons.Default.DesktopWindows),
    Linux("Linux", Icons.Default.Terminal),
    IOS("iOS", Icons.Default.PhoneIphone),
}

internal enum class HomeCategory(
    val label: String,
    val keywords: Set<String>,
) {
    All("All", emptySet()),
    AI(
        "AI",
        setOf(
            "ai",
            "artificial intelligence",
            "machine learning",
            "deep learning",
            "llm",
            "gpt",
            "neural",
            "computer vision",
        ),
    ),
    Privacy(
        "Privacy",
        setOf("privacy", "encryption", "encrypted", "vpn", "password", "secure", "security"),
    ),
    Networking(
        "Networking",
        setOf("network", "networking", "server", "remote desktop", "ssh", "proxy", "socket", "http"),
    ),
    Media(
        "Media",
        setOf("photo", "image", "video", "camera", "wallpaper", "audio", "music", "media"),
    ),
    Developer(
        "Developer",
        setOf(
            "developer",
            "github",
            "git",
            "android",
            "kotlin",
            "compose",
            "gradle",
            "sdk",
            "build",
            "ci",
            "automation",
        ),
    ),
    Utilities(
        "Utilities",
        setOf("utility", "utilities", "tool", "manager", "battery", "screen", "ota", "qr"),
    ),
    Games(
        "Games",
        setOf("game", "gaming", "godot", "unity", "engine"),
    ),
}

internal enum class HomeSort(val label: String) {
    Updated("Updated"),
    Popular("Popular"),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun HomeScreen(
    repositories: List<GitHubRepositoryModel>,
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    var selectedPlatformName by rememberSaveable { mutableStateOf(HomePlatform.All.name) }
    var selectedCategoryName by rememberSaveable { mutableStateOf(HomeCategory.All.name) }
    var selectedSortName by rememberSaveable { mutableStateOf(HomeSort.Updated.name) }
    var showPlatformSheet by rememberSaveable { mutableStateOf(false) }

    val selectedPlatform = HomePlatform.entries.firstOrNull { it.name == selectedPlatformName }
        ?: HomePlatform.All
    val selectedCategory = HomeCategory.entries.firstOrNull { it.name == selectedCategoryName }
        ?: HomeCategory.All
    val selectedSort = HomeSort.entries.firstOrNull { it.name == selectedSortName }
        ?: HomeSort.Updated

    val publicRepositoryCount = remember(repositories) {
        repositories.count { !it.private }
    }
    val visibleRepositories = remember(
        repositories,
        selectedPlatform,
        selectedCategory,
        selectedSort,
    ) {
        homeRepositoryFeed(
            repositories = repositories,
            platform = selectedPlatform,
            category = selectedCategory,
            sort = selectedSort,
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = StandardScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "GitHub Rock",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }

            item {
                HomeBrowsingHeader(
                    selectedPlatform = selectedPlatform,
                    onSelectPlatform = { showPlatformSheet = true },
                )
            }

            item {
                HomeCategoryRow(
                    selected = selectedCategory,
                    onSelected = { selectedCategoryName = it.name },
                )
            }

            item {
                HomeResultsHeader(
                    category = selectedCategory,
                    repositoryCount = visibleRepositories.size,
                    selectedSort = selectedSort,
                    onToggleSort = {
                        selectedSortName = if (selectedSort == HomeSort.Updated) {
                            HomeSort.Popular.name
                        } else {
                            HomeSort.Updated.name
                        }
                    },
                )
            }

            if (isLoading) {
                item { LoadingWorkspaceCard() }
            }

            if (!isLoading && visibleRepositories.isEmpty()) {
                item {
                    EmptyDiscoveryCard(
                        hasPublicRepositories = publicRepositoryCount > 0,
                        onClearFilters = {
                            selectedPlatformName = HomePlatform.All.name
                            selectedCategoryName = HomeCategory.All.name
                        },
                        onRefresh = onRefresh,
                    )
                }
            }

            items(
                items = visibleRepositories.take(30),
                key = { it.id },
            ) { repository ->
                DiscoveryRepositoryCard(
                    repository = repository,
                    rank = if (selectedSort == HomeSort.Popular) {
                        visibleRepositories.indexOf(repository) + 1
                    } else {
                        null
                    },
                    onClick = { onOpenRepo(repository) },
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showPlatformSheet) {
        HomePlatformPickerSheet(
            selected = selectedPlatform,
            onSelect = {
                selectedPlatformName = it.name
                showPlatformSheet = false
            },
            onDismiss = { showPlatformSheet = false },
        )
    }
}

@Composable
private fun HomeBrowsingHeader(
    selectedPlatform: HomePlatform,
    onSelectPlatform: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Browsing",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        Surface(
            onClick = onSelectPlatform,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = selectedPlatform.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = selectedPlatform.label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Choose platform",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeCategoryRow(
    selected: HomeCategory,
    onSelected: (HomeCategory) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(HomeCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelected(category) },
                label = {
                    Text(
                        text = category.label,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun HomeResultsHeader(
    category: HomeCategory,
    repositoryCount: Int,
    selectedSort: HomeSort,
    onToggleSort: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "$repositoryCount repositor${if (repositoryCount == 1) "y" else "ies"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onToggleSort,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = selectedSort.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LoadingWorkspaceCard() {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Loading GitHub workspace" },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Column {
                Text("Loading your workspace…", fontWeight = FontWeight.Bold)
                Text(
                    "Fetching public repositories and discovery metadata.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyDiscoveryCard(
    hasPublicRepositories: Boolean,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (hasPublicRepositories) {
                    "No public repositories match"
                } else {
                    "No public repositories found"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (hasPublicRepositories) {
                    "Try another platform or category."
                } else {
                    "Pull down to refresh public repositories."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = if (hasPublicRepositories) onClearFilters else onRefresh) {
                Text(if (hasPublicRepositories) "Show all public repositories" else "Refresh")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DiscoveryRepositoryCard(
    repository: GitHubRepositoryModel,
    rank: Int?,
    onClick: () -> Unit,
) {
    val showImages = LocalRemoteImagesEnabled.current
    val platforms = remember(repository) { repositoryPlatforms(repository) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    if (showImages && repository.owner.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = repository.owner.avatarUrl,
                            contentDescription = "${repository.owner.login} avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = repository.name.take(2).uppercase(Locale.US),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rank?.let {
                            Text(
                                text = "#$it",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Text(
                            text = repository.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = buildString {
                            append("@")
                            append(repository.owner.login)
                            repository.language?.takeIf(String::isNotBlank)?.let {
                                append("  •  ")
                                append(it)
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = repository.description ?: "No repository description provided.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (platforms.isEmpty()) {
                    RepositoryPlatformChip(
                        icon = Icons.Default.Devices,
                        label = "Cross-platform",
                    )
                } else {
                    platforms.forEach { platform ->
                        RepositoryPlatformChip(
                            icon = platform.icon,
                            label = platform.label,
                        )
                    }
                }
                RepositoryPlatformChip(
                    icon = Icons.Default.Check,
                    label = "Public",
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepositoryFooterMetric(
                    icon = Icons.Default.Star,
                    value = compactCount(repository.stars),
                )
                RepositoryFooterMetric(
                    icon = Icons.Default.CallSplit,
                    value = compactCount(repository.forks),
                )
                RepositoryFooterMetric(
                    icon = Icons.Default.Schedule,
                    value = relativeRepositoryTime(repository.updatedAt),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open ${repository.name}",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun RepositoryPlatformChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RepositoryFooterMetric(
    icon: ImageVector,
    value: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePlatformPickerSheet(
    selected: HomePlatform,
    onSelect: (HomePlatform) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Choose your OS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )

            HomePlatform.entries.forEach { platform ->
                val isSelected = platform == selected
                Surface(
                    onClick = { onSelect(platform) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = platform.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = platform.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun homeRepositoryFeed(
    repositories: List<GitHubRepositoryModel>,
    platform: HomePlatform,
    category: HomeCategory,
    sort: HomeSort,
): List<GitHubRepositoryModel> {
    val filtered = repositories.asSequence()
        .filterNot(GitHubRepositoryModel::private)
        .filter { repository ->
            if (platform == HomePlatform.All) {
                true
            } else {
                val supportedPlatforms = repositoryPlatforms(repository)
                supportedPlatforms.isEmpty() || platform in supportedPlatforms
            }
        }
        .filter { repositoryMatchesCategory(it, category) }

    return when (sort) {
        HomeSort.Updated -> filtered
            .sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.updatedAt }
                    .thenByDescending { it.stars },
            )
            .toList()
        HomeSort.Popular -> filtered
            .sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.stars }
                    .thenByDescending { it.updatedAt },
            )
            .toList()
    }
}

internal fun repositoryPlatforms(repository: GitHubRepositoryModel): List<HomePlatform> {
    val text = repositoryDiscoveryText(repository)
    val language = repository.language.orEmpty().lowercase(Locale.US)
    val platforms = linkedSetOf<HomePlatform>()

    if (matchesAny(text, ANDROID_KEYWORDS)) platforms += HomePlatform.Android
    if (matchesAny(text, MACOS_KEYWORDS)) platforms += HomePlatform.MacOS
    if (matchesAny(text, WINDOWS_KEYWORDS)) platforms += HomePlatform.Windows
    if (matchesAny(text, LINUX_KEYWORDS)) platforms += HomePlatform.Linux
    if (matchesAny(text, IOS_KEYWORDS)) platforms += HomePlatform.IOS

    if (language in setOf("swift", "objective c", "objective-c")) {
        platforms += HomePlatform.MacOS
        platforms += HomePlatform.IOS
    }
    if (language in setOf("c#", "powershell")) platforms += HomePlatform.Windows

    return platforms.toList()
}

internal fun repositoryMatchesCategory(
    repository: GitHubRepositoryModel,
    category: HomeCategory,
): Boolean {
    if (category == HomeCategory.All) return true
    val text = repositoryDiscoveryText(repository)
    return matchesAny(text, category.keywords)
}

internal fun relativeRepositoryTime(
    value: String,
    now: Instant = Instant.now(),
): String {
    val updated = runCatching { Instant.parse(value) }.getOrNull() ?: return "Updated"
    val seconds = Duration.between(updated, now).seconds.coerceAtLeast(0L)
    return when {
        seconds < 60L -> "now"
        seconds < 3_600L -> "${seconds / 60L} m ago"
        seconds < 86_400L -> "${seconds / 3_600L} h ago"
        seconds < 604_800L -> "${seconds / 86_400L} d ago"
        seconds < 2_592_000L -> "${seconds / 604_800L} w ago"
        seconds < 31_536_000L -> "${seconds / 2_592_000L} mo ago"
        else -> "${seconds / 31_536_000L} y ago"
    }
}

private fun repositoryDiscoveryText(repository: GitHubRepositoryModel): String = normalizeDiscoveryText(
    listOfNotNull(
        repository.name,
        repository.fullName,
        repository.description,
        repository.language,
        repository.topics.joinToString(" "),
    ).joinToString(" "),
)

private fun normalizeDiscoveryText(value: String): String = value
    .lowercase(Locale.US)
    .replace(Regex("[^a-z0-9+#]+"), " ")
    .trim()

private fun matchesAny(text: String, keywords: Set<String>): Boolean = keywords.any { keyword ->
    val normalizedKeyword = normalizeDiscoveryText(keyword)
    if (normalizedKeyword.length <= 2) {
        text.split(' ').any { it == normalizedKeyword }
    } else {
        text.contains(normalizedKeyword)
    }
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}

private val ANDROID_KEYWORDS = setOf(
    "android",
    "apk",
    "aab",
    "jetpack compose",
    "android studio",
)
private val MACOS_KEYWORDS = setOf("macos", "mac os", "osx", "appkit", "cocoa")
private val WINDOWS_KEYWORDS = setOf("windows", "win32", "winui", "wpf", "uwp")
private val LINUX_KEYWORDS = setOf("linux", "appimage", "flatpak", "gtk", "wayland", "x11")
private val IOS_KEYWORDS = setOf("ios", "iphone", "ipad", "swiftui", "uikit")
