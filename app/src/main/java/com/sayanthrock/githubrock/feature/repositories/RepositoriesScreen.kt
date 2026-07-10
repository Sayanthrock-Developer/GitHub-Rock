package com.sayanthrock.githubrock.feature.repositories

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.RepositorySummary
import com.sayanthrock.githubrock.ui.theme.GlassCard

@Composable
fun RepositoriesScreen(
    demoMode: Boolean,
    onRepository: (RepositorySummary) -> Unit,
    viewModel: RepositoriesViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val repositories = viewModel.repositories.collectAsLazyPagingItems()

    LaunchedEffect(demoMode) { viewModel.setDemoMode(demoMode) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Repositories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            if (demoMode) "Isolated sample repositories" else "Owned, contributed and public repositories",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("Search repositories") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        )

        when (val refresh = repositories.loadState.refresh) {
            is LoadState.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            is LoadState.Error -> ErrorState(refresh.error.message ?: "Unable to load repositories.", repositories::retry)
            is LoadState.NotLoading -> {
                if (repositories.itemCount == 0) {
                    EmptyState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(repositories.itemCount) { index ->
                            repositories[index]?.let { repository ->
                                RepositoryCard(repository) {
                                    viewModel.markOpened(repository)
                                    onRepository(repository)
                                }
                            }
                        }
                        if (repositories.loadState.append is LoadState.Loading) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(repository: RepositorySummary, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (repository.avatarUrl != null) {
                    AsyncImage(
                        model = repository.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                } else {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(5.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(repository.fullName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        repository.description ?: "No description",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (repository.isPrivate) Icon(Icons.Default.Lock, "Private repository")
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("★ ${repository.stars}", style = MaterialTheme.typography.bodySmall)
                Text("⑂ ${repository.forks}", style = MaterialTheme.typography.bodySmall)
                Text("Issues ${repository.openIssues}", style = MaterialTheme.typography.bodySmall)
                repository.language?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            Text(message)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Text(" Retry")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Folder, null)
            Text("No repositories found", fontWeight = FontWeight.SemiBold)
            Text("Try a different search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RepositoryDetailsScreen(owner: String, repository: String) {
    val context = LocalContext.current
    val tabs = listOf("Overview", "Code", "Issues", "Pull Requests", "Actions", "Releases")
    var selected by remember { mutableIntStateOf(0) }

    Column(Modifier.padding(16.dp)) {
        Text("$owner/$repository", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = selected == index, onClick = { selected = index }, text = { Text(tab) })
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(tabs[selected], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (selected == 0) {
                        "Repository overview is available now. Live file editing, issue management, pull request reviews, Actions dispatch and release writes are staged for later milestones and are not represented as complete."
                    } else {
                        "This section is part of the verified next milestone. Open the repository on GitHub for full access today."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = { openUrl(context, "https://github.com/$owner/$repository") }) {
                    Icon(Icons.Default.OpenInBrowser, null)
                    Text(" Open on GitHub")
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
}
