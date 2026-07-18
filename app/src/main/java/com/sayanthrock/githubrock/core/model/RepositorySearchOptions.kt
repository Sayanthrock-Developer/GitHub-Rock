package com.sayanthrock.githubrock.core.model

enum class RepositoryTypeFilter(val label: String) {
    All("All types"),
    Sources("Sources"),
    Forks("Forks"),
    Public("Public"),
    Private("Private")
}

enum class RepositorySort(val label: String, val apiValue: String) {
    Updated("Recently updated", "updated"),
    Stars("Most stars", "stars"),
    Forks("Most forks", "forks")
}

data class RepositorySearchOptions(
    val query: String = "",
    val language: String? = null,
    val type: RepositoryTypeFilter = RepositoryTypeFilter.All,
    val sort: RepositorySort = RepositorySort.Updated
) {
    fun githubQuery(): String = buildList {
        query.trim().takeIf(String::isNotBlank)?.let(::add)
        language?.trim()?.takeIf(String::isNotBlank)?.let { add("language:${it.asQualifierValue()}") }
        when (type) {
            RepositoryTypeFilter.All -> Unit
            RepositoryTypeFilter.Sources -> add("fork:false")
            RepositoryTypeFilter.Forks -> add("fork:only")
            RepositoryTypeFilter.Public -> add("is:public")
            RepositoryTypeFilter.Private -> add("is:private")
        }
    }.joinToString(" ").ifBlank { "android stars:>1000" }

    fun applyLocally(repositories: List<GitHubRepositoryModel>): List<GitHubRepositoryModel> {
        val filtered = repositories.filter { repository ->
            val languageMatches = language.isNullOrBlank() || repository.language.equals(language, ignoreCase = true)
            val typeMatches = when (type) {
                RepositoryTypeFilter.All -> true
                RepositoryTypeFilter.Sources -> !repository.fork
                RepositoryTypeFilter.Forks -> repository.fork
                RepositoryTypeFilter.Public -> !repository.private
                RepositoryTypeFilter.Private -> repository.private
            }
            languageMatches && typeMatches
        }
        return when (sort) {
            RepositorySort.Updated -> filtered.sortedByDescending(GitHubRepositoryModel::updatedAt)
            RepositorySort.Stars -> filtered.sortedByDescending(GitHubRepositoryModel::stars)
            RepositorySort.Forks -> filtered.sortedByDescending(GitHubRepositoryModel::forks)
        }
    }

    private fun String.asQualifierValue(): String {
        val safe = replace("\\", "").replace("\"", "")
        return if (safe.any(Char::isWhitespace)) "\"$safe\"" else safe
    }
}
