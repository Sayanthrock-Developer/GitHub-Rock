package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.RepositoryDto
import com.sayanthrock.githubrock.core.model.RepositoryOwnerDto
import com.sayanthrock.githubrock.core.model.toSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMapperTest {
    @Test
    fun `repository dto preserves identity and visibility`() {
        val summary = RepositoryDto(
            id = 42,
            name = "GitHub-Rock",
            fullName = "SayanthRock/GitHub-Rock",
            owner = RepositoryOwnerDto("SayanthRock"),
            isPrivate = true,
            htmlUrl = "https://github.com/SayanthRock/GitHub-Rock",
            stars = 10
        ).toSummary()

        assertEquals("SayanthRock/GitHub-Rock", summary.fullName)
        assertEquals(10, summary.stars)
        assertTrue(summary.isPrivate)
    }
}
