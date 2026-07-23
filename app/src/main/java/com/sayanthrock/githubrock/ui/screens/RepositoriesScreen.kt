package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.R
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RepositoryPlatformFilter
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.core.model.RepositorySourceFilter
import com.sayanthrock.githubrock.core.model.RepositoryTypeFilter
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryGalleryCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RepositorySearchHistoryViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {
    val history = preferences.repositorySearchHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun record(query: String) {
        viewModelScope.launch { preferences.addRepositorySearch(query) }
    }

    fun clear() {
        viewModelScope.launch { preferences.clearRepositorySearchHistory() }
    }
}

/** Searchable native repository library for connected and public GitHub projects. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RepositoriesScreen(
    repositories: List<GitHubRepositoryModel>,
    loading: Boolean,
    onSearch: (RepositorySearchOptions) -> Unit,
    creationEnabled: Boolean,
    onOpen: (GitHubRepositoryModel) -> Unit,
    connectedLogin: String? = null,
    historyViewModel: RepositorySearchHistoryViewModel = hiltViewModel()
) {
    val history by historyViewModel.history.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf<String?>(null) }
    var type by rememberSaveable { mutableStateOf(RepositoryTypeFilter.All) }
    var sort by rememberSaveable { mutableStateOf(RepositorySort.Updated) }
    var source by rememberSaveable { mutableStateOf(RepositorySourceFilter.AllGitHub) }
    var platform by rememberSaveable { mutableStateOf(RepositoryPlatformFilter.All) }
    var sourceOwner by rememberSaveable { mutableStateOf("") }
    var languageMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    var platformMenu by remember { mutableStateOf(false) }
    var showCreateRepository by rememberSaveable { mutableStateOf(false) }

    val effectiveOwner = when (source) {
        RepositorySourceFilter.ConnectedAccount -> connectedLogin
        RepositorySourceFilter.User,
        RepositorySourceFilter.Organization -> sourceOwner
        else -> null
    }
    val options = RepositorySearchOptions(
        query = query,
        language = language,
        type = type,
        sort = sort,
        source = source,
        platform = platform,
        sourceOwner = effectiveOwner
    )
    val languages = remember(repositories) {
        (COMMON_LANGUAGES + repositories.mapNotNull { it.language }).distinct().sorted()
    }
    val visibleRepositories = remember(
        repositories,
        query,
        language,
        type,
        sort,
        source,
        platform,
        effectiveOwner
    ) {
        options.applyLocally(repositories)
    }

    fun submit(searchQuery: String = query) {
        val normalized = searchQuery.trim()
        query = searchQuery
        if (normalized.isNotBlank()) historyViewModel.record(normalized)
        onSearch(
            options.copy(
                query = normalized,
                sourceOwner = when (source) {
                    RepositorySourceFilter.ConnectedAccount -> connectedLogin
                    RepositorySourceFilter.User,
                    RepositorySourceFilter.Organization -> sourceOwner.trim()
                    else -> null
                }
            )
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                StandardScreenHeader(
                    title = "Repositories",
                    subtitle = if (visibleRepositories.isEmpty()) {
                        "Search public GitHub repositories"
                    } else {
                        "${visibleRepositories.size} projects available"
                    }
                )
            }
            if (creationEnabled) {
                OutlinedButton(onClick = { showCreateRepository = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { submit(it) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search repositories, owners, or topics…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    onSearch(options.copy(query = ""))
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

        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Recent searches",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = historyViewModel::clear) { Text("Clear") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it }) { item ->
                    AssistChip(onClick = { submit(item) }, label = { Text(item, maxLines = 1) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilterChip(
                    selected = source != RepositorySourceFilter.AllGitHub,
                    onClick = { sourceMenu = true },
                    label = { Text("Source: ${source.label}") }
                )
                DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                    RepositorySourceFilter.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            enabled = item != RepositorySourceFilter.ConnectedAccount || !connectedLogin.isNullOrBlank(),
                            onClick = {
                                source = item
                                if (item == RepositorySourceFilter.ConnectedAccount) {
                                    sourceOwner = connectedLogin.orEmpty()
                                }
                                sourceMenu = false
                            }
                        )
                    }
                }
            }
            Box {
                FilterChip(
                    selected = platform != RepositoryPlatformFilter.All,
                    onClick = { platformMenu = true },
                    label = { Text("Platform: ${platform.label}") }
                )
                DropdownMenu(expanded = platformMenu, onDismissRequest = { platformMenu = false }) {
                    RepositoryPlatformFilter.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            onClick = { platform = item; platformMenu = false }
                        )
                    }
                }
            }
            Box {
                FilterChip(
                    selected = sort != RepositorySort.Updated,
                    onClick = { sortMenu = true },
                    label = { Text("Sort by: ${sort.label}") }
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    RepositorySort.entries.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { sort = item; sortMenu = false })
                    }
                }
            }
            Box {
                FilterChip(
                    selected = language != null,
                    onClick = { languageMenu = true },
                    label = { Text("Language: ${language ?: "All"}") }
                )
                DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All languages") },
                        onClick = { language = null; languageMenu = false }
                    )
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
        }

        if (source == RepositorySourceFilter.Organization || source == RepositorySourceFilter.User) {
            OutlinedTextField(
                value = sourceOwner,
                onValueChange = { sourceOwner = it.removePrefix("@") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                label = {
                    Text(if (source == RepositorySourceFilter.Organization) "Organization login" else "GitHub username")
                },
                supportingText = { Text("Enter the account to limit public repository results.") }
            )
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
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (query.isBlank()) "No repositories loaded" else "No repositories found",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                if (query.isBlank()) {
                                    "Search all public GitHub projects or choose a source, platform, language, and sort order."
                                } else {
                                    "Try another owner, repository name, topic, or filter combination."
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

    if (showCreateRepository && creationEnabled) {
        CreateRepositorySheet(
            onDismiss = { showCreateRepository = false },
            onCreated = { repository ->
                showCreateRepository = false
                onOpen(repository)
            }
        )
    }
}

private val COMMON_LANGUAGES = listOf(
    "C", "C#", "C++", "Dart", "Go", "HTML", "Java", "JavaScript", "Kotlin", "PHP",
    "Python", "Rust", "Shell", "Swift", "TypeScript"
)
