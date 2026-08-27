package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.data.repository.GitHubNotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationsFilter { data object All : NotificationsFilter; data object Unread : NotificationsFilter }

data class GitHubNotificationsState(
    val items: List<GitHubNotification> = emptyList(),
    val filter: NotificationsFilter = NotificationsFilter.All,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val actionError: String? = null
) {
    val visibleItems get() = if (filter is NotificationsFilter.Unread) items.filter { it.unread } else items
    val unreadCount get() = items.count { it.unread }
}

@HiltViewModel
class GitHubNotificationsViewModel @Inject constructor(private val repository: GitHubNotificationsRepository) : ViewModel() {
    private val _state = MutableStateFlow(GitHubNotificationsState())
    val state: StateFlow<GitHubNotificationsState> = _state.asStateFlow()
    private var page = 0
    init { refresh() }

    fun refresh() {
        page = 0
        _state.update { it.copy(loading = true, error = null, actionError = null, hasMore = true) }
        viewModelScope.launch {
            runCatching { repository.load(1) }
                .onSuccess { result -> page = 1; _state.update { it.copy(items = result, loading = false, hasMore = result.size >= 50) } }
                .onFailure { _state.update { it.copy(loading = false, error = message(it)) } }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore) return
        _state.update { it.copy(loadingMore = true, error = null) }
        viewModelScope.launch {
            val next = page + 1
            runCatching { repository.load(next) }
                .onSuccess { result -> page = next; _state.update { it.copy(items = it.items + result, loadingMore = false, hasMore = result.size >= 50) } }
                .onFailure { _state.update { it.copy(loadingMore = false, error = message(it)) } }
        }
    }

    fun setFilter(filter: NotificationsFilter) { _state.update { it.copy(filter = filter) } }

    fun markRead(id: String) {
        if (_state.value.items.none { it.id == id && it.unread }) return
        viewModelScope.launch {
            runCatching { repository.markRead(id) }
                .onSuccess { ok -> if (ok) _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(unread = false) else it }) } else _state.update { it.copy(actionError = "GitHub could not mark this notification as read.") } }
                .onFailure { _state.update { it.copy(actionError = message(it)) } }
        }
    }

    fun markAllRead() {
        if (_state.value.unreadCount == 0) return
        viewModelScope.launch {
            runCatching { repository.markAllRead() }
                .onSuccess { ok -> if (ok) _state.update { s -> s.copy(items = s.items.map { it.copy(unread = false) }) } else _state.update { it.copy(actionError = "GitHub could not mark notifications as read.") } }
                .onFailure { _state.update { it.copy(actionError = message(it)) } }
        }
    }

    private fun message(error: Throwable) = when (error) {
        is retrofit2.HttpException -> if (error.code() == 401 || error.code() == 403) "GitHub authentication is required to load notifications." else "GitHub notifications are temporarily unavailable (HTTP ${error.code()})."
        else -> "Unable to reach GitHub. Check your connection and try again."
    }
}
