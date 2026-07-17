package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
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

/** Returns an explicit repository preview URL or GitHub's generated Open Graph image. */
fun GitHubRepositoryModel.repositoryPreviewImageUrl(): String {
    val explicitPreview = previewImageUrl?.trim()?.takeIf { it.startsWith("https://") }
    if (explicitPreview != null) return explicitPreview

    val cacheKey = updatedAt.ifBlank { id.toString() }.hashCode().toUInt()
    return "https://opengraph.githubassets.com/$cacheKey/$fullName"
}

/** Displays a repository preview image with accessible template and privacy labels. */
@Composable
fun RepositoryPreview(
    repository: GitHubRepositoryModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics {
            contentDescription = "${repository.fullName} repository preview image"
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box {
            SubcomposeAsyncImage(
                model = repository.repositoryPreviewImageUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { RepositoryPreviewFallback(repository, showProgress = true) },
                error = { RepositoryPreviewFallback(repository) },
                success = { SubcomposeAsyncImageContent() }
            )

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (repository.isTemplate) {
                    RepositoryVisualBadge("Template repository", accent = true)
                }
                if (repository.private) {
                    RepositoryVisualBadge("Private")
                }
            }
        }
    }
}

/** Displays a GitHub avatar while preserving readable initials during loading or errors. */
@Composable
fun GitHubAvatar(
    imageUrl: String?,
    fallbackText: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge
) {
    Surface(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val normalizedUrl = imageUrl?.trim()?.takeIf(String::isNotBlank)
        if (normalizedUrl == null) {
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
private fun RepositoryPreviewFallback(
    repository: GitHubRepositoryModel,
    showProgress: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .08f)),
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
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = repository.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = repository.owner.login,
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
            text = fallbackText.trim().take(2).uppercase().ifBlank { "GH" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RepositoryVisualBadge(label: String, accent: Boolean = false) {
    val container = if (accent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .94f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = .94f)
    }
    val foreground = if (accent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = foreground,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
