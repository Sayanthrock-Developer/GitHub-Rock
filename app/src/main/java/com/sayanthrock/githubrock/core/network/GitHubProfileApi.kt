package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.GitHubOrganizationAccount
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubProfileApi {
    @GET("users/{username}")
    suspend fun user(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun repositories(
        @Path("username") username: String,
        @Query("type") type: String = "owner",
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubRepositoryModel>

    @GET("orgs/{organization}/repos")
    suspend fun organizationRepositories(
        @Path("organization") organization: String,
        @Query("type") type: String = "public",
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubRepositoryModel>

    @GET("user/orgs")
    suspend fun organizations(
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubOrganizationAccount>

    @GET("users/{username}/followers")
    suspend fun followers(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("users/{username}/following")
    suspend fun following(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubUser>

    @GET("user/following/{username}")
    suspend fun followingStatus(@Path("username") username: String): Response<Unit>

    @PUT("user/following/{username}")
    suspend fun follow(@Path("username") username: String): Response<Unit>

    @DELETE("user/following/{username}")
    suspend fun unfollow(@Path("username") username: String): Response<Unit>
}
