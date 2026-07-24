package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.core.model.Release
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseFilterTest {
    @Test
    fun prereleaseOnlyRepositoriesOpenTheirAvailableDownloads() {
        assertEquals(
            ReleaseFilter.PreRelease,
            initialReleaseFilter(
                releases = listOf(release("v0.2.1", prerelease = true)),
                initialTag = null
            )
        )
    }

    @Test
    fun stableReleasesRemainTheDefaultWhenBothKindsExist() {
        assertEquals(
            ReleaseFilter.Stable,
            initialReleaseFilter(
                releases = listOf(
                    release("v0.3.0-beta.1", prerelease = true),
                    release("v0.2.1", prerelease = false)
                ),
                initialTag = null
            )
        )
    }

    @Test
    fun aDeepLinkedPrereleaseOverridesTheStableDefault() {
        assertEquals(
            ReleaseFilter.PreRelease,
            initialReleaseFilter(
                releases = listOf(
                    release("v0.3.0-beta.1", prerelease = true),
                    release("v0.2.1", prerelease = false)
                ),
                initialTag = "v0.3.0-beta.1"
            )
        )
    }

    private fun release(tag: String, prerelease: Boolean) = Release(
        id = tag.hashCode().toLong(),
        tagName = tag,
        prerelease = prerelease
    )
}
