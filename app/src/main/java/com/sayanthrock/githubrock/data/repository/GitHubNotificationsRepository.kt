package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.core.network.GitHubRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubNotificationsRepository @Inject constructor(
    private val githubApi: GitHubRestApi
) {
    suspend fun load(page: Int, perPage: Int = 50): List<GitHubNotification> =
        withContext(Dispatchers.IO) { githubApi.notifications(page = page, perPage = perPage) }

    suspend fun markRead(id: String): Boolean = withContext(Dispatchers.IO) {
        githubApi.markNotificationRead(id).isSuccessful
    }

    suspend fun markAllRead(): Boolean = withContext(Dispatchers.IO) {
        githubApi.markNotificationsRead().isSuccessful
    }
}
