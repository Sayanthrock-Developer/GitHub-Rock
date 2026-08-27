package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember(state.query) { mutableStateOf(state.query) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = StandardScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Explore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Discover what the GitHub community is building.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        searchJob?.cancel()
                        searchJob = scope.launch {
                            delay(350)
                            viewModel.search(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search repositories, topics, or languages") },
                    label = { Text("Search GitHub") },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExploreViewModel.ExploreMode.entries.forEach { mode ->
                        FilterChip(selected = state.mode == mode, onClick = { viewModel.load(mode, query) }, label = { Text(mode.title) })
                    }
                }
            }
            item { Text(state.mode.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (state.loading) {
                item {
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text("Finding repositories…", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            state.error?.let { message ->
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ErrorOutline, null)
                            Text(message)
                            TextButton(onClick = { viewModel.clearError(); viewModel.load() }) { Text("Retry") }
                        }
                    }
                }
            }
            if (!state.loading && state.repositories.isEmpty() && state.error == null) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nothing found", fontWeight = FontWeight.Bold)
                            Text("Try a different search or discovery category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(state.repositories.take(50), key = { it.id }) { repository ->
                ExploreRepositoryCard(repository, onOpenRepo, onOpenProfile)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ExploreRepositoryCard(
    repository: GitHubRepositoryModel,
    onOpenRepo: (GitHubRepositoryModel) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenRepo(repository) }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(repository.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${repository.owner.login}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Default.ArrowForward, contentDescription = "Open repository")
            }
            Text(repository.description ?: "No repository description provided.", maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(17.dp))
                    Text(formatCompact(repository.stars))
                }
                Text("Forks ${formatCompact(repository.forks)}")
                repository.language?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
            if (repository.topics.isNotEmpty()) {
                Text(repository.topics.take(5).joinToString("  •  "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { onOpenProfile(repository.owner.login) }) { Text("View developer") }
        }
    }
}

private fun formatCompact(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fk".format(value / 1_000.0)
    else -> value.toString()
}
