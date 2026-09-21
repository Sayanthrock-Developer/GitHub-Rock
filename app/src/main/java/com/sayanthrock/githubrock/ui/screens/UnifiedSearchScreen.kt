package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.data.repository.*
import com.sayanthrock.githubrock.data.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private enum class SearchKind(val label: String) {
    All("All"), Repositories("Repositories"), Code("Code"), Issues("Issues"),
    PullRequests("Pull requests"), Users("Users"), Commits("Commits"), Topics("Topics")
}
data class UnifiedTopicResult(val name: String, val repositories: List<GitHubRepositoryModel>)
data class UnifiedSearchState(
    val query: String = "", val kind: String = SearchKind.All.name,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val code: List<CodeSearchItem> = emptyList(),
    val issues: List<IssueSearchItem> = emptyList(),
    val pullRequests: List<IssueSearchItem> = emptyList(),
    val owners: List<GitHubUser> = emptyList(),
    val commits: List<CommitSearchItem> = emptyList(),
    val topics: List<UnifiedTopicResult> = emptyList(),
    val loading: Boolean = false, val loadingMore: Boolean = false,
    val hasMore: Boolean = false, val error: String? = null
)

@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val repository: GitHubSearchRepository,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(UnifiedSearchState())
    val state: StateFlow<UnifiedSearchState> = _state
    val history = preferences.repositorySearchHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null
    private var page = 1
    private var generation = 0L

    fun queryChanged(value: String) {
        generation++
        val requestGeneration = generation
        _state.value = _state.value.copy(query = value, error = null)
        searchJob?.cancel()
        if (value.trim().length < 2) {
            clearResults()
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            if (requestGeneration == generation) search(1, false, requestGeneration)
        }
    }

    fun setKind(kind: String) {
        generation++
        val requestGeneration = generation
        _state.value = _state.value.copy(kind = kind)
        searchJob?.cancel()
        if (_state.value.query.trim().length >= 2) {
            searchJob = viewModelScope.launch { search(1, false, requestGeneration) }
        }
    }

    fun submit(value: String = _state.value.query) {
        val normalized = value.trim()
        if (normalized.length < 2) return
        generation++
        val requestGeneration = generation
        _state.value = _state.value.copy(query = normalized)
        viewModelScope.launch { preferences.addRepositorySearch(normalized) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(1, false, requestGeneration) }
    }

    fun clearHistory() { viewModelScope.launch { preferences.clearRepositorySearchHistory() } }

    fun loadMore() {
        if (_state.value.loading || _state.value.loadingMore || !_state.value.hasMore) return
        generation++
        val requestGeneration = generation
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(page + 1, true, requestGeneration) }
    }

    private fun clearResults() {
        _state.value = _state.value.copy(
            repositories = emptyList(), code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), owners = emptyList(), commits = emptyList(),
            topics = emptyList(), loading = false, loadingMore = false, hasMore = false
        )
    }

    private suspend fun search(targetPage: Int, append: Boolean, requestGeneration: Long) {
        val query = _state.value.query.trim()
        val kind = SearchKind.entries.firstOrNull { it.name == _state.value.kind } ?: SearchKind.All
        if (query.length < 2 || requestGeneration != generation) return
        page = targetPage
        _state.value = _state.value.copy(loading = !append, loadingMore = append, error = null)
        try {
            val result: Any = when (kind) {
                SearchKind.Repositories -> repository.repositories(query, targetPage)
                SearchKind.Code -> repository.code(query, targetPage)
                SearchKind.Issues -> repository.issues(query, targetPage)
                SearchKind.PullRequests -> repository.pullRequests(query, targetPage)
                SearchKind.Users -> repository.owners(query, targetPage)
                SearchKind.Commits -> repository.commits(query, targetPage)
                SearchKind.Topics -> repository.topics(query, targetPage)
                SearchKind.All -> repository.all(query, targetPage)
            }
            if (requestGeneration != generation) return
            _state.value = if (append) merge(result, kind) else replace(result, kind)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (requestGeneration == generation) {
                _state.value = _state.value.copy(
                    loading = false, loadingMore = false,
                    error = error.message?.takeIf { it.isNotBlank() } ?: "Search failed"
                )
            }
        }
    }

    private fun replace(result: Any, kind: SearchKind): UnifiedSearchState {
        val base = _state.value.copy(
            repositories = emptyList(), code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), owners = emptyList(), commits = emptyList(), topics = emptyList()
        )
        return when (kind) {
            SearchKind.Repositories -> { val r = result as RepositorySearchPage; base.copy(repositories = r.repositories, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.Code -> { val r = result as CodeSearchPage; base.copy(code = r.items, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.Issues -> { val r = result as IssueSearchPage; base.copy(issues = r.items, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.PullRequests -> { val r = result as IssueSearchPage; base.copy(pullRequests = r.items, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.Users -> { val r = result as OwnerSearchPage; base.copy(owners = r.owners, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.Commits -> { val r = result as CommitSearchPage; base.copy(commits = r.items, hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.Topics -> { val r = result as RepositorySearchPage; base.copy(topics = topicsFrom(r.repositories), hasMore = r.hasMore, loading = false, loadingMore = false) }
            SearchKind.All -> {
                val r = result as UnifiedSearchPage
                base.copy(
                    repositories = r.repositories.repositories, code = r.code.items, issues = r.issues.items,
                    pullRequests = r.pullRequests.items, owners = r.owners.owners, commits = r.commits.items,
                    topics = topicsFrom(r.repositories.repositories), hasMore = r.hasMore,
                    loading = false, loadingMore = false
                )
            }
        }
    }

    private fun merge(result: Any, kind: SearchKind): UnifiedSearchState {
        val current = _state.value
        val next = replace(result, kind)
        return next.copy(
            repositories = (current.repositories + next.repositories).distinctBy { it.id },
            code = (current.code + next.code).distinctBy { it.sha + it.path },
            issues = (current.issues + next.issues).distinctBy { it.id },
            pullRequests = (current.pullRequests + next.pullRequests).distinctBy { it.id },
            owners = (current.owners + next.owners).distinctBy { it.id },
            commits = (current.commits + next.commits).distinctBy { it.sha },
            topics = (current.topics + next.topics).distinctBy { it.name.lowercase() }
        )
    }

    private fun topicsFrom(repositories: List<GitHubRepositoryModel>): List<UnifiedTopicResult> =
        repositories.flatMap { repo -> repo.topics.map { it to repo } }
            .groupBy { it.first.lowercase() }
            .map { (_, entries) -> UnifiedTopicResult(entries.first().first, entries.map { it.second }.distinctBy { it.id }) }
            .sortedBy { it.name.lowercase() }
}

@Composable
fun UnifiedSearchScreen(
    onBack: () -> Unit,
    onOpenRepository: (GitHubRepositoryModel) -> Unit,
    onOpenOwner: (String) -> Unit,
    viewModel: UnifiedSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val history by viewModel.history.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) { query = state.query }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; viewModel.queryChanged(it) },
                modifier = Modifier.weight(1f), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = ""; viewModel.queryChanged("") }) { Icon(Icons.Default.Clear, "Clear") } }) else null,
                placeholder = { Text("Search GitHub") }
            )
        }

        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchKind.entries.forEach { kind ->
                        FilterChip(selected = state.kind == kind.name, onClick = { viewModel.setKind(kind.name) }, label = { Text(kind.label) })
                    }
                }
            }
        }

        if (query.trim().length < 2 && history.isNotEmpty()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearHistory) { Icon(Icons.Default.DeleteSweep, "Clear search history") }
                    }
                }
                items(history, key = { it }) { item ->
                    AssistChip(onClick = { query = item; viewModel.submit(item) }, label = { Text(item) }, leadingIcon = { Icon(Icons.Default.History, null) })
                }
            }
            return@Column
        }

        if (state.loading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Searching GitHub…", Modifier.padding(top = 12.dp))
            }
            return@Column
        }

        state.error?.let {
            Text(it, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.repositories.isNotEmpty()) {
                item { SectionTitle("Repositories") }
                items(state.repositories, key = { "repo-" + it.id }) { repo ->
                    ResultCard({ onOpenRepository(repo) }) {
                        Text(repo.fullName, fontWeight = FontWeight.Bold)
                        Text(repo.description.orEmpty().ifBlank { "No description" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text((repo.language ?: "Unknown") + " · ★ " + repo.stars + " · forks " + repo.forks, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (state.code.isNotEmpty()) {
                item { SectionTitle("Code") }
                items(state.code, key = { "code-" + it.sha + "-" + it.path }) { item ->
                    ResultCard({ item.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.path, style = MaterialTheme.typography.labelMedium)
                        item.repository?.fullName?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            if (state.issues.isNotEmpty()) {
                item { SectionTitle("Issues") }
                items(state.issues, key = { "issue-" + it.id }) { item -> SearchIssueCard(item, "Issue", uriHandler) }
            }
            if (state.pullRequests.isNotEmpty()) {
                item { SectionTitle("Pull requests") }
                items(state.pullRequests, key = { "pr-" + it.id }) { item -> SearchIssueCard(item, "Pull request", uriHandler) }
            }
            if (state.owners.isNotEmpty()) {
                item { SectionTitle("Users") }
                items(state.owners, key = { "user-" + it.id }) { owner ->
                    ResultCard({ onOpenOwner(owner.login) }) {
                        Text(owner.name?.takeIf(String::isNotBlank) ?: owner.login, fontWeight = FontWeight.Bold)
                        Text("@" + owner.login + " · " + owner.followers + " followers · " + owner.publicRepos + " repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.commits.isNotEmpty()) {
                item { SectionTitle("Commits") }
                items(state.commits, key = { "commit-" + it.sha }) { item ->
                    ResultCard({ item.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
                        Text(item.commit.message.lineSequence().firstOrNull().orEmpty(), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.sha.take(7) + (item.repository?.fullName?.let { " · " + it } ?: ""), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (state.topics.isNotEmpty()) {
                item { SectionTitle("Topics") }
                items(state.topics, key = { "topic-" + it.name }) { topic ->
                    ResultCard({ topic.repositories.firstOrNull()?.let(onOpenRepository) }) {
                        Text("#" + topic.name, fontWeight = FontWeight.Bold)
                        Text(topic.repositories.size.toString() + " matching repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!state.loadingMore && state.hasMore) item { AssistChip(onClick = viewModel::loadMore, label = { Text("Load more") }) }
            if (state.loadingMore) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            if (state.repositories.isEmpty() && state.code.isEmpty() && state.issues.isEmpty() && state.pullRequests.isEmpty() && state.owners.isEmpty() && state.commits.isEmpty() && state.topics.isEmpty()) {
                item { Text("No results found.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable private fun ResultCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp), content = content)
    }
}

@Composable private fun SearchIssueCard(item: IssueSearchItem, type: String, uriHandler: androidx.compose.ui.platform.UriHandler) {
    ResultCard({ item.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
        Text("#" + item.number + " · " + item.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(type + " · " + item.user.login + " · " + item.state, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
