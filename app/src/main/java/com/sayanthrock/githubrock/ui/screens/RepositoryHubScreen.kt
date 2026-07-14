package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.model.ReleaseAsset
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryArtwork
import kotlinx.coroutines.launch

private enum class ReleaseFilter(val label: String) {
    Stable("Stable"),
    PreRelease("Pre-release"),
    All("All")
}

/** Single repository page containing identity, releases, tools, statistics and documentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryHubScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    initialTag: String? = null,
    viewModel: RepositoryHubViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository?.id) {
        viewModel.start(repository)
    }

    val displayedRepository = state.repository ?: repository
    val openUrl: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            displayedRepository?.name ?: "Repository",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        displayedRepository?.owner?.login?.let {
                            Text(
                                "@$it",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url ->
                        IconButton(onClick = { openUrl(url) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open repository on GitHub")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f)
                )
            )
        }
    ) { padding ->
        RepositoryHubContent(
            repository = displayedRepository,
            releases = state.releases,
            readme = state.readme,
            loading = state.loading,
            releasesLoading = state.releasesLoading,
            readmeLoading = state.readmeLoading,
            error = state.error,
            releasesError = state.releasesError,
            readmeError = state.readmeError,
            initialTag = initialTag,
            onRetry = viewModel::retry,
            onOpenUrl = openUrl,
            onDownload = { asset ->
                if (asset.downloadUrl.startsWith("https://example.com")) {
                    scope.launch { snackbar.showSnackbar("Demo assets are preview-only") }
                } else {
                    downloadsViewModel.enqueue(asset.downloadUrl, asset.name)
                    scope.launch { snackbar.showSnackbar("${asset.name} added to Downloads") }
                }
            },
            modifier = Modifier.padding(padding)
        )
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
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (loading && repository == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        error?.let { message ->
            item { RepositoryErrorCard(message = message, onRetry = onRetry) }
        }

        repository?.let { repo ->
            item { RepositoryHubHero(repo) }
            item { RepositoryQuickTools(repo, onOpenUrl) }
            item {
                RepositoryReleasePanel(
                    releases = releases,
                    loading = releasesLoading,
                    error = releasesError,
                    initialTag = initialTag,
                    onDownload = onDownload
                )
            }
            item { RepositoryStats(repo, releases) }
            item { RepositoryActionButtons(repo, onOpenUrl) }
            val latestRelease = releases.firstOrNull { !it.draft }
            if (latestRelease != null) {
                item { WhatsNewCard(latestRelease) }
            }
        }

        item { ReadmeTitle() }
        when {
            readmeLoading -> item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Loading README…", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            readme != null -> item { RepositoryMarkdownCard(readme) }
            readmeError != null -> item {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    var filter by rememberSaveable { mutableStateOf(ReleaseFilter.Stable) }
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
    var selectedAssetId by rememberSaveable(selectedRelease?.id) {
        mutableStateOf(preferredAsset(selectedRelease?.assets.orEmpty())?.id)
    }
    val selectedAsset = selectedRelease?.assets?.firstOrNull { it.id == selectedAssetId }
        ?: preferredAsset(selectedRelease?.assets.orEmpty())

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("Latest release", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
                Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReleaseDropdown(
                            label = "Asset",
                            value = selectedAsset?.name ?: "No asset",
                            modifier = Modifier.weight(1.35f),
                            options = selectedRelease?.assets.orEmpty().map { it.name },
                            onSelected = { name ->
                                selectedAssetId = selectedRelease?.assets?.firstOrNull { it.name == name }?.id
                            }
                        )
                        ReleaseDropdown(
                            label = "Version",
                            value = selectedRelease?.tagName.orEmpty(),
                            modifier = Modifier.weight(.9f),
                            options = visibleReleases.map { it.tagName },
                            onSelected = { selectedTag = it }
                        )
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
                            Text("Download & inspect", fontWeight = FontWeight.Black)
                            Text(
                                selectedAsset?.let { "${assetArchitecture(it.name)} · ${formatBytes(it.size)}" }
                                    ?: "No installable asset",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    Text(
                        "Downloaded APK files are verified and inspected before Android's installer opens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
                val body = release.body?.takeIf(String::isNotBlank)
                if (body != null) {
                    MarkdownRenderer.render(body).take(MAX_RELEASE_BLOCKS).forEach { block ->
                        MarkdownBlockView(block)
                    }
                } else {
                    Text("No release notes were provided.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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

private fun preferredAsset(assets: List<ReleaseAsset>): ReleaseAsset? =
    assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.name.contains("arm64", true) }
        ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        ?: assets.firstOrNull { it.name.endsWith(".aab", ignoreCase = true) }
        ?: assets.firstOrNull()

private fun assetArchitecture(name: String): String = when {
    name.contains("arm64", true) || name.contains("aarch64", true) -> "arm64-v8a"
    name.contains("armeabi", true) || name.contains("armv7", true) -> "armeabi-v7a"
    name.contains("x86_64", true) -> "x86_64"
    name.contains("universal", true) -> "universal"
    else -> "release asset"
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
