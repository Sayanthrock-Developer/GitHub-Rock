package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.core.model.RepositoryTypeFilter
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryGalleryCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader

/**
 * Displays a searchable visual library of repositories with loading and empty states.
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
    onSearch: (RepositorySearchOptions) -> Unit,
    onOpen: (GitHubRepositoryModel) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf<String?>(null) }
    var type by rememberSaveable { mutableStateOf(RepositoryTypeFilter.All) }
    var sort by rememberSaveable { mutableStateOf(RepositorySort.Updated) }
    var languageMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var showCreateRepository by rememberSaveable { mutableStateOf(false) }
    val languages = remember(repositories) { repositories.mapNotNull { it.language }.distinct().sorted() }
    val options = RepositorySearchOptions(query, language, type, sort)
    val visibleRepositories = remember(repositories, language, type, sort) {
        options.applyLocally(repositories)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                StandardScreenHeader(
                    title = "Repositories",
                    subtitle = if (visibleRepositories.isEmpty()) {
                        "Search the GitHub ecosystem"
                    } else {
                        "${visibleRepositories.size} visual projects available"
                    }
                )
            }
            OutlinedButton(onClick = { showCreateRepository = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New")
            }
        }
        Spacer(Modifier.height(16.dp))
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { onSearch(RepositorySearchOptions(query.trim(), language, type, sort)) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Find a repository…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    onSearch(RepositorySearchOptions(language = language, type = type, sort = sort))
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

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilterChip(
                    selected = language != null,
                    onClick = { languageMenu = true },
                    label = { Text("Language: ${language ?: "All"}") }
                )
                DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                    DropdownMenuItem(text = { Text("All languages") }, onClick = { language = null; languageMenu = false })
                    languages.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { language = item; languageMenu = false })
                    }
                }
            }
            Box {
                FilterChip(
                    selected = type != RepositoryTypeFilter.All,
                    onClick = { typeMenu = true },
                    label = { Text("Type: ${type.label}") }
                )
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    RepositoryTypeFilter.entries.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { type = item; typeMenu = false })
                    }
                }
            }
            Box {
                FilterChip(
                    selected = sort != RepositorySort.Updated,
                    onClick = { sortMenu = true },
                    label = { Text("Sort: ${sort.label}") }
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    RepositorySort.entries.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { sort = item; sortMenu = false })
                    }
                }
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!loading && visibleRepositories.isEmpty()) {
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

            items(visibleRepositories, key = { it.id }) { repository ->
                RepositoryGalleryCard(repository) { onOpen(repository) }
            }
        }
    }

    if (showCreateRepository) {
        CreateRepositorySheet(
            onDismiss = { showCreateRepository = false },
            onCreated = { repository ->
                showCreateRepository = false
                onOpen(repository)
            }
        )
    }
}
