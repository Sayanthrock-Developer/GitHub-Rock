package com.sayanthrock.githubrock.data.repository

import com.sayanthrock.githubrock.core.model.GitHubOrganizationAccount
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.core.network.GitHubProfileApi
import com.sayanthrock.githubrock.core.util.runCatchingPreservingCancellation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class NativeProfileRepository @Inject constructor(
    private val api: GitHubProfileApi,
    private val artworkResolver: RepositoryArtworkResolver
) {
    suspend fun profile(login: String): GitHubUser = withContext(Dispatchers.IO) {
        api.user(login)
    }

    suspend fun repositories(login: String): List<GitHubRepositoryModel> = withContext(Dispatchers.IO) {
        val userRepositories = runCatchingPreservingCancellation { api.repositories(login) }
        val repositories = userRepositories.getOrElse { error ->
            if (error is HttpException && error.code() == 404) {
                api.organizationRepositories(login)
            } else {
                throw error
            }
        }
        artworkResolver.attach(repositories)
    }

    suspend fun organizationRepositories(login: String): List<GitHubRepositoryModel> = withContext(Dispatchers.IO) {
        artworkResolver.attach(api.organizationRepositories(login))
    }

    suspend fun organizations(): List<GitHubOrganizationAccount> = withContext(Dispatchers.IO) {
        api.organizations()
    }

    suspend fun followers(login: String): List<GitHubUser> = withContext(Dispatchers.IO) {
        api.followers(login)
    }

    suspend fun following(login: String): List<GitHubUser> = withContext(Dispatchers.IO) {
        api.following(login)
    }

    suspend fun isFollowing(login: String): Boolean = withContext(Dispatchers.IO) {
        api.followingStatus(login).isSuccessful
    }

    suspend fun setFollowing(login: String, following: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (following) api.follow(login).isSuccessful else api.unfollow(login).isSuccessful
    }
}
