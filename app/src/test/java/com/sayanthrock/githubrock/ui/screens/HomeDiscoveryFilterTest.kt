package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDiscoveryFilterTest {
    private val owner = Owner(login = "SayanthRock")

    @Test
    fun androidAndDeveloperFiltersUseRepositoryMetadata() {
        val androidRepository = repository(
            id = 1,
            name = "GitHub-Rock",
            description = "Native Android developer control centre",
            language = "Kotlin",
            topics = listOf("android", "jetpack-compose"),
        )
        val webRepository = repository(
            id = 2,
            name = "Rock-Web",
            description = "Responsive website",
            language = "TypeScript",
        )

        assertEquals(
            listOf(androidRepository),
            homeRepositoryFeed(
                repositories = listOf(webRepository, androidRepository),
                platform = HomePlatform.Android,
                category = HomeCategory.Developer,
                sort = HomeSort.Updated,
            ),
        )
    }

    @Test
    fun popularSortUsesStarsBeforeUpdateTime() {
        val smaller = repository(id = 1, name = "Smaller", stars = 12, updatedAt = "2026-07-23T10:00:00Z")
        val larger = repository(id = 2, name = "Larger", stars = 900, updatedAt = "2026-07-20T10:00:00Z")

        assertEquals(
            listOf(larger, smaller),
            homeRepositoryFeed(
                repositories = listOf(smaller, larger),
                platform = HomePlatform.All,
                category = HomeCategory.All,
                sort = HomeSort.Popular,
            ),
        )
    }

    @Test
    fun swiftRepositoryAppearsForMacOsAndIos() {
        val swiftRepository = repository(
            id = 3,
            name = "Rock-Mobile",
            language = "Swift",
        )

        val platforms = repositoryPlatforms(swiftRepository)

        assertTrue(HomePlatform.MacOS in platforms)
        assertTrue(HomePlatform.IOS in platforms)
    }

    @Test
    fun unclassifiedPublicRepositoryIsAvailableAsCrossPlatform() {
        val crossPlatformRepository = repository(
            id = 4,
            name = "Rock-Tools",
            description = "General purpose repository",
            language = "TypeScript",
        )

        HomePlatform.entries
            .filterNot { it == HomePlatform.All }
            .forEach { platform ->
                assertEquals(
                    listOf(crossPlatformRepository),
                    homeRepositoryFeed(
                        repositories = listOf(crossPlatformRepository),
                        platform = platform,
                        category = HomeCategory.All,
                        sort = HomeSort.Updated,
                    ),
                )
            }
    }

    @Test
    fun privateRepositoryIsNeverExposedInHomeDiscovery() {
        val publicRepository = repository(id = 5, name = "Public-Rock")
        val privateRepository = repository(id = 6, name = "Private-Rock", private = true)

        assertEquals(
            listOf(publicRepository),
            homeRepositoryFeed(
                repositories = listOf(privateRepository, publicRepository),
                platform = HomePlatform.All,
                category = HomeCategory.All,
                sort = HomeSort.Updated,
            ),
        )
    }

    @Test
    fun relativeUpdateTimeUsesCompactStoreStyleLabels() {
        assertEquals(
            "2 w ago",
            relativeRepositoryTime(
                value = "2026-07-09T12:00:00Z",
                now = Instant.parse("2026-07-23T12:00:00Z"),
            ),
        )
    }

    private fun repository(
        id: Long,
        name: String,
        description: String? = null,
        language: String? = null,
        topics: List<String> = emptyList(),
        stars: Int = 0,
        updatedAt: String = "2026-07-23T12:00:00Z",
        private: Boolean = false,
    ) = GitHubRepositoryModel(
        id = id,
        name = name,
        fullName = "${owner.login}/$name",
        owner = owner,
        description = description,
        language = language,
        topics = topics,
        stars = stars,
        updatedAt = updatedAt,
        private = private,
    )
}
