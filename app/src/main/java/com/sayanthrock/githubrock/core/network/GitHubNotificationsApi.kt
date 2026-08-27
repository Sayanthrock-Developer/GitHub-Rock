package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.GitHubNotification
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubNotificationsApi {
    @GET("notifications")
    suspend fun notifications(
        @Query("all") all: Boolean = true,
        @Query("participating") participating: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): List<GitHubNotification>

    @PUT("notifications/threads/{threadId}")
    suspend fun markThreadRead(@Path("threadId") threadId: String): Response<Unit>

    @PUT("notifications")
    suspend fun markAllRead(): Response<Unit>
}
