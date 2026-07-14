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
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel

/**
 * Branded repository artwork that uses GitHub's public social-preview image when available.
 * Private repositories fall back to a subdued owner avatar so no private preview is requested.
 */
@Composable
fun RepositoryArtwork(
    repository: GitHubRepositoryModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val artworkHeight = if (compact) 132.dp else 176.dp

    Box(
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
        if (!repository.private) {
            AsyncImage(
                model = repository.socialPreviewUrl(),
                contentDescription = "${repository.fullName} repository preview image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (repository.owner.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = repository.owner.avatarUrl,
                contentDescription = null,
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
                            MaterialTheme.colorScheme.background.copy(alpha = .04f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = .92f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                repository.isTemplate -> RepositoryArtworkBadge("Template")
                repository.fork -> RepositoryArtworkBadge("Fork")
            }
            if (repository.private) RepositoryArtworkBadge("Private")
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
            ) {
                if (repository.owner.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = repository.owner.avatarUrl,
                        contentDescription = "${repository.owner.login} avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(if (compact) 42.dp else 48.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(if (compact) 42.dp else 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "@${repository.owner.login}",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    repository.language ?: "GitHub repository",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .76f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RepositoryArtworkBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
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

private fun GitHubRepositoryModel.socialPreviewUrl(): String {
    val cacheKey = "${id}-${updatedAt.hashCode()}"
    return "https://opengraph.githubassets.com/github-rock-$cacheKey/$fullName"
}
