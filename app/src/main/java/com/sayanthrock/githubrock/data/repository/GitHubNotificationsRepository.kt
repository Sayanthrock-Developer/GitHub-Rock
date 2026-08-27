package com.sayanthrock.githubrock.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sayanthrock.githubrock.core.model.GitHubNotification
import com.sayanthrock.githubrock.core.network.GitHubNotificationsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubNotificationsRepository @Inject constructor(
    client: OkHttpClient,
    json: Json
) {
    private val api: GitHubNotificationsApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubNotificationsApi::class.java)

    suspend fun load(page: Int, perPage: Int = 50): List<GitHubNotification> =
        withContext(Dispatchers.IO) { api.notifications(page = page, perPage = perPage) }

    suspend fun markRead(id: String): Boolean = withContext(Dispatchers.IO) {
        api.markThreadRead(id).isSuccessful
    }

    suspend fun markAllRead(): Boolean = withContext(Dispatchers.IO) {
        api.markAllRead().isSuccessful
    }
}
