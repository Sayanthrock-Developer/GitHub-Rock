package com.sayanthrock.githubrock.ui.screens

import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.data.local.DownloadEntity
import java.io.File

/** Adds application artwork above the existing full download manager. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val applicationDownloads = remember(downloads) {
        downloads.filter { it.fileName.endsWith(".apk", ignoreCase = true) }.take(8)
    }

    Column(Modifier.fillMaxSize()) {
        if (applicationDownloads.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Applications",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(applicationDownloads, key = { it.id }) { item ->
                        DownloadApplicationArtwork(item)
                    }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            DownloadsScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun DownloadApplicationArtwork(item: DownloadEntity) {
    val context = LocalContext.current
    val archiveIcon = remember(item.localPath, item.status) {
        item.localPath
            ?.let(::File)
            ?.takeIf { it.exists() && it.extension.equals("apk", ignoreCase = true) }
            ?.let { apk ->
                runCatching {
                    val packageManager = context.packageManager
                    val packageInfo = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                    packageInfo?.applicationInfo?.let { applicationInfo ->
                        applicationInfo.sourceDir = apk.absolutePath
                        applicationInfo.publicSourceDir = apk.absolutePath
                        applicationInfo.loadIcon(packageManager)
                    }
                }.getOrNull()
            }
    }
    val artwork = archiveIcon ?: remember(item.sourceUrl) { githubOwnerArtwork(item.sourceUrl) }

    Surface(
        modifier = Modifier.size(width = 180.dp, height = 82.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = "${item.fileName} application icon",
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.fileName.removeSuffix(".apk"),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

private fun githubOwnerArtwork(sourceUrl: String): String? {
    val uri = runCatching { Uri.parse(sourceUrl) }.getOrNull() ?: return null
    val segments = uri.pathSegments.orEmpty()
    val owner = when {
        uri.host.equals("github.com", ignoreCase = true) -> segments.firstOrNull()
        uri.host.equals("api.github.com", ignoreCase = true) -> {
            val reposIndex = segments.indexOf("repos")
            segments.getOrNull(reposIndex + 1)
        }
        else -> null
    }?.takeIf { it.isNotBlank() && it != "repos" }
    return owner?.let { "https://github.com/$it.png?size=128" }
}
