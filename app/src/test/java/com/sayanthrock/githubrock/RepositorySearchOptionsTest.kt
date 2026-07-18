package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.core.model.RepositorySearchOptions
import com.sayanthrock.githubrock.core.model.RepositorySort
import com.sayanthrock.githubrock.core.model.RepositoryTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorySearchOptionsTest {
    @Test fun queryIncludesLanguageAndTypeQualifiers() {
        val query = RepositorySearchOptions(
            query = "mobile app",
            language = "C++",
            type = RepositoryTypeFilter.Forks,
            sort = RepositorySort.Stars
        ).githubQuery()

        assertEquals("mobile app language:C++ fork:only", query)
    }

    @Test fun localFilteringAndSortingStayDeterministic() {
        val repositories = listOf(
            repository(1, "Kotlin", stars = 2, fork = false),
            repository(2, "Kotlin", stars = 9, fork = false),
            repository(3, "Java", stars = 20, fork = true)
        )
        val result = RepositorySearchOptions(
            language = "Kotlin",
            type = RepositoryTypeFilter.Sources,
            sort = RepositorySort.Stars
        ).applyLocally(repositories)

        assertEquals(listOf(2L, 1L), result.map { it.id })
        assertTrue(result.none(GitHubRepositoryModel::fork))
    }

    private fun repository(id: Long, language: String, stars: Int, fork: Boolean) = GitHubRepositoryModel(
        id = id,
        name = "repo-$id",
        fullName = "owner/repo-$id",
        owner = Owner("owner"),
        language = language,
        stars = stars,
        fork = fork
    )
}
