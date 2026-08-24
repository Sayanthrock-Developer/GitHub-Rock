package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubIssue
import com.sayanthrock.githubrock.core.model.IssueComment
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class IssueListState { OPEN, CLOSED }

data class IssuesUiState(
    val selected: IssueListState = IssueListState.OPEN,
    val issues: List<GitHubIssue> = emptyList(),
    val comments: List<IssueComment> = emptyList(),
    val selectedIssue: GitHubIssue? = null,
    val loading: Boolean = false,
    val commentsLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class IssuesViewModel @Inject constructor(
    private val api: GitHubRestApi,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {
    private val owner = checkNotNull(savedStateHandle.get<String>("owner"))
    private val repo = checkNotNull(savedStateHandle.get<String>("repo"))
    private val _state = MutableStateFlow(IssuesUiState())
    val state = _state.asStateFlow()

    init { load(IssueListState.OPEN) }

    fun select(state: IssueListState) {
        _state.update { it.copy(selected = state, selectedIssue = null, comments = emptyList()) }
        load(state)
    }

    fun open(issue: GitHubIssue) {
        _state.update { it.copy(selectedIssue = issue, comments = emptyList(), commentsLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.issueComments(owner, repo, issue.number) }
                .onSuccess { comments -> _state.update { it.copy(comments = comments, commentsLoading = false) } }
                .onFailure { error -> _state.update { it.copy(commentsLoading = false, error = error.message ?: "Unable to load issue comments") } }
        }
    }

    fun closeDetails() = _state.update { it.copy(selectedIssue = null, comments = emptyList()) }

    fun retry() = load(_state.value.selected)

    private fun load(state: IssueListState) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                api.issues(owner, repo, state = if (state == IssueListState.OPEN) "open" else "closed", perPage = 100)
                    .filter { it.pullRequest == null }
            }.onSuccess { issues ->
                _state.update { it.copy(issues = issues, loading = false) }
            }.onFailure { error ->
                _state.update { it.copy(issues = emptyList(), loading = false, error = error.message ?: "Unable to load issues") }
            }
        }
    }
}

@Composable
fun IssuesScreen(
    onBack: () -> Unit,
    viewModel: IssuesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.selectedIssue

    if (selected != null) {
        IssueDetailsScreen(
            issue = selected,
            comments = state.comments,
            loadingComments = state.commentsLoading,
            error = state.error,
            onBack = viewModel::closeDetails,
            onRetry = { viewModel.open(selected) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issues") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selected == IssueListState.OPEN,
                    onClick = { viewModel.select(IssueListState.OPEN) },
                    label = { Text("Open") },
                    leadingIcon = { Icon(Icons.Default.ErrorOutline, null, Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = state.selected == IssueListState.CLOSED,
                    onClick = { viewModel.select(IssueListState.CLOSED) },
                    label = { Text("Closed") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) }
                )
            }

            state.error?.let { error ->
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }

            if (state.loading) {
                Spacer(Modifier.size(24.dp))
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else if (state.issues.isEmpty() && state.error == null) {
                Text(
                    if (state.selected == IssueListState.OPEN) "No open issues" else "No closed issues",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.issues, key = { it.id }) { issue ->
                        IssueCard(issue = issue, onClick = { viewModel.open(issue) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(issue: GitHubIssue, onClick: () -> Unit) {
    androidx.compose.material3.Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (issue.state.equals("open", true)) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = issue.state
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(issue.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("#${issue.number} · ${issue.user.login}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(issue.state.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
            }
            if (issue.labels.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    issue.labels.take(4).forEach { label -> AssistChip(onClick = {}, enabled = false, label = { Text(label.name) }) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Updated ${issue.updatedAt.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(16.dp))
                    Text(" ${issue.commentCount}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun IssueDetailsScreen(
    issue: GitHubIssue,
    comments: List<IssueComment>,
    loadingComments: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue #${issue.number}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(issue.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("#${issue.number} · ${issue.user.login} · ${issue.state}")
                    Text("Updated ${issue.updatedAt.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (issue.labels.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            issue.labels.forEach { AssistChip(onClick = {}, enabled = false, label = { Text(it.name) }) }
                        }
                    }
                }
            }
            item {
                androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                    Text(
                        issue.body?.ifBlank { "No description." } ?: "No description.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { Text("Comments (${issue.commentCount})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (loadingComments) item { CircularProgressIndicator() }
            error?.let {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
            items(comments, key = { it.id }) { comment ->
                androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(comment.user.login, fontWeight = FontWeight.SemiBold)
                        Text(comment.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(comment.body)
                    }
                }
            }
        }
    }
}
