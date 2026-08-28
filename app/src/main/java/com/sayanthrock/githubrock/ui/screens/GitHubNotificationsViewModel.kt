package com.sayanthrock.githubrock.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.data.repository.GitHubNotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        loadJob?.cancel()
        page = 0
        _state.update { it.copy(loading = true, loadingMore = false, error = null, actionError = null, hasMore = true) }
        loadJob = viewModelScope.launch {
            try {
                val result = repository.load(1)
                page = 1
                _state.update { it.copy(items = result, loading = false, hasMore = result.size >= PAGE_SIZE) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update { state -> state.copy(loading = false, error = message(error)) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore) return
        _state.update { it.copy(loadingMore = true, error = null) }
        loadJob = viewModelScope.launch {
            val next = page + 1
            try {
                val result = repository.load(next)
                page = next
                _state.update { it.copy(items = it.items + result, loadingMore = false, hasMore = result.size >= PAGE_SIZE) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update { state -> state.copy(loadingMore = false, error = message(error)) }
            }
        }
    }

    fun setFilter(filter: NotificationsFilter) { _state.update { it.copy(filter = filter) } }

    fun markRead(id: String) {
        if (_state.value.items.none { it.id == id && it.unread }) return
        viewModelScope.launch {
            runCatching { repository.markRead(id) }
                .onSuccess { ok -> if (ok) _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(unread = false) else it }) } else _state.update { it.copy(actionError = "GitHub could not mark this notification as read.") } }
                .onFailure { error -> _state.update { state -> state.copy(actionError = message(error)) } }
        }
    }

    fun markAllRead() {
        if (_state.value.unreadCount == 0) return
        viewModelScope.launch {
            runCatching { repository.markAllRead() }
                .onSuccess { ok -> if (ok) _state.update { s -> s.copy(items = s.items.map { it.copy(unread = false) }) } else _state.update { it.copy(actionError = "GitHub could not mark notifications as read.") } }
                .onFailure { error -> _state.update { state -> state.copy(actionError = message(error)) } }
        }
    }

    private fun message(error: Throwable) = when (error) {
        is retrofit2.HttpException -> if (error.code() == 401 || error.code() == 403) "GitHub authentication is required to load notifications." else "GitHub notifications are temporarily unavailable (HTTP ${error.code()})."
        else -> "Unable to reach GitHub. Check your connection and try again."
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
