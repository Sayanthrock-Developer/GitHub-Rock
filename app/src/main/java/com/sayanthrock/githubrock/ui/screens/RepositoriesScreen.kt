package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard

/**
 * Displays a searchable list of repositories with loading and empty states.
 *
 * @param repositories The repositories to display.
 * @param loading Whether repository data is currently loading.
 * @param onSearch Called with the submitted or cleared search query.
 * @param onOpen Called when a repository is selected.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RepositoriesScreen(
    repositories: List<GitHubRepositoryModel>,
    loading: Boolean,
    onSearch: (String) -> Unit,
    onOpen: (GitHubRepositoryModel) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Repositories", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(14.dp))
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { onSearch(query.trim()) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search repositories") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    onSearch("")
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_repository_search)
                                )
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth()
        ) {}

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!loading && repositories.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (query.isBlank()) "No repositories loaded" else "No repositories found",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                if (query.isBlank()) {
                                    "Search for a public repository or connect GitHub to load your workspace."
                                } else {
                                    "Try another owner, repository name, or keyword."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(repositories, key = { it.id }) { repository ->
                RepositoryCard(repository) { onOpen(repository) }
            }
        }
    }
}
