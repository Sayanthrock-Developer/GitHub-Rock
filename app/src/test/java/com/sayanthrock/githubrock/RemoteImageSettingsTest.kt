package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.RemoteImageCategory
import com.sayanthrock.githubrock.data.settings.RemoteImageOverride
import com.sayanthrock.githubrock.data.settings.RemoteImageSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteImageSettingsTest {
    @Test
    fun globalSettingControlsInheritedCategories() {
        val settings = RemoteImageSettings(allEnabled = false)

        assertFalse(settings.isEnabled(RemoteImageCategory.Avatars))
        assertFalse(settings.isEnabled(RemoteImageCategory.RepositoryArtwork))
        assertFalse(settings.isEnabled(RemoteImageCategory.ProfileRepository))
    }

    @Test
    fun categoryOverrideWinsOverGlobalSetting() {
        val settings = RemoteImageSettings(
            allEnabled = false,
            avatarOverride = RemoteImageOverride.Enabled,
            repositoryArtworkOverride = RemoteImageOverride.Disabled
        )

        assertTrue(settings.isEnabled(RemoteImageCategory.Avatars))
        assertFalse(settings.isEnabled(RemoteImageCategory.RepositoryArtwork))
        assertFalse(settings.isEnabled(RemoteImageCategory.ProfileRepository))
    }

    @Test
    fun disabledGlobalCanStillAllowSelectedCategories() {
        val settings = RemoteImageSettings(
            allEnabled = false,
            avatarOverride = RemoteImageOverride.Enabled,
            profileRepositoryOverride = RemoteImageOverride.Enabled
        )

        assertTrue(settings.isEnabled(RemoteImageCategory.Avatars))
        assertTrue(settings.isEnabled(RemoteImageCategory.ProfileRepository))
    }
}
