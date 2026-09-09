package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseChecksumResolverTest {
    private fun asset(id: Long, name: String) = ReleaseAsset(
        id = id,
        name = name,
        size = 1,
        downloadUrl = "https://api.github.com/assets/$id",
        browserDownloadUrl = "https://github.com/example/repo/releases/download/v1/$name"
    )

    @Test
    fun findsMatchingSha256AssetBeforeGenericChecksum() {
        val apk = asset(1, "app-release.apk")
        val generic = asset(2, "SHA256SUMS.txt")
        val matching = asset(3, "app-release.apk.sha256")

        assertEquals(matching.id, ReleaseChecksumResolver.findFor(apk, listOf(apk, generic, matching))?.id)
    }

    @Test
    fun ignoresSha512Assets() {
        val apk = asset(1, "app-release.apk")
        val sha512 = asset(2, "app-release.apk.sha512")

        assertNull(ReleaseChecksumResolver.findFor(apk, listOf(apk, sha512)))
    }

    @Test
    fun recognizesCommonSha256Extensions() {
        assertEquals(true, ReleaseChecksumResolver.isSha256Asset("SHA256SUMS.txt"))
        assertEquals(true, ReleaseChecksumResolver.isSha256Asset("app.aab.sha256"))
        assertEquals(false, ReleaseChecksumResolver.isSha256Asset("app.aab.sha512"))
    }
}
