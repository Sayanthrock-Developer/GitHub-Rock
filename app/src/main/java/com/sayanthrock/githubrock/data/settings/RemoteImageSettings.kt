package com.sayanthrock.githubrock.data.settings

/** Categories of remote images whose behavior can be overridden independently. */
enum class RemoteImageCategory {
    Avatars,
    RepositoryArtwork,
    ProfileRepository
}

/** Category override. Inherit keeps the global "All" setting as the default. */
enum class RemoteImageOverride {
    Inherit,
    Enabled,
    Disabled
}

enum class RemoteImageNetworkPolicy {
    AnyNetwork,
    WifiOnly
}

enum class RemoteImageQuality {
    Low,
    Balanced,
    High
}

enum class RemoteImageShape {
    Rounded,
    Square,
    Adaptive
}

enum class RemoteImagePlaceholder {
    Initials,
    Icon,
    None
}

enum class RemoteImageSize {
    Small,
    Medium,
    Large
}

enum class RemoteImageAnimation {
    Allow,
    Block
}

/**
 * Central policy for remote images. The global setting is the default for every category;
 * explicit category overrides always win.
 */
data class RemoteImageSettings(
    val allEnabled: Boolean = true,
    val avatarOverride: RemoteImageOverride = RemoteImageOverride.Inherit,
    val repositoryArtworkOverride: RemoteImageOverride = RemoteImageOverride.Inherit,
    val profileRepositoryOverride: RemoteImageOverride = RemoteImageOverride.Inherit,
    val networkPolicy: RemoteImageNetworkPolicy = RemoteImageNetworkPolicy.AnyNetwork,
    val quality: RemoteImageQuality = RemoteImageQuality.Balanced,
    val cacheImages: Boolean = true,
    val shape: RemoteImageShape = RemoteImageShape.Rounded,
    val size: RemoteImageSize = RemoteImageSize.Medium,
    val placeholder: RemoteImagePlaceholder = RemoteImagePlaceholder.Initials,
    val animation: RemoteImageAnimation = RemoteImageAnimation.Allow
) {
    fun isEnabled(category: RemoteImageCategory): Boolean = when (category) {
        RemoteImageCategory.Avatars -> avatarOverride.resolve(allEnabled)
        RemoteImageCategory.RepositoryArtwork -> repositoryArtworkOverride.resolve(allEnabled)
        RemoteImageCategory.ProfileRepository -> profileRepositoryOverride.resolve(allEnabled)
    }

    private fun RemoteImageOverride.resolve(global: Boolean): Boolean = when (this) {
        RemoteImageOverride.Inherit -> global
        RemoteImageOverride.Enabled -> true
        RemoteImageOverride.Disabled -> false
    }
}
