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

enum class RepositorySourceFilter(val label: String) {
    AllGitHub("All GitHub"),
    Public("Public repositories"),
    ConnectedAccount("My account"),
    Organization("Organization"),
    User("Other account")
}

enum class RepositoryPlatformFilter(val label: String, val topic: String?) {
    All("All platforms", null),
    Android("Android", "android"),
    Web("Web", "web"),
    Desktop("Desktop", "desktop"),
    Multiplatform("Multiplatform", "multiplatform")
}

data class RepositorySearchOptions(
    val query: String = "",
    val language: String? = null,
    val type: RepositoryTypeFilter = RepositoryTypeFilter.All,
    val sort: RepositorySort = RepositorySort.Updated,
    val source: RepositorySourceFilter = RepositorySourceFilter.AllGitHub,
    val platform: RepositoryPlatformFilter = RepositoryPlatformFilter.All,
    val sourceOwner: String? = null
) {
    fun githubQuery(): String = buildList {
        query.trim().takeIf(String::isNotBlank)?.let(::add)
        language?.trim()?.takeIf(String::isNotBlank)?.let { add("language:${it.asQualifierValue()}") }
        platform.topic?.let { add("topic:${it.asQualifierValue()}") }
        when (type) {
            RepositoryTypeFilter.All -> Unit
            RepositoryTypeFilter.Sources -> add("fork:false")
            RepositoryTypeFilter.Forks -> add("fork:only")
            RepositoryTypeFilter.Public -> add("is:public")
            RepositoryTypeFilter.Private -> add("is:private")
        }
        when (source) {
            RepositorySourceFilter.AllGitHub -> Unit
            RepositorySourceFilter.Public -> add("is:public")
            RepositorySourceFilter.ConnectedAccount,
            RepositorySourceFilter.User -> sourceOwner.normalizedOwner()?.let { add("user:${it.asQualifierValue()}") }
            RepositorySourceFilter.Organization -> sourceOwner.normalizedOwner()?.let { add("org:${it.asQualifierValue()}") }
        }
    }.distinct().joinToString(" ").ifBlank { "android stars:>1000" }

    fun applyLocally(repositories: List<GitHubRepositoryModel>): List<GitHubRepositoryModel> {
        val owner = sourceOwner.normalizedOwner()
        val filtered = repositories.filter { repository ->
            val languageMatches = language.isNullOrBlank() || repository.language.equals(language, ignoreCase = true)
            val typeMatches = when (type) {
                RepositoryTypeFilter.All -> true
                RepositoryTypeFilter.Sources -> !repository.fork
                RepositoryTypeFilter.Forks -> repository.fork
                RepositoryTypeFilter.Public -> !repository.private
                RepositoryTypeFilter.Private -> repository.private
            }
            val sourceMatches = when (source) {
                RepositorySourceFilter.AllGitHub -> true
                RepositorySourceFilter.Public -> !repository.private
                RepositorySourceFilter.ConnectedAccount,
                RepositorySourceFilter.User,
                RepositorySourceFilter.Organization -> owner == null || repository.owner.login.equals(owner, ignoreCase = true)
            }
            val platformMatches = platform.topic == null || repository.topics.any {
                it.equals(platform.topic, ignoreCase = true)
            }
            languageMatches && typeMatches && sourceMatches && platformMatches
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

    private fun String?.normalizedOwner(): String? = this
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() && it.length <= 39 && it.all { character -> character.isLetterOrDigit() || character == '-' } }
}
