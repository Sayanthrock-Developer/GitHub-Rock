package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RepositoryPlatformFilter
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.core.model.RepositorySourceFilter
import com.sayanthrock.githubrock.core.model.RepositoryTypeFilter
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.ui.components.GitHubAvatar
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
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

private enum class RepositoryChartMode(
    val label: String,
    val apiSort: RepositorySort
) {
    Trending("Trending", RepositorySort.Updated),
    Releases("Releases", RepositorySort.Updated),
    Popular("Popular", RepositorySort.Stars)
}

/** Searchable native repository library presented as a compact top-charts experience. */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var source by rememberSaveable { mutableStateOf(RepositorySourceFilter.AllGitHub) }
    var sourceOwner by rememberSaveable { mutableStateOf("") }
    var selectedModeName by rememberSaveable { mutableStateOf(RepositoryChartMode.Trending.name) }
    var selectedPlatformName by rememberSaveable { mutableStateOf(HomePlatform.All.name) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showCreateRepository by rememberSaveable { mutableStateOf(false) }

    val selectedMode = RepositoryChartMode.entries.firstOrNull { it.name == selectedModeName }
        ?: RepositoryChartMode.Trending
    val selectedPlatform = HomePlatform.entries.firstOrNull { it.name == selectedPlatformName }
        ?: HomePlatform.All
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
        sort = selectedMode.apiSort,
        source = source,
        platform = RepositoryPlatformFilter.All,
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
        source,
        effectiveOwner,
        selectedPlatform,
        selectedMode
    ) {
        val filtered = options.applyLocally(repositories)
            .asSequence()
            .filter { it.matchesRepositoryQuery(query) }
            .filter { repositoryMatchesPlatform(it, selectedPlatform) }

        when (selectedMode) {
            RepositoryChartMode.Trending -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.updatedAt }
                    .thenByDescending { it.stars }
            ).toList()
            RepositoryChartMode.Releases -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.updatedAt }
                    .thenByDescending { it.forks }
            ).toList()
            RepositoryChartMode.Popular -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.stars }
                    .thenByDescending { it.forks }
            ).toList()
        }
    }
    val activeFilterCount = listOf(
        query.isNotBlank(),
        language != null,
        type != RepositoryTypeFilter.All,
        source != RepositorySourceFilter.AllGitHub,
        selectedPlatform != HomePlatform.All
    ).count { it }

    fun submit() {
        val normalized = query.trim()
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

    fun resetFilters() {
        query = ""
        language = null
        type = RepositoryTypeFilter.All
        source = RepositorySourceFilter.AllGitHub
        sourceOwner = ""
        selectedPlatformName = HomePlatform.All.name
        onSearch(RepositorySearchOptions(sort = selectedMode.apiSort))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RepositoryChartsHeader(
                repositoryCount = visibleRepositories.size,
                creationEnabled = creationEnabled,
                activeFilterCount = activeFilterCount,
                onCreate = { showCreateRepository = true },
                onOpenFilters = { showFilters = true }
            )
        }

        item {
            RepositoryChartTabs(
                selected = selectedMode,
                onSelect = { mode ->
                    selectedModeName = mode.name
                    onSearch(options.copy(sort = mode.apiSort))
                }
            )
        }

        if (activeFilterCount > 0) {
            item {
                RepositoryActiveFilters(
                    query = query,
                    selectedPlatform = selectedPlatform,
                    language = language,
                    type = type,
                    source = source,
                    onOpenFilters = { showFilters = true },
                    onReset = ::resetFilters
                )
            }
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        if (!loading && visibleRepositories.isEmpty()) {
            item {
                RepositoryChartsEmptyState(
                    hasFilters = activeFilterCount > 0,
                    onReset = ::resetFilters,
                    onRefresh = ::submit
                )
            }
        }

        items(visibleRepositories, key = { it.id }) { repository ->
            val index = visibleRepositories.indexOf(repository)
            val badge = when (selectedMode) {
                RepositoryChartMode.Trending -> null
                RepositoryChartMode.Releases -> "New"
                RepositoryChartMode.Popular -> "No. ${index + 1}"
            }
            RepositoryChartCard(
                repository = repository,
                badge = badge,
                onClick = { onOpen(repository) }
            )
        }
    }

    if (showFilters) {
        RepositoryFiltersSheet(
            query = query,
            onQueryChange = { query = it },
            history = history,
            onUseHistory = { query = it },
            onClearHistory = historyViewModel::clear,
            selectedPlatform = selectedPlatform,
            onPlatformChange = { selectedPlatformName = it.name },
            source = source,
            onSourceChange = { selected ->
                source = selected
                if (selected == RepositorySourceFilter.ConnectedAccount) {
                    sourceOwner = connectedLogin.orEmpty()
                }
            },
            connectedLogin = connectedLogin,
            sourceOwner = sourceOwner,
            onSourceOwnerChange = { sourceOwner = it.removePrefix("@") },
            type = type,
            onTypeChange = { type = it },
            language = language,
            languages = languages,
            onLanguageChange = { language = it },
            onReset = ::resetFilters,
            onApply = {
                submit()
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
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

@Composable
private fun RepositoryChartsHeader(
    repositoryCount: Int,
    creationEnabled: Boolean,
    activeFilterCount: Int,
    onCreate: () -> Unit,
    onOpenFilters: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Repositories",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$repositoryCount project${if (repositoryCount == 1) "" else "s"} available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (creationEnabled) {
            Surface(
                onClick = onCreate,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Create repository")
                }
            }
        }

        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            }
        ) {
            Surface(
                onClick = onOpenFilters,
                modifier = Modifier
                    .size(52.dp)
                    .semantics { contentDescription = "Filter repositories" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
        }
    }
}

@Composable
private fun RepositoryChartTabs(
    selected: RepositoryChartMode,
    onSelect: (RepositoryChartMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RepositoryChartMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Text(
                    text = mode.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 13.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RepositoryActiveFilters(
    query: String,
    selectedPlatform: HomePlatform,
    language: String?,
    type: RepositoryTypeFilter,
    source: RepositorySourceFilter,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (query.isNotBlank()) AssistChip(onClick = onOpenFilters, label = { Text("Search: $query") })
        if (selectedPlatform != HomePlatform.All) {
            AssistChip(
                onClick = onOpenFilters,
                leadingIcon = {
                    Icon(selectedPlatform.icon, contentDescription = null, modifier = Modifier.size(17.dp))
                },
                label = { Text(selectedPlatform.label) }
            )
        }
        language?.let { AssistChip(onClick = onOpenFilters, label = { Text(it) }) }
        if (type != RepositoryTypeFilter.All) {
            AssistChip(onClick = onOpenFilters, label = { Text(type.label) })
        }
        if (source != RepositorySourceFilter.AllGitHub) {
            AssistChip(onClick = onOpenFilters, label = { Text(source.label) })
        }
        TextButton(onClick = onReset) { Text("Clear") }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RepositoryChartCard(
    repository: GitHubRepositoryModel,
    badge: String?,
    onClick: () -> Unit
) {
    val platforms = remember(repository) { repositoryPlatforms(repository) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GitHubAvatar(
                    imageUrl = repository.owner.avatarUrl,
                    fallbackText = repository.name,
                    contentDescription = "${repository.owner.login} repository owner",
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(18.dp)
                )

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = repository.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("@")
                            append(repository.owner.login)
                            repository.language?.takeIf(String::isNotBlank)?.let {
                                append("  •  ")
                                append(it)
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                badge?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = repository.description ?: "No repository description provided.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (platforms.isEmpty()) {
                    RepositoryChartPlatformChip(Icons.Default.Devices, "Cross-platform")
                } else {
                    platforms.forEach { platform ->
                        RepositoryChartPlatformChip(platform.icon, platform.label)
                    }
                }
                RepositoryChartPlatformChip(
                    icon = if (repository.private) Icons.Default.Lock else Icons.Default.Check,
                    label = if (repository.private) "Private" else "Public"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RepositoryChartMetric(Icons.Default.Star, compactRepositoryCount(repository.stars))
                RepositoryChartMetric(Icons.Default.CallSplit, compactRepositoryCount(repository.forks))
                RepositoryChartMetric(Icons.Default.Schedule, relativeRepositoryTime(repository.updatedAt))
                Spacer(Modifier.weight(1f))
                Text("Open", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open ${repository.name}",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RepositoryChartPlatformChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RepositoryChartMetric(icon: ImageVector, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun RepositoryChartsEmptyState(
    hasFilters: Boolean,
    onReset: () -> Unit,
    onRefresh: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (hasFilters) Icons.Default.Search else Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasFilters) "No repositories match" else "No repositories loaded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasFilters) {
                    "Try another OS, source, language, type, or search term."
                } else {
                    "Refresh or search GitHub to load repository results."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = if (hasFilters) onReset else onRefresh) {
                Text(if (hasFilters) "Reset filters" else "Refresh")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun RepositoryFiltersSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<String>,
    onUseHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    selectedPlatform: HomePlatform,
    onPlatformChange: (HomePlatform) -> Unit,
    source: RepositorySourceFilter,
    onSourceChange: (RepositorySourceFilter) -> Unit,
    connectedLogin: String?,
    sourceOwner: String,
    onSourceOwnerChange: (String) -> Unit,
    type: RepositoryTypeFilter,
    onTypeChange: (RepositoryTypeFilter) -> Unit,
    language: String?,
    languages: List<String>,
    onLanguageChange: (String?) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var languageMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Filter repositories",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Search GitHub and choose the projects you want to see.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close filters")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                label = { Text("Repository, owner, or topic") },
                shape = RoundedCornerShape(18.dp)
            )

            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Recent searches", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearHistory) { Text("Clear") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history, key = { it }) { item ->
                        AssistChip(onClick = { onUseHistory(item) }, label = { Text(item, maxLines = 1) })
                    }
                }
            }

            Text(
                text = "Choose your OS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            HomePlatform.entries.forEach { platform ->
                val isSelected = platform == selectedPlatform
                Surface(
                    onClick = { onPlatformChange(platform) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(platform.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text(
                            text = platform.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                }
            }

            Text("Source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepositorySourceFilter.entries.forEach { item ->
                    FilterChip(
                        selected = item == source,
                        enabled = item != RepositorySourceFilter.ConnectedAccount || !connectedLogin.isNullOrBlank(),
                        onClick = { onSourceChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }

            if (source == RepositorySourceFilter.Organization || source == RepositorySourceFilter.User) {
                OutlinedTextField(
                    value = sourceOwner,
                    onValueChange = onSourceOwnerChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(if (source == RepositorySourceFilter.Organization) "Organization login" else "GitHub username")
                    },
                    supportingText = { Text("Limit results to this public account.") }
                )
            }

            Text("Repository type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepositoryTypeFilter.entries.forEach { item ->
                    FilterChip(
                        selected = item == type,
                        onClick = { onTypeChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }

            Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { languageMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = language ?: "All languages",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose language")
                }
                DropdownMenu(
                    expanded = languageMenu,
                    onDismissRequest = { languageMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All languages") },
                        onClick = {
                            onLanguageChange(null)
                            languageMenu = false
                        }
                    )
                    languages.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onLanguageChange(item)
                                languageMenu = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

private fun GitHubRepositoryModel.matchesRepositoryQuery(query: String): Boolean {
    val normalized = query.trim().lowercase(Locale.US)
    if (normalized.isBlank()) return true
    return listOfNotNull(
        name,
        fullName,
        owner.login,
        description,
        language,
        topics.joinToString(" ")
    ).any { it.lowercase(Locale.US).contains(normalized) }
}

private fun repositoryMatchesPlatform(
    repository: GitHubRepositoryModel,
    platform: HomePlatform
): Boolean {
    if (platform == HomePlatform.All) return true
    val supported = repositoryPlatforms(repository)
    return supported.isEmpty() || platform in supported
}

private fun compactRepositoryCount(value: Int): String = when {
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

private val COMMON_LANGUAGES = listOf(
    "C", "C#", "C++", "Dart", "Go", "HTML", "Java", "JavaScript", "Kotlin", "PHP",
    "Python", "Rust", "Shell", "Swift", "TypeScript"
)
