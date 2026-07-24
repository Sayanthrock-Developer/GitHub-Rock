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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val defaultSort: RepositoryResultSort
) {
    Trending("Trending", RepositoryResultSort.BestMatch),
    Releases("Releases", RepositoryResultSort.RecentlyReleased),
    Popular("Popular", RepositoryResultSort.MostStars)
}

private enum class RepositoryResultSort(
    val label: String,
    val apiSort: RepositorySort
) {
    MostStars("Most Stars", RepositorySort.Stars),
    MostForks("Most Forks", RepositorySort.Forks),
    BestMatch("Best Match", RepositorySort.Updated),
    RecentlyUpdated("Recently Updated", RepositorySort.Updated),
    RecentlyReleased("Recently Released", RepositorySort.Updated)
}

private enum class RepositoryFilterPanel {
    Main,
    Language,
    Sort
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
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf<String?>(null) }
    var type by rememberSaveable { mutableStateOf(RepositoryTypeFilter.All) }
    var source by rememberSaveable { mutableStateOf(RepositorySourceFilter.AllGitHub) }
    var sourceOwner by rememberSaveable { mutableStateOf("") }
    var selectedModeName by rememberSaveable { mutableStateOf(RepositoryChartMode.Trending.name) }
    var selectedPlatformName by rememberSaveable { mutableStateOf(HomePlatform.All.name) }
    var selectedSortName by rememberSaveable { mutableStateOf(RepositoryResultSort.BestMatch.name) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showCreateRepository by rememberSaveable { mutableStateOf(false) }

    val selectedMode = RepositoryChartMode.entries.firstOrNull { it.name == selectedModeName }
        ?: RepositoryChartMode.Trending
    val selectedPlatform = HomePlatform.entries.firstOrNull { it.name == selectedPlatformName }
        ?: HomePlatform.All
    val selectedSort = RepositoryResultSort.entries.firstOrNull { it.name == selectedSortName }
        ?: RepositoryResultSort.BestMatch

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
        sort = selectedSort.apiSort,
        source = source,
        platform = RepositoryPlatformFilter.All,
        sourceOwner = effectiveOwner
    )

    val languages = remember(repositories) {
        (COMMON_LANGUAGES + repositories.mapNotNull { it.language })
            .distinct()
            .sorted()
    }

    val visibleRepositories = remember(
        repositories,
        query,
        language,
        type,
        source,
        effectiveOwner,
        selectedPlatform,
        selectedSort,
        sortAscending
    ) {
        val filtered = options.applyLocally(repositories)
            .asSequence()
            .filter { it.matchesRepositoryQuery(query) }
            .filter { repositoryMatchesPlatform(it, selectedPlatform) }
            .toList()

        val descending = when (selectedSort) {
            RepositoryResultSort.MostStars -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.stars }
                    .thenByDescending { it.updatedAt }
            )
            RepositoryResultSort.MostForks -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.forks }
                    .thenByDescending { it.stars }
            )
            RepositoryResultSort.BestMatch -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.updatedAt }
                    .thenByDescending { it.stars }
                    .thenByDescending { it.forks }
            )
            RepositoryResultSort.RecentlyUpdated,
            RepositoryResultSort.RecentlyReleased -> filtered.sortedWith(
                compareByDescending<GitHubRepositoryModel> { it.updatedAt }
                    .thenByDescending { it.stars }
            )
        }

        if (sortAscending) descending.reversed() else descending
    }

    val activeFilterCount = listOf(
        query.isNotBlank(),
        language != null,
        type != RepositoryTypeFilter.All,
        source != RepositorySourceFilter.AllGitHub,
        selectedPlatform != HomePlatform.All,
        selectedSort != selectedMode.defaultSort,
        sortAscending
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
        selectedSortName = selectedMode.defaultSort.name
        sortAscending = false
        onSearch(RepositorySearchOptions(sort = selectedMode.defaultSort.apiSort))
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
                onSearch = { searchFocusRequester.requestFocus() },
                onOpenFilters = { showFilters = true }
            )
        }

        item {
            RepositoryChartTabs(
                selected = selectedMode,
                onSelect = { mode ->
                    selectedModeName = mode.name
                    selectedSortName = mode.defaultSort.name
                    sortAscending = false
                    onSearch(options.copy(sort = mode.defaultSort.apiSort))
                }
            )
        }

        item {
            RepositorySearchField(
                query = query,
                onQueryChange = { query = it },
                onSubmit = {
                    submit()
                    focusManager.clearFocus()
                },
                onClear = { query = "" },
                focusRequester = searchFocusRequester
            )
        }

        if (history.isNotEmpty() && query.isBlank()) {
            item {
                RepositorySearchHistory(
                    history = history,
                    onUse = {
                        query = it
                        submit()
                    },
                    onClear = historyViewModel::clear
                )
            }
        }

        if (activeFilterCount > 0) {
            item {
                RepositoryActiveFilters(
                    query = query,
                    selectedPlatform = selectedPlatform,
                    language = language,
                    type = type,
                    source = source,
                    sort = selectedSort,
                    ascending = sortAscending,
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
            sort = selectedSort,
            onSortChange = { selectedSortName = it.name },
            ascending = sortAscending,
            onAscendingChange = { sortAscending = it },
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
    onSearch: () -> Unit,
    onOpenFilters: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        RepositoryRoundAction(
            icon = Icons.Default.Search,
            contentDescription = "Search repositories",
            onClick = onSearch
        )

        if (creationEnabled) {
            RepositoryRoundAction(
                icon = Icons.Default.Add,
                contentDescription = "Create repository",
                onClick = onCreate
            )
        }

        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            }
        ) {
            RepositoryRoundAction(
                icon = Icons.Default.Tune,
                contentDescription = "Filter repositories",
                primary = true,
                onClick = onOpenFilters
            )
        }
    }
}

@Composable
private fun RepositoryRoundAction(
    icon: ImageVector,
    contentDescription: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (primary) 6.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
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
private fun RepositorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear repository search")
                }
            }
        },
        placeholder = { Text("Search repositories, owners, or topics") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun RepositorySearchHistory(
    history: List<String>,
    onUse: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Recent searches",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear) { Text("Clear") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it }) { item ->
                AssistChip(onClick = { onUse(item) }, label = { Text(item, maxLines = 1) })
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
    sort: RepositoryResultSort,
    ascending: Boolean,
    onOpenFilters: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
        if (sort != RepositoryResultSort.BestMatch || ascending) {
            AssistChip(
                onClick = onOpenFilters,
                label = { Text("${sort.label} · ${if (ascending) "Ascending" else "Descending"}") }
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
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
                    "Try another OS, source, language, type, sort order, or search term."
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
    sort: RepositoryResultSort,
    onSortChange: (RepositoryResultSort) -> Unit,
    ascending: Boolean,
    onAscendingChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var panelName by rememberSaveable { mutableStateOf(RepositoryFilterPanel.Main.name) }
    val panel = RepositoryFilterPanel.entries.firstOrNull { it.name == panelName }
        ?: RepositoryFilterPanel.Main

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        when (panel) {
            RepositoryFilterPanel.Main -> RepositoryFilterMainPanel(
                selectedPlatform = selectedPlatform,
                onPlatformChange = onPlatformChange,
                source = source,
                onSourceChange = onSourceChange,
                connectedLogin = connectedLogin,
                sourceOwner = sourceOwner,
                onSourceOwnerChange = onSourceOwnerChange,
                type = type,
                onTypeChange = onTypeChange,
                language = language,
                sort = sort,
                ascending = ascending,
                onOpenLanguage = { panelName = RepositoryFilterPanel.Language.name },
                onOpenSort = { panelName = RepositoryFilterPanel.Sort.name },
                onReset = onReset,
                onApply = onApply
            )
            RepositoryFilterPanel.Language -> RepositoryLanguagePanel(
                selected = language,
                languages = languages,
                onSelect = {
                    onLanguageChange(it)
                    panelName = RepositoryFilterPanel.Main.name
                },
                onBack = { panelName = RepositoryFilterPanel.Main.name }
            )
            RepositoryFilterPanel.Sort -> RepositorySortPanel(
                selected = sort,
                ascending = ascending,
                onSelect = onSortChange,
                onAscendingChange = onAscendingChange,
                onBack = { panelName = RepositoryFilterPanel.Main.name }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RepositoryFilterMainPanel(
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
    sort: RepositoryResultSort,
    ascending: Boolean,
    onOpenLanguage: () -> Unit,
    onOpenSort: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter results",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            TextButton(onClick = onReset) {
                Text("Reset all", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        RepositoryFilterSectionTitle("Source")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepositorySourceFilter.entries.forEach { item ->
                FilterChip(
                    selected = item == source,
                    enabled = item != RepositorySourceFilter.ConnectedAccount || !connectedLogin.isNullOrBlank(),
                    onClick = { onSourceChange(item) },
                    label = { Text(item.label) },
                    leadingIcon = if (item == source) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    }
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
                supportingText = { Text("Limit results to this public account.") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        RepositoryFilterSectionTitle("Platform")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomePlatform.entries.forEach { platform ->
                FilterChip(
                    selected = platform == selectedPlatform,
                    onClick = { onPlatformChange(platform) },
                    label = { Text(if (platform == HomePlatform.All) "All" else platform.label) },
                    leadingIcon = if (platform == selectedPlatform) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        { Icon(platform.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    }
                )
            }
        }

        RepositoryFilterSectionTitle("Repository type")
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

        RepositoryFilterSectionTitle("Language")
        RepositoryFilterNavigationRow(
            icon = Icons.Default.Code,
            label = language ?: "All Languages",
            contentDescription = "Choose repository language",
            onClick = onOpenLanguage
        )

        RepositoryFilterSectionTitle("Sort by")
        RepositoryFilterNavigationRow(
            icon = Icons.Default.Tune,
            label = "${sort.label} · ${if (ascending) "Ascending" else "Descending"}",
            contentDescription = "Choose repository sort order",
            onClick = onOpenSort
        )

        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RepositoryFilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RepositoryFilterNavigationRow(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RepositoryLanguagePanel(
    selected: String?,
    languages: List<String>,
    onSelect: (String?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RepositoryNestedPanelHeader(title = "Filter by Language", onBack = onBack)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            RepositoryChoiceTile(
                label = "All Languages",
                selected = selected == null,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(null) }
            )
            languages.forEach { item ->
                RepositoryChoiceTile(
                    label = item,
                    selected = item == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun RepositorySortPanel(
    selected: RepositoryResultSort,
    ascending: Boolean,
    onSelect: (RepositoryResultSort) -> Unit,
    onAscendingChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RepositoryNestedPanelHeader(title = "Sort by", onBack = onBack)

        RepositoryResultSort.entries.forEach { item ->
            Surface(
                onClick = { onSelect(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (item == selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item == selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = item.label,
                        modifier = Modifier.weight(1f),
                        color = if (item == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RepositoryChoiceTile(
                label = "Descending",
                selected = !ascending,
                modifier = Modifier.weight(1f),
                onClick = { onAscendingChange(false) }
            )
            RepositoryChoiceTile(
                label = "Ascending",
                selected = ascending,
                modifier = Modifier.weight(1f),
                onClick = { onAscendingChange(true) }
            )
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RepositoryNestedPanelHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun RepositoryChoiceTile(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = .14f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    "Python", "Ruby", "Rust", "Shell", "Swift", "TypeScript"
)
