package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.theme.LocalRemoteImagesEnabled

/**
 * Branded repository artwork with a privacy-aware image fallback.
 *
 * Full-size heroes intentionally separate the large preview from the project icon and owner identity.
 * The lower identity rail reserves space for the project icon rendered by the parent hero, preventing
 * the previous banner/avatar/icon overlap.
 */
@Composable
fun RepositoryArtwork(
    repository: GitHubRepositoryModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val showImages = LocalRemoteImagesEnabled.current
    val previewHeight = if (compact) 92.dp else 124.dp
    val identityHeight = if (compact) 40.dp else 52.dp
    val artworkHeight = previewHeight + identityHeight
    val previewDescription = "${repository.fullName} repository preview image"
    val ownerFallbackPainter = if (showImages) {
        repository.owner.avatarUrl.takeIf(String::isNotBlank)?.let { rememberAsyncImagePainter(it) }
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(artworkHeight)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = .72f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = .46f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
        ) {
            when {
                showImages && !repository.previewImageUrl.isNullOrBlank() -> AsyncImage(
                    model = repository.previewImageUrl,
                    contentDescription = previewDescription,
                    placeholder = ownerFallbackPainter,
                    error = ownerFallbackPainter,
                    fallback = ownerFallbackPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                showImages && repository.owner.avatarUrl.isNotBlank() -> AsyncImage(
                    model = repository.owner.avatarUrl,
                    contentDescription = previewDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(.22f)
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = .03f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = .46f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (repository.isTemplate) RepositoryArtworkBadge("Template")
                if (repository.fork) RepositoryArtworkBadge("Fork")
                if (repository.private) RepositoryArtworkBadge("Private")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(identityHeight),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .96f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (compact) 14.dp else 108.dp,
                        end = 14.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(if (compact) 26.dp else 30.dp),
                    shape = RoundedCornerShape(if (compact) 9.dp else 10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (compact) 15.dp else 17.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "@${repository.owner.login}",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = if (compact) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!compact) {
                        Text(
                            repository.language ?: "GitHub repository",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryArtworkBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .58f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
