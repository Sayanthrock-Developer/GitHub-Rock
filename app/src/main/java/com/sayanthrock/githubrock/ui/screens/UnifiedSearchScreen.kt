package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import com.sayanthrock.githubrock.data.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private enum class SearchKind(val label: String) { All("All"), Repositories("Repositories"), Owners("Owners"), Topics("Topics") }

data class UnifiedTopicResult(val name: String, val repositories: List<GitHubRepositoryModel>)

data class UnifiedSearchState(
    val query: String = "",
    val kind: String = SearchKind.All.name,
    val repositories: List<GitHubRepositoryModel> = emptyList(),
    val owners: List<GitHubUser> = emptyList(),
    val topics: List<UnifiedTopicResult> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val api: GitHubRestApi,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(UnifiedSearchState())
    val state: StateFlow<UnifiedSearchState> = _state
    val history = preferences.repositorySearchHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null
    private var page = 1

    fun queryChanged(value: String) {
        _state.value = _state.value.copy(query = value, error = null)
        searchJob?.cancel()
        if (value.trim().length < 2) {
            _state.value = _state.value.copy(repositories = emptyList(), owners = emptyList(), topics = emptyList(), loading = false, hasMore = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(220)
            search(1, append = false)
        }
    }

    fun setKind(kind: String) {
        _state.value = _state.value.copy(kind = kind)
        if (_state.value.query.trim().length >= 2) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { search(1, append = false) }
        }
    }

    fun submit(value: String = _state.value.query) {
        val normalized = value.trim()
        if (normalized.length < 2) return
        _state.value = _state.value.copy(query = normalized)
        viewModelScope.launch { preferences.addRepositorySearch(normalized) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(1, append = false) }
    }

    fun loadMore() {
        if (_state.value.loading || _state.value.loadingMore || !_state.value.hasMore) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(page + 1, append = true) }
    }

    private suspend fun search(targetPage: Int, append: Boolean) {
        val query = _state.value.query.trim()
        val kind = SearchKind.entries.firstOrNull { it.name == _state.value.kind } ?: SearchKind.All
        if (query.length < 2) return
        page = targetPage
        _state.value = _state.value.copy(loading = !append, loadingMore = append, error = null)
        try {
            val result = when (kind) {
                SearchKind.Repositories -> searchRepositories(query, targetPage)
                SearchKind.Owners -> searchOwners(query, targetPage)
                SearchKind.Topics -> searchTopics(query, targetPage)
                SearchKind.All -> searchAll(query, targetPage)
            }
            _state.value = if (append) merge(result) else result
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = _state.value.copy(loading = false, loadingMore = false, error = error.message ?: "Search failed")
        }
    }

    private suspend fun searchRepositories(query: String, targetPage: Int): UnifiedSearchState {
        val response = api.searchRepositories(query, perPage = 30, page = targetPage)
        return _state.value.copy(repositories = response.items, owners = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = response.items.size == 30)
    }

    private suspend fun searchOwners(query: String, targetPage: Int): UnifiedSearchState {
        val response = api.searchUsers(query, perPage = 30, page = targetPage)
        return _state.value.copy(owners = response.items, repositories = emptyList(), topics = emptyList(), loading = false, loadingMore = false, hasMore = response.items.size == 30)
    }

    private suspend fun searchTopics(query: String, targetPage: Int): UnifiedSearchState {
        val response = api.searchRepositories("topic:$query", perPage = 30, page = targetPage)
        val grouped = response.items.flatMap { it.topics }.filter { it.contains(query, true) }.distinctBy { it.lowercase() }.map { topic ->
            UnifiedTopicResult(topic, response.items.filter { repo -> repo.topics.any { it.equals(topic, true) } })
        }
        return _state.value.copy(topics = grouped, repositories = emptyList(), owners = emptyList(), loading = false, loadingMore = false, hasMore = response.items.size == 30)
    }

    private suspend fun searchAll(query: String, targetPage: Int): UnifiedSearchState = coroutineScope {
        val repositories = async { api.searchRepositories(query, perPage = 30, page = targetPage) }
        val owners = async { api.searchUsers(query, perPage = 30, page = targetPage) }
        val repoResponse = repositories.await()
        val ownerResponse = owners.await()
        val topics = repoResponse.items.flatMap { it.topics }.filter { it.contains(query, true) }.distinctBy { it.lowercase() }.map { topic ->
            UnifiedTopicResult(topic, repoResponse.items.filter { repo -> repo.topics.any { it.equals(topic, true) } })
        }
        _state.value.copy(repositories = repoResponse.items, owners = ownerResponse.items, topics = topics, loading = false, loadingMore = false, hasMore = repoResponse.items.size == 30 || ownerResponse.items.size == 30)
    }

    private fun merge(next: UnifiedSearchState): UnifiedSearchState {
        val current = _state.value
        return next.copy(
            repositories = (current.repositories + next.repositories).distinctBy { it.id },
            owners = (current.owners + next.owners).distinctBy { it.id },
            topics = (current.topics + next.topics).distinctBy { it.name.lowercase() }
        )
    }
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

    LaunchedEffect(Unit) { query = state.query }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; viewModel.queryChanged(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = ""; viewModel.queryChanged("") }) { Icon(Icons.Default.Clear, "Clear") } }) else null,
                placeholder = { Text("Search repositories, owners, or topics") }
            )
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchKind.entries.forEach { kind ->
                FilterChip(selected = state.kind == kind.name, onClick = { viewModel.setKind(kind.name) }, label = { Text(kind.label) })
            }
        }

        if (query.trim().length < 2 && history.isNotEmpty()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(history) { item ->
                    AssistChip(onClick = { query = item; viewModel.submit(item) }, label = { Text(item) }, leadingIcon = { Icon(Icons.Default.History, null) })
                }
            }
            return@Column
        }

        if (state.loading) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Searching…", modifier = Modifier.padding(top = 12.dp))
            }
            return@Column
        }

        if (state.error != null) {
            Text(state.error.orEmpty(), modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.repositories.isNotEmpty()) {
                item { Text("Repositories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.repositories, key = { it.id }) { repo ->
                    Surface(onClick = { onOpenRepository(repo) }, shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(repo.fullName, fontWeight = FontWeight.Bold)
                            Text(repo.description.orEmpty().ifBlank { "No description" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${repo.language ?: "Unknown"} · ★ ${repo.stars} · forks ${repo.forks}", style = MaterialTheme.typography.labelMedium)
                            if (repo.topics.isNotEmpty()) Text(repo.topics.take(5).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (state.owners.isNotEmpty()) {
                item { Text("Owners", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(state.owners, key = { it.id }) { owner ->
                    Surface(onClick = { onOpenOwner(owner.login) }, shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(owner.name?.takeIf { it.isNotBlank() } ?: owner.login, fontWeight = FontWeight.Bold)
                            Text("@${owner.login} · ${owner.followers} followers · ${owner.publicRepos} repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            owner.bio?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
                        }
                    }
                }
            }
            if (state.topics.isNotEmpty()) {
                item { Text("Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(state.topics, key = { it.name }) { topic ->
                    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("#${topic.name}", fontWeight = FontWeight.Bold)
                            Text("${topic.repositories.size} matching repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (!state.loadingMore && state.hasMore) item { AssistChip(onClick = viewModel::loadMore, label = { Text("Load more") }) }
            if (state.loadingMore) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            if (state.repositories.isEmpty() && state.owners.isEmpty() && state.topics.isEmpty()) item { Text("No results found.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
