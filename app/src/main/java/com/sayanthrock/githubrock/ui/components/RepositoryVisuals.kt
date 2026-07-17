package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled

/** Returns an explicit repository preview URL or GitHub's generated Open Graph image. */
fun GitHubRepositoryModel.repositoryPreviewImageUrl(): String {
    val explicitPreview = previewImageUrl?.trim()?.takeIf { it.startsWith("https://") }
    if (explicitPreview != null) return explicitPreview
    val cacheKey = updatedAt.ifBlank { id.toString() }.hashCode().toUInt()
    return "https://opengraph.githubassets.com/$cacheKey/$fullName"
}

/** Premium repository card with preview image, owner logo, template state, and metrics. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RepositoryGalleryCard(
    repository: GitHubRepositoryModel,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RepositoryPreview(repository, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GitHubAvatar(
                    imageUrl = repository.owner.avatarUrl,
                    fallbackText = repository.owner.login,
                    contentDescription = "${repository.owner.login} profile logo",
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.large
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        repository.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        repository.owner.login,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open ${repository.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                repository.description ?: "No repository description.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepositoryMetaPill(Icons.Default.Star, "Stars", compactCount(repository.stars))
                RepositoryMetaPill(Icons.Default.CallSplit, "Forks", compactCount(repository.forks))
                RepositoryMetaPill(Icons.Default.ErrorOutline, "Open issues", compactCount(repository.openIssues))
                RepositoryMetaPill(Icons.Default.Code, "Language", repository.language ?: "Repository", accent = true)
                repository.topics.firstOrNull()?.takeIf(String::isNotBlank)?.let { topic ->
                    RepositoryMetaPill(Icons.Default.Tag, "Topic", topic)
                }
            }
        }
    }
}

/** Displays a repository preview image or a readable local fallback when images are disabled. */
@Composable
fun RepositoryPreview(repository: GitHubRepositoryModel, modifier: Modifier = Modifier) {
    val showImages = LocalRemoteImagesEnabled.current
    Surface(
        modifier = modifier.semantics { contentDescription = "${repository.fullName} repository preview image" },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box {
            if (showImages) {
                SubcomposeAsyncImage(
                    model = repository.repositoryPreviewImageUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { RepositoryPreviewFallback(repository, showProgress = true) },
                    error = { RepositoryPreviewFallback(repository) },
                    success = { SubcomposeAsyncImageContent() }
                )
            } else {
                RepositoryPreviewFallback(repository)
            }

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (repository.isTemplate) RepositoryVisualBadge("Template repository", accent = true)
                if (repository.private) RepositoryVisualBadge("Private")
            }
        }
    }
}

/** Displays a GitHub avatar while preserving readable initials when images are off. */
@Composable
fun GitHubAvatar(
    imageUrl: String?,
    fallbackText: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge
) {
    val showImages = LocalRemoteImagesEnabled.current
    Surface(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val normalizedUrl = imageUrl?.trim()?.takeIf(String::isNotBlank)
        if (!showImages || normalizedUrl == null) {
            AvatarFallback(fallbackText)
        } else {
            SubcomposeAsyncImage(
                model = normalizedUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { AvatarFallback(fallbackText) },
                error = { AvatarFallback(fallbackText) },
                success = { SubcomposeAsyncImageContent() }
            )
        }
    }
}

@Composable
private fun RepositoryPreviewFallback(repository: GitHubRepositoryModel, showProgress: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = .08f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showProgress) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                repository.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                repository.owner.login,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvatarFallback(fallbackText: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            fallbackText.trim().take(2).uppercase().ifBlank { "GH" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RepositoryVisualBadge(label: String, accent: Boolean = false) {
    val container = if (accent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .94f)
    else MaterialTheme.colorScheme.surface.copy(alpha = .94f)
    val foreground = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RepositoryMetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Boolean = false
) {
    val foreground = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.semantics { contentDescription = "$label: $value" },
        shape = MaterialTheme.shapes.large,
        color = if (accent) MaterialTheme.colorScheme.primary.copy(alpha = .10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(15.dp))
            Text(
                value,
                color = foreground,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
