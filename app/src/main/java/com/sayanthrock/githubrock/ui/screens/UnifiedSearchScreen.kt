package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.*
import com.sayanthrock.githubrock.data.repository.*
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.vector
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

private enum class SearchKind(val label: String) {
    All("All"), Repositories("Repositories"), Code("Code"), Issues("Issues"),
    PullRequests("Pull requests"), Users("Users"), Commits("Commits"), Topics("Topics")
}
data class UnifiedTopicResult(val name: String, val repositories: List<GitHubRepositoryModel>)
private enum class SearchError { RateLimited, Authentication, PermissionDenied, Network, Generic }

data class UnifiedSearchState(
    val query: String = "",
    val kind: String = SearchKind.All.name,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val owners: List<GitHubUser> = emptyList(),
    val code: List<CodeSearchItem> = emptyList(),
    val issues: List<GitHubIssue> = emptyList(),
    val pullRequests: List<GitHubIssue> = emptyList(),
    val commits: List<CommitSearchItem> = emptyList(),
    val topics: List<UnifiedTopicResult> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val hasSearched: Boolean = false,
    val error: SearchError? = null
)

@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val repository: GitHubSearchRepository,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(UnifiedSearchState())
    val state: StateFlow<UnifiedSearchState> = _state.asStateFlow()
    val history = preferences.repositorySearchHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null
    private var page = 1
    private var generation = 0L

    fun queryChanged(value: String) {
        generation++
        searchJob?.cancel()
        _state.update { it.copy(query = value, error = null, hasSearched = false) }
        if (value.trim().length < 2) clearResults()
    }

    fun submit(value: String = _state.value.query) {
        val normalized = normalize(value)
        if (normalized.length < 2) return
        generation++
        val requestGeneration = generation
        searchJob?.cancel()
        _state.update { it.copy(query = normalized, error = null, hasSearched = true) }
        viewModelScope.launch { preferences.addRepositorySearch(normalized) }
        searchJob = viewModelScope.launch { search(1, false, requestGeneration) }
    }

    fun setKind(kind: String) {
        if (_state.value.kind == kind) return
        _state.update { it.copy(kind = kind, error = null, hasSearched = false) }
        if (_state.value.query.trim().length >= 2) submit()
    }

    fun clearSearch() {
        generation++
        searchJob?.cancel()
        page = 1
        _state.value = UnifiedSearchState(kind = _state.value.kind)
    }

    fun clearHistory() {
        viewModelScope.launch { preferences.clearRepositorySearchHistory() }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        val requestGeneration = generation
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(page + 1, true, requestGeneration) }
    }

    private suspend fun search(targetPage: Int, append: Boolean, requestGeneration: Long) {
        val q = normalize(_state.value.query)
        if (q.length < 2) return
        page = targetPage
        _state.update { it.copy(loading = !append, loadingMore = append, error = null, hasSearched = true) }
        try {
            val next = when (SearchKind.entries.firstOrNull { it.name == _state.value.kind } ?: SearchKind.All) {
                SearchKind.All -> allState(q, targetPage)
                SearchKind.Repositories -> repoState(q, targetPage)
                SearchKind.Code -> codeState(q, targetPage)
                SearchKind.Issues -> issueState(q, targetPage, false)
                SearchKind.PullRequests -> issueState(q, targetPage, true)
                SearchKind.Users -> userState(q, targetPage)
                SearchKind.Commits -> commitState(q, targetPage)
                SearchKind.Topics -> topicState(q, targetPage)
            }
            if (requestGeneration != generation) return
            _state.value = if (append) merge(next) else next
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (requestGeneration == generation) _state.update { it.copy(loading = false, loadingMore = false, error = classify(e)) }
        }
    }

    private suspend fun repoState(q: String, p: Int) = repository.repositories(q, p).let {
        _state.value.copy(repositories = it.repositories, owners = emptyList(), code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), commits = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun userState(q: String, p: Int) = repository.owners(q, p).let {
        _state.value.copy(repositories = emptyList(), owners = it.owners, code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), commits = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun codeState(q: String, p: Int) = repository.code(q, p).let {
        _state.value.copy(repositories = emptyList(), owners = emptyList(), code = it.items, issues = emptyList(),
            pullRequests = emptyList(), commits = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun issueState(q: String, p: Int, prs: Boolean) = repository.issues(q, p, prs).let {
        _state.value.copy(repositories = emptyList(), owners = emptyList(), code = emptyList(),
            issues = if (prs) emptyList() else it.issues, pullRequests = if (prs) it.issues else emptyList(),
            commits = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun commitState(q: String, p: Int) = repository.commits(q, p).let {
        _state.value.copy(repositories = emptyList(), owners = emptyList(), code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), commits = it.commits, topics = emptyList(), loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun topicState(q: String, p: Int) = repository.topics(q, p).let {
        val topics = it.repositories.flatMap { repo -> repo.topics }.filter { name -> name.contains(q, true) }
            .distinctBy { name -> name.lowercase() }.map { name ->
                UnifiedTopicResult(name, it.repositories.filter { repo -> repo.topics.any { t -> t.equals(name, true) } })
            }
        _state.value.copy(repositories = emptyList(), owners = emptyList(), code = emptyList(), issues = emptyList(),
            pullRequests = emptyList(), commits = emptyList(), topics = topics, loading = false, loadingMore = false, hasMore = it.hasMore)
    }
    private suspend fun allState(q: String, p: Int): UnifiedSearchState {
        val a = repository.all(q, p)
        val topics = a.topics.repositories.flatMap { it.topics }.filter { it.contains(q, true) }.distinctBy { it.lowercase() }.map { name ->
            UnifiedTopicResult(name, a.topics.repositories.filter { repo -> repo.topics.any { t -> t.equals(name, true) } })
        }
        return _state.value.copy(repositories = a.repositories.repositories, owners = a.owners.owners, code = a.code.items,
            issues = a.issues.issues, pullRequests = a.pullRequests.issues, commits = a.commits.commits, topics = topics,
            loading = false, loadingMore = false, hasMore = a.repositories.hasMore || a.owners.hasMore || a.code.hasMore ||
                a.issues.hasMore || a.pullRequests.hasMore || a.commits.hasMore || a.topics.hasMore)
    }
    private fun merge(next: UnifiedSearchState) = next.copy(
        repositories = (_state.value.repositories + next.repositories).distinctBy { it.id },
        owners = (_state.value.owners + next.owners).distinctBy { it.id },
        code = (_state.value.code + next.code).distinctBy { it.sha + it.path },
        issues = (_state.value.issues + next.issues).distinctBy { it.id },
        pullRequests = (_state.value.pullRequests + next.pullRequests).distinctBy { it.id },
        commits = (_state.value.commits + next.commits).distinctBy { it.sha },
        topics = (_state.value.topics + next.topics).distinctBy { it.name.lowercase() }
    )
    private fun classify(e: Exception) = when (e) {
        is HttpException -> when (e.code()) { 401 -> SearchError.Authentication; 403, 429 -> SearchError.RateLimited; 404 -> SearchError.PermissionDenied; else -> SearchError.Generic }
        is IOException -> SearchError.Network
        else -> SearchError.Generic
    }
    private fun clearResults() { _state.update { it.copy(repositories = emptyList(), owners = emptyList(), code = emptyList(), issues = emptyList(),
        pullRequests = emptyList(), commits = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = false, error = null) } }
    private fun normalize(value: String) = value.trim().replace(Regex("\\s+"), " ")
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
    LaunchedEffect(state.query) { if (query != state.query) query = state.query }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), "Back") }
            OutlinedTextField(
                value = query, onValueChange = { query = it; viewModel.queryChanged(it) },
                modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(RockIcon.Search.vector(), null) },
                trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = viewModel::clearSearch) {
                    Icon(RockIcon.Close.vector(), "Clear search")
                } }) else null,
                placeholder = { Text("Search GitHub") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit(query) })
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchKind.entries.forEach { kind ->
                FilterChip(selected = state.kind == kind.name, onClick = { viewModel.setKind(kind.name) }, label = { Text(kind.label) })
            }
        }

        if (query.trim().length < 2 && history.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        AssistChip(onClick = viewModel::clearHistory, label = { Text("Clear history") })
                    }
                }
                items(history) { item ->
                    Surface(onClick = { query = item; viewModel.submit(item) }, shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(RockIcon.History.vector(), null)
                            Text(item, modifier = Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            return@Column
        }

        if (!state.hasSearched) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(RockIcon.Search.vector(), null)
                Text("Search GitHub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("Repositories, code, issues, pull requests, users, commits and topics.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            return@Column
        }

        if (state.loading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Searching GitHub…", modifier = Modifier.padding(top = 12.dp))
            }
            return@Column
        }

        state.error?.let {
            val message = when (it) {
                SearchError.RateLimited -> "GitHub search is temporarily rate-limited."
                SearchError.Authentication -> "GitHub authorization is required."
                SearchError.PermissionDenied -> "GitHub denied access to this search."
                SearchError.Network -> "Network unavailable. Check your connection."
                SearchError.Generic -> "GitHub could not complete the search."
            }
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(RockIcon.Error.vector(), null)
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                Button(onClick = viewModel::submit, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
            }
            return@Column
        }

        val empty = state.repositories.isEmpty() && state.owners.isEmpty() && state.code.isEmpty() &&
            state.issues.isEmpty() && state.pullRequests.isEmpty() && state.commits.isEmpty() && state.topics.isEmpty()
        if (empty) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("No results", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("GitHub returned no matches for “" + state.query + "”.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = viewModel::clearSearch, modifier = Modifier.padding(top = 16.dp)) { Text("Clear search") }
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.repositories.isNotEmpty()) {
                item { SectionTitle("Repositories", state.repositories.size) }
                items(state.repositories, key = { "repo:" + it.id }) { repo ->
                    ResultCard({ onOpenRepository(repo) }) {
                        Text(repo.fullName, fontWeight = FontWeight.Bold)
                        Text(repo.description.orEmpty().ifBlank { "No description" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text((repo.language ?: "Unknown") + " · ★ " + repo.stars + " · forks " + repo.forks, style = MaterialTheme.typography.labelMedium)
                        if (repo.topics.isNotEmpty()) Text(repo.topics.take(5).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (state.code.isNotEmpty()) {
                item { SectionTitle("Code", state.code.size) }
                items(state.code, key = { "code:" + it.sha + ":" + it.path }) { item ->
                    ResultCard({ item.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
                        Text(item.path, fontWeight = FontWeight.Bold)
                        Text(item.repository.fullName, color = MaterialTheme.colorScheme.primary)
                        Text(item.sha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.issues.isNotEmpty()) {
                item { SectionTitle("Issues", state.issues.size) }
                items(state.issues, key = { "issue:" + it.id }) { issue -> IssueResult(issue, uriHandler) }
            }
            if (state.pullRequests.isNotEmpty()) {
                item { SectionTitle("Pull requests", state.pullRequests.size) }
                items(state.pullRequests, key = { "pr:" + it.id }) { issue -> IssueResult(issue, uriHandler) }
            }
            if (state.owners.isNotEmpty()) {
                item { SectionTitle("Users", state.owners.size) }
                items(state.owners, key = { "user:" + it.id }) { owner ->
                    ResultCard({ onOpenOwner(owner.login) }) {
                        Text(owner.name?.takeIf(String::isNotBlank) ?: owner.login, fontWeight = FontWeight.Bold)
                        Text("@" + owner.login, color = MaterialTheme.colorScheme.primary)
                        Text(owner.followers.toString() + " followers · " + owner.publicRepos + " repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        owner.bio?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            if (state.commits.isNotEmpty()) {
                item { SectionTitle("Commits", state.commits.size) }
                items(state.commits, key = { "commit:" + it.sha }) { commit ->
                    ResultCard({ commit.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
                        Text(commit.commit.message.lineSequence().firstOrNull().orEmpty().ifBlank { "Commit" }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(commit.repository?.fullName ?: "GitHub commit", color = MaterialTheme.colorScheme.primary)
                        Text(commit.sha.take(7), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.topics.isNotEmpty()) {
                item { SectionTitle("Topics", state.topics.size) }
                items(state.topics, key = { "topic:" + it.name.lowercase() }) { topic ->
                    ResultCard({ topic.repositories.firstOrNull()?.let(onOpenRepository) }) {
                        Text("#" + topic.name, fontWeight = FontWeight.Bold)
                        Text(topic.repositories.size.toString() + " matching repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.hasMore) {
                item {
                    if (state.loadingMore) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    } else AssistChip(onClick = viewModel::loadMore, label = { Text("Load more") })
                }
            }
        }
    }
}

@Composable private fun SectionTitle(title: String, count: Int) {
    Text(title + " · " + count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}
@Composable private fun ResultCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
@Composable private fun IssueResult(issue: GitHubIssue, uriHandler: androidx.compose.ui.platform.UriHandler) {
    ResultCard({ issue.htmlUrl.takeIf(String::isNotBlank)?.let(uriHandler::openUri) }) {
        Text("#" + issue.number + " · " + issue.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (issue.pullRequest != null) "Pull request" else "Issue", color = MaterialTheme.colorScheme.primary)
        Text("@" + issue.user.login + " · " + issue.state, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
