package com.sayanthrock.githubrock.core.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sayanthrock.githubrock.core.database.RecentRepositoryDao
import com.sayanthrock.githubrock.core.database.RecentRepositoryEntity
import com.sayanthrock.githubrock.core.model.GitHubUserDto
import com.sayanthrock.githubrock.core.model.RateLimitCore
import com.sayanthrock.githubrock.core.model.RepositorySummary
import com.sayanthrock.githubrock.core.model.toSummary
import com.sayanthrock.githubrock.core.network.GitHubApi
import com.sayanthrock.githubrock.core.security.TokenStore
import com.sayanthrock.githubrock.demo.DemoData
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val api: GitHubApi,
    private val tokenStore: TokenStore,
    private val recentDao: RecentRepositoryDao
) {
    fun repositories(demoMode: Boolean, searchQuery: String): Pager<Int, RepositorySummary> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
        pagingSourceFactory = {
            RepositoryPagingSource(api, tokenStore.hasSession(), demoMode, searchQuery.trim())
        }
    )

    fun recentRepositories(): Flow<List<RecentRepositoryEntity>> = recentDao.observeRecent()

    suspend fun markOpened(repository: RepositorySummary) {
        recentDao.upsert(
            RecentRepositoryEntity(
                id = repository.id,
                fullName = repository.fullName,
                description = repository.description,
                htmlUrl = repository.htmlUrl,
                language = repository.language,
                stars = repository.stars,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
        recentDao.trim()
    }

    suspend fun profile(): GitHubUserDto {
        val response = api.currentUser()
        if (!response.isSuccessful) throw IOException("GitHub profile request failed (${response.code()}).")
        return response.body() ?: throw IOException("GitHub returned an empty profile response.")
    }

    suspend fun rateLimit(): RateLimitCore {
        val response = api.rateLimit()
        if (!response.isSuccessful) throw IOException("GitHub rate-limit request failed (${response.code()}).")
        return response.body()?.resources?.core
            ?: throw IOException("GitHub returned an empty rate-limit response.")
    }
}

private class RepositoryPagingSource(
    private val api: GitHubApi,
    private val authenticated: Boolean,
    private val demoMode: Boolean,
    private val searchQuery: String
) : PagingSource<Int, RepositorySummary>() {
    override fun getRefreshKey(state: PagingState<Int, RepositorySummary>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RepositorySummary> {
        val page = params.key ?: 1
        return try {
            if (demoMode) {
                val filtered = DemoData.repositories.filter {
                    searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.description.orEmpty().contains(searchQuery, ignoreCase = true)
                }
                return LoadResult.Page(filtered, prevKey = null, nextKey = null)
            }

            val items = if (authenticated && searchQuery.isBlank()) {
                val response = api.userRepositories(page, params.loadSize.coerceAtMost(100))
                if (!response.isSuccessful) throw IOException("Repository request failed (${response.code()}).")
                response.body().orEmpty().map { it.toSummary() }
            } else {
                val query = searchQuery.ifBlank { "stars:>5000" }
                val response = api.searchRepositories(query, page, params.loadSize.coerceAtMost(100))
                if (!response.isSuccessful) throw IOException("Repository search failed (${response.code()}).")
                response.body()?.items.orEmpty().map { it.toSummary() }
            }

            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (items.isEmpty()) null else page + 1
            )
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }
    }
}
