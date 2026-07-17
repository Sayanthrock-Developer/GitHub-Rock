package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.Owner
import com.sayanthrock.githubrock.ui.components.repositoryPreviewImageUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryVisualUrlTest {
    @Test
    fun explicitSecurePreviewImageIsPreferred() {
        val expected = "https://images.example.test/preview.png"
        val repository = repository(previewImageUrl = expected)

        assertEquals(expected, repository.repositoryPreviewImageUrl())
    }

    @Test
    fun missingPreviewUsesGitHubOpenGraphImage() {
        val repository = repository(previewImageUrl = null)
        val generated = repository.repositoryPreviewImageUrl()

        assertTrue(generated.startsWith("https://opengraph.githubassets.com/"))
        assertTrue(generated.endsWith("/SayanthRock/Rock-Wedding"))
    }

    @Test
    fun insecureCustomPreviewFallsBackToGitHub() {
        val repository = repository(previewImageUrl = "http://example.test/preview.png")

        assertTrue(repository.repositoryPreviewImageUrl().startsWith("https://opengraph.githubassets.com/"))
    }

    private fun repository(previewImageUrl: String?) = GitHubRepositoryModel(
        id = 1,
        name = "Rock-Wedding",
        fullName = "SayanthRock/Rock-Wedding",
        owner = Owner(login = "SayanthRock"),
        updatedAt = "2026-07-17T00:00:00Z",
        previewImageUrl = previewImageUrl
    )
}
