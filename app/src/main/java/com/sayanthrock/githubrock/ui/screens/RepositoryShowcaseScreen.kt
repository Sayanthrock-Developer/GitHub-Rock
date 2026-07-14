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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryArtwork

/**
 * Visual landing page shown before the advanced repository management workspace.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryShowcaseScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    onOpenWorkspace: () -> Unit,
    viewModel: RepositoryShowcaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(repository?.id) {
        viewModel.start(repository)
    }

    val displayedRepository = state.repository ?: repository

    Scaffold(
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
                    if (!displayedRepository?.htmlUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(displayedRepository?.htmlUrl))
                                )
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open repository on GitHub")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .94f)
                )
            )
        }
    ) { padding ->
        RepositoryShowcaseContent(
            repository = displayedRepository,
            readme = state.readme,
            loading = state.loading,
            readmeLoading = state.readmeLoading,
            error = state.error,
            readmeError = state.readmeError,
            onRetry = viewModel::retry,
            onOpenWorkspace = onOpenWorkspace,
            onOpenGitHub = {
                displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun RepositoryShowcaseContent(
    repository: GitHubRepositoryModel?,
    readme: String?,
    loading: Boolean,
    readmeLoading: Boolean,
    error: String?,
    readmeError: String?,
    onRetry: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (loading && repository == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        error?.let { message ->
            item {
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
        }

        repository?.let { repo ->
            item { RepositoryIdentityHero(repo) }
            item { RepositoryDescriptionCard(repo) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenWorkspace,
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Developer workspace")
                    }
                    OutlinedButton(
                        onClick = onOpenGitHub,
                        enabled = repo.htmlUrl.isNotBlank(),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("GitHub")
                    }
                }
            }
            item { RepositoryDetailsGrid(repo) }
            if (repo.topics.isNotEmpty()) {
                item { RepositoryTopics(repo.topics) }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("README.md", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Project documentation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        when {
            readmeLoading -> item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Loading README…", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            readme != null -> item {
                RepositoryReadmeCard(readme)
            }
            readmeError != null -> item {
                GlassCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(readmeError, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryIdentityHero(repository: GitHubRepositoryModel) {
    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Box(Modifier.fillMaxWidth()) {
                RepositoryArtwork(repository = repository, compact = false)
                RepositoryProjectIcon(
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
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            repository.fullName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    RepositoryTypeBadge(repository)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (repository.private) MiniBadge("Private")
                    if (repository.fork) MiniBadge("Fork")
                    if (repository.isTemplate) MiniBadge("Template")
                    if (!repository.private && !repository.fork && !repository.isTemplate) MiniBadge("Public")
                }
            }
        }
    }
}

@Composable
private fun RepositoryProjectIcon(repository: GitHubRepositoryModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(76.dp),
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
                                MaterialTheme.colorScheme.primary.copy(alpha = .24f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = .2f)
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
private fun RepositoryTypeBadge(repository: GitHubRepositoryModel) {
    val label = when {
        repository.isTemplate -> "Template"
        repository.topics.any { it.equals("android", true) || it.contains("app", true) } -> "Application"
        else -> "Repository"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .36f))
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
private fun MiniBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RepositoryDescriptionCard(repository: GitHubRepositoryModel) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("About this project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                repository.description ?: "This repository does not have a description yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))
            Text(
                "Default branch · ${repository.defaultBranch}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RepositoryDetailsGrid(repository: GitHubRepositoryModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Project details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile("Stars", compactCount(repository.stars), Modifier.weight(1f))
            DetailTile("Forks", compactCount(repository.forks), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile("Open issues", compactCount(repository.openIssues), Modifier.weight(1f))
            DetailTile("Language", repository.language ?: "Not detected", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile("Branch", repository.defaultBranch, Modifier.weight(1f))
            DetailTile("Updated", repository.updatedAt.take(10).ifBlank { "Unknown" }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RepositoryTopics(topics: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.take(12).forEach { topic ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .11f)
                ) {
                    Text(
                        topic,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryReadmeCard(markdown: String) {
    val blocks = remember(markdown) { MarkdownRenderer.render(markdown) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blocks.take(MAX_README_BLOCKS).forEach { block -> ReadmeBlock(block) }
            if (blocks.size > MAX_README_BLOCKS) {
                HorizontalDivider()
                Text(
                    "README preview truncated for performance. Open the developer workspace to inspect the complete file.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ReadmeBlock(block: MarkdownBlock) {
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
            Text(
                block.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MarkdownBlockKind.Code -> Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = .72f)
        ) {
            Text(
                block.text,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
        MarkdownBlockKind.Divider -> HorizontalDivider()
        MarkdownBlockKind.Paragraph -> Text(
            block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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

private const val MAX_README_BLOCKS = 90
