package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.screens.ProfileLibrarySection
import com.sayanthrock.githubrock.ui.screens.ProfileUpdateSection
import com.sayanthrock.githubrock.ui.screens.profileLibraryKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLibraryContractTest {
    @Test
    fun profileLibraryAndUpdateRoutesAreUniqueAndRoundTrip() {
        assertEquals(
            ProfileLibrarySection.entries.size,
            ProfileLibrarySection.entries.map(ProfileLibrarySection::route).toSet().size
        )
        assertEquals(
            ProfileUpdateSection.entries.size,
            ProfileUpdateSection.entries.map(ProfileUpdateSection::route).toSet().size
        )
        ProfileLibrarySection.entries.forEach { section ->
            assertEquals(section, ProfileLibrarySection.fromRoute(section.route))
        }
        ProfileUpdateSection.entries.forEach { section ->
            assertEquals(section, ProfileUpdateSection.fromRoute(section.route))
        }
    }

    @Test
    fun favouriteKeyUsesNormalizedFullRepositoryName() {
        val repository = GitHubRepositoryModel(
            id = 1,
            name = "GitHub-Rock",
            fullName = " SayanthRock/GitHub-Rock ",
            owner = Owner("SayanthRock")
        )

        assertEquals("sayanthrock/github-rock", repository.profileLibraryKey())
        assertTrue(repository.profileLibraryKey().contains('/'))
    }
}
