package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.core.util.ReleaseAssetClassifier
import com.sayanthrock.githubrock.core.util.ReleaseAssetInfo
import com.sayanthrock.githubrock.core.util.ReleasePlatform
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryArtwork

internal enum class ReleaseFilter(val label: String) {
    Stable("Stable"),
    PreRelease("Pre-release"),
    All("All")
}

internal fun initialReleaseFilter(
    releases: List<Release>,
    initialTag: String?
): ReleaseFilter {
    val published = releases.filterNot(Release::draft)
    val linkedRelease = published.firstOrNull { it.tagName == initialTag }
    return when {
        linkedRelease?.prerelease == true -> ReleaseFilter.PreRelease
        linkedRelease != null -> ReleaseFilter.Stable
        published.any { !it.prerelease } -> ReleaseFilter.Stable
        published.any(Release::prerelease) -> ReleaseFilter.PreRelease
        else -> ReleaseFilter.All
    }
}

@Composable
fun RepositoryHubContent(
    repository: GitHubRepositoryModel?,
    releases: List<Release>,
    readme: String?,
    loading: Boolean,
    releasesLoading: Boolean,
    readmeLoading: Boolean,
    error: String?,
    releasesError: String?,
    readmeError: String?,
    initialTag: String? = null,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDownload: (ReleaseAsset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (loading && repository == null) {
            item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        error?.let { message ->
            item(key = "error") { RepositoryErrorCard(message = message, onRetry = onRetry) }
        }

        repository?.let { repo ->
            item(key = "hero") { RepositoryHubHero(repo) }
            item(key = "tools") { RepositoryQuickTools(repo, onOpenUrl) }
            item(key = "releases") {
                RepositoryReleasePanel(
                    releases = releases,
                    loading = releasesLoading,
                    error = releasesError,
                    initialTag = initialTag,
                    onDownload = onDownload
                )
            }
            item(key = "stats") { RepositoryStats(repo, releases) }
            item(key = "actions") { RepositoryActionButtons(repo, onOpenUrl) }
            releases.firstOrNull { !it.draft }?.let { release ->
                item(key = "whats_new_${release.id}") { WhatsNewCard(release) }
            }
        }

        item(key = "readme_title") { ReadmeTitle() }
        when {
            readmeLoading -> item(key = "readme_loading") {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Loading README…", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            readme != null -> item(key = "readme_content") { RepositoryMarkdownCard(readme) }
            readmeError != null -> item(key = "readme_error") {
                GlassCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Text(readmeError, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryHubHero(repository: GitHubRepositoryModel) {
    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Box(Modifier.fillMaxWidth()) {
                RepositoryArtwork(repository = repository, compact = false)
                RepositoryHubIcon(
                    repository = repository,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 18.dp, y = 34.dp)
                )
            }
            Spacer(Modifier.height(42.dp))
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            repository.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            repository.fullName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    RepositoryKindPill(repository)
                }
                Text(
                    repository.description ?: "This repository does not have a description yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniPill(if (repository.private) "Private" else "Public")
                    repository.language?.let { MiniPill(it) }
                    if (repository.topics.any { it.equals("android", true) }) MiniPill("Android")
                }
                Text(
                    "Last updated ${repository.updatedAt.take(10).ifBlank { "Unknown" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RepositoryHubIcon(repository: GitHubRepositoryModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(78.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
    ) {
        when {
            !repository.previewImageUrl.isNullOrBlank() -> AsyncImage(
                model = repository.previewImageUrl,
                contentDescription = "${repository.name} application icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            repository.owner.avatarUrl.isNotBlank() -> AsyncImage(
                model = repository.owner.avatarUrl,
                contentDescription = "${repository.name} application icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = .28f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = .18f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, contentDescription = "${repository.name} application icon")
            }
        }
    }
}

@Composable
private fun RepositoryKindPill(repository: GitHubRepositoryModel) {
    val isApp = repository.topics.any {
        it.equals("android", true) || it.equals("app", true) || it.endsWith("-app", true)
    }
    val label = when {
        repository.isTemplate -> "Template"
        isApp -> "Application"
        else -> "Repository"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .32f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .68f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RepositoryQuickTools(repository: GitHubRepositoryModel, onOpenUrl: (String) -> Unit) {
    val base = repository.htmlUrl.trimEnd('/')
    val tools = listOf(
        Triple("Code", Icons.Default.Code, base),
        Triple("Issues", Icons.Default.Description, "$base/issues"),
        Triple("Pull requests", Icons.Default.Description, "$base/pulls"),
        Triple("Actions", Icons.Default.Build, "$base/actions"),
        Triple("Releases", Icons.Default.Download, "$base/releases")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Repository tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            tools.forEach { (label, icon, url) ->
                Surface(
                    onClick = { onOpenUrl(url) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .56f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryReleasePanel(
    releases: List<Release>,
    loading: Boolean,
    error: String?,
    initialTag: String?,
    onDownload: (ReleaseAsset) -> Unit
) {
    var filter by rememberSaveable(initialTag, releases) {
        mutableStateOf(initialReleaseFilter(releases, initialTag))
    }
    val visibleReleases = releases.filterNot { it.draft }.filter { release ->
        when (filter) {
            ReleaseFilter.Stable -> !release.prerelease
            ReleaseFilter.PreRelease -> release.prerelease
            ReleaseFilter.All -> true
        }
    }
    var selectedTag by rememberSaveable(releases, filter, initialTag) {
        mutableStateOf(
            initialTag?.takeIf { tag -> visibleReleases.any { it.tagName == tag } }
                ?: visibleReleases.firstOrNull()?.tagName
        )
    }
    val selectedRelease = visibleReleases.firstOrNull { it.tagName == selectedTag }
        ?: visibleReleases.firstOrNull()
    val releaseAssets = selectedRelease?.assets.orEmpty()
    var selectedPlatformName by rememberSaveable(selectedRelease?.id) {
        mutableStateOf(ReleaseAssetClassifier.preferredPlatform(releaseAssets).name)
    }
    val selectedPlatform = ReleasePlatform.entries
        .firstOrNull { it.name == selectedPlatformName }
        ?: ReleasePlatform.Other
    val platformAssets = ReleaseAssetClassifier.assetsForPlatform(releaseAssets, selectedPlatform)
    var selectedAssetId by rememberSaveable(selectedRelease?.id, selectedPlatformName) {
        mutableStateOf(ReleaseAssetClassifier.preferredAsset(releaseAssets, selectedPlatform)?.id)
    }
    val selectedAsset = platformAssets.firstOrNull { it.id == selectedAssetId }
        ?: ReleaseAssetClassifier.preferredAsset(releaseAssets, selectedPlatform)
    val selectedAssetInfo = selectedAsset?.let { ReleaseAssetClassifier.classify(it.name) }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Get the app", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Choose a platform, version, and file from the publisher's GitHub Release.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReleaseFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.label) }
                )
            }
        }

        when {
            loading -> GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Loading releases…")
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            visibleReleases.isEmpty() -> GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("No ${filter.label.lowercase()} release found", fontWeight = FontWeight.Bold)
                    Text(
                        error ?: "This repository has no matching published release assets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> GlassCard {
                val activeRelease = requireNotNull(selectedRelease)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReleasePickerHeader(activeRelease)
                    ReleaseDropdown(
                        label = "Version",
                        value = activeRelease.tagName,
                        options = visibleReleases.map { it.tagName },
                        onSelected = { selectedTag = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ReleasePlatformSelector(
                        assets = releaseAssets,
                        selected = selectedPlatform,
                        onSelected = { selectedPlatformName = it.name }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            "Choose a file",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (platformAssets.isEmpty()) {
                            EmptyPlatformDownload(platform = selectedPlatform)
                        } else {
                            platformAssets.forEach { asset ->
                                ReleaseAssetOption(
                                    asset = asset,
                                    selected = asset.id == selectedAsset?.id,
                                    onSelected = { selectedAssetId = asset.id }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { selectedAsset?.let(onDownload) },
                        enabled = selectedAsset != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .semantics { contentDescription = "Download selected release asset" },
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedAsset?.let { "Download for ${selectedPlatform.label}" }
                                    ?: "No ${selectedPlatform.label} download",
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                selectedAsset?.let { asset ->
                                    listOfNotNull(
                                        selectedAssetInfo?.format,
                                        selectedAssetInfo?.architecture,
                                        formatBytes(asset.size)
                                    ).joinToString(" · ")
                                } ?: "Try another platform or version",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                    ReleaseProtectionCard(
                        platform = selectedPlatform,
                        assetInfo = selectedAssetInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleasePickerHeader(release: Release) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                release.name?.takeIf(String::isNotBlank) ?: release.tagName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                release.publishedAt?.take(10)?.let { "Published $it" } ?: release.tagName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (release.prerelease) {
                MaterialTheme.colorScheme.secondary.copy(alpha = .13f)
            } else {
                MaterialTheme.colorScheme.tertiary.copy(alpha = .13f)
            }
        ) {
            Text(
                if (release.prerelease) "Pre-release" else "Stable",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = if (release.prerelease) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReleasePlatformSelector(
    assets: List<ReleaseAsset>,
    selected: ReleasePlatform,
    onSelected: (ReleasePlatform) -> Unit
) {
    val visiblePlatforms = ReleasePlatform.entries.filter { platform ->
        platform != ReleasePlatform.Other ||
            ReleaseAssetClassifier.assetsForPlatform(assets, ReleasePlatform.Other).isNotEmpty()
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Choose your platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Available files are detected from the release asset names.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            visiblePlatforms.forEach { platform ->
                val count = ReleaseAssetClassifier.assetsForPlatform(assets, platform).size
                ReleasePlatformTile(
                    platform = platform,
                    count = count,
                    selected = selected == platform,
                    onClick = { onSelected(platform) }
                )
            }
        }
    }
}

@Composable
private fun ReleasePlatformTile(
    platform: ReleasePlatform,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(126.dp)
            .semantics { contentDescription = "Select ${platform.label} downloads" },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = .13f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .55f)
            else MaterialTheme.colorScheme.outline.copy(alpha = .42f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(platformIcon(platform), contentDescription = null, tint = color)
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(platform.label, color = color, fontWeight = FontWeight.Bold)
            Text(
                "$count ${if (count == 1) "file" else "files"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyPlatformDownload(platform: ReleasePlatform) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .36f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("No ${platform.label} file in this release", fontWeight = FontWeight.Bold)
            Text(
                "Choose another platform or version. GitHub Rock only shows files the publisher actually uploaded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReleaseAssetOption(
    asset: ReleaseAsset,
    selected: Boolean,
    onSelected: () -> Unit
) {
    val info = ReleaseAssetClassifier.classify(asset.name)
    Surface(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = .1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .38f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .48f)
            else MaterialTheme.colorScheme.outline.copy(alpha = .34f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    asset.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniPill(info.format)
                    MiniPill(info.architecture)
                    MiniPill(formatBytes(asset.size))
                }
            }
        }
    }
}

@Composable
private fun ReleaseProtectionCard(
    platform: ReleasePlatform,
    assetInfo: ReleaseAssetInfo?
) {
    val explanation = when {
        assetInfo?.isSupportFile == true ->
            "This publisher-supplied checksum or signature file is saved beside your other downloads."
        platform == ReleasePlatform.Android && assetInfo?.format == "APK" ->
            "A SHA-256 fingerprint is recorded after download. APK package details, permissions, SDK levels, and signing certificate are inspected before Android's installer opens."
        platform == ReleasePlatform.Android && assetInfo?.format == "AAB" ->
            "A SHA-256 fingerprint is recorded after download. Android App Bundles are publishing artifacts and cannot be installed directly on a device."
        assetInfo != null ->
            "A SHA-256 fingerprint is recorded after download. ${platform.label} files are saved for sharing or transfer and cannot run on this Android device."
        else ->
            "Select a published file to enable the protected background download queue."
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = .08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .24f))
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Standard download protection", fontWeight = FontWeight.Bold)
                Text(
                    explanation,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "A local fingerprint proves file consistency, not publisher identity, unless it is compared with a trusted checksum or signature.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun platformIcon(platform: ReleasePlatform): ImageVector = when (platform) {
    ReleasePlatform.Android -> Icons.Default.Android
    ReleasePlatform.Windows -> Icons.Default.DesktopWindows
    ReleasePlatform.Linux -> Icons.Default.Terminal
    ReleasePlatform.IOS -> Icons.Default.PhoneIphone
    ReleasePlatform.MacOS -> Icons.Default.LaptopMac
    ReleasePlatform.Other -> Icons.Default.InsertDriveFile
}

@Composable
private fun ReleaseDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Box {
            Surface(
                onClick = { if (options.isNotEmpty()) expanded = true },
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .54f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value.ifBlank { "Unavailable" },
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryStats(repository: GitHubRepositoryModel, releases: List<Release>) {
    val assetCount = releases.sumOf { it.assets.size }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Forks", compactCount(repository.forks), Modifier.weight(1f))
            StatTile("Stars", compactCount(repository.stars), Modifier.weight(1.25f))
            StatTile("Issues", compactCount(repository.openIssues), Modifier.weight(.85f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Release assets", assetCount.toString(), Modifier.weight(1f))
            StatTile("Language", repository.language ?: "Not detected", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .44f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .52f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RepositoryActionButtons(repository: GitHubRepositoryModel, onOpenUrl: (String) -> Unit) {
    val base = repository.htmlUrl.trimEnd('/')
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { onOpenUrl("$base/issues") },
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Issues")
        }
        OutlinedButton(
            onClick = { onOpenUrl("$base/security") },
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Security")
        }
    }
}

@Composable
private fun WhatsNewCard(release: Release) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("What’s New", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            release.name?.takeIf(String::isNotBlank) ?: release.tagName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            release.tagName,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        release.publishedAt?.take(10) ?: "Unpublished",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                release.body?.takeIf(String::isNotBlank)?.let { body ->
                    MarkdownRenderer.render(body).take(MAX_RELEASE_BLOCKS).forEach { block ->
                        MarkdownBlockView(block)
                    }
                } ?: Text("No release notes were provided.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReadmeTitle() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("README.md", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Project documentation", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
        ) {
            Text(
                "Rendered",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RepositoryMarkdownCard(markdown: String) {
    val blocks = remember(markdown) { MarkdownRenderer.render(markdown) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blocks.take(MAX_README_BLOCKS).forEach { MarkdownBlockView(it) }
            if (blocks.size > MAX_README_BLOCKS) {
                HorizontalDivider()
                Text(
                    "README preview shortened for performance. Use the Code tool to open the complete repository documentation.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    when (block.kind) {
        MarkdownBlockKind.Heading -> Text(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                3 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold
        )
        MarkdownBlockKind.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(block.text, modifier = Modifier.weight(1f))
        }
        MarkdownBlockKind.Quote -> Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f))
        ) {
            Text(block.text, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MarkdownBlockKind.Code -> Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = .72f)
        ) {
            Text(
                block.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                softWrap = false
            )
        }
        MarkdownBlockKind.Divider -> HorizontalDivider()
        MarkdownBlockKind.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RepositoryErrorCard(message: String, onRetry: () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try again")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> {
        val whole = value / 1_000_000
        val decimal = (value % 1_000_000) / 100_000
        if (decimal == 0) "${whole}M" else "$whole.${decimal}M"
    }
    value >= 1_000 -> {
        val whole = value / 1_000
        val decimal = (value % 1_000) / 100
        if (decimal == 0) "${whole}k" else "$whole.${decimal}k"
    }
    else -> value.toString()
}

private const val MAX_README_BLOCKS = 20
private const val MAX_RELEASE_BLOCKS = 10
