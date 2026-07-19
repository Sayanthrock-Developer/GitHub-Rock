package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.CreateRepositoryRequest
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.model.RenameBranchRequest
import com.sayanthrock.githubrock.core.model.RepositoryLicenseTemplate
import com.sayanthrock.githubrock.core.model.RepositoryOrganization
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RepositoryCreationApi {
    @GET("user")
    suspend fun authenticatedUser(): GitHubUser

    @GET("user/orgs")
    suspend fun organizations(
        @Query("per_page") perPage: Int = 100
    ): List<RepositoryOrganization>

    @GET("gitignore/templates")
    suspend fun gitignoreTemplates(): List<String>

    @GET("licenses")
    suspend fun licenses(
        @Query("featured") featured: Boolean = true
    ): List<RepositoryLicenseTemplate>

    @POST("user/repos")
    suspend fun createUserRepository(
        @Body request: CreateRepositoryRequest
    ): GitHubRepositoryModel

    @POST("orgs/{org}/repos")
    suspend fun createOrganizationRepository(
        @Path("org") organization: String,
        @Body request: CreateRepositoryRequest
    ): GitHubRepositoryModel

    @POST("repos/{owner}/{repo}/branches/{branch}/rename")
    suspend fun renameBranch(
        @Path("owner") owner: String,
        @Path("repo") repository: String,
        @Path("branch") branch: String,
        @Body request: RenameBranchRequest
    ): Response<Unit>
}
