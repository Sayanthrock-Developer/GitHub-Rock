package com.sayanthrock.githubrock.data.settings

/** Categories of remote images whose behavior can be overridden independently. */
enum class RemoteImageCategory { Avatars, RepositoryArtwork, ProfileRepository }

enum class RemoteImageOverride {
    Inherit, Enabled, Disabled;
    companion object { fun fromStored(value: String?): RemoteImageOverride = entries.firstOrNull { it.name == value } ?: Inherit }
}

enum class RemoteImageNetworkPolicy {
    AnyNetwork, WifiOnly;
    companion object { fun fromStored(value: String?): RemoteImageNetworkPolicy = entries.firstOrNull { it.name == value } ?: AnyNetwork }
}

enum class RemoteImageQuality {
    Low, Balanced, High;
    companion object { fun fromStored(value: String?): RemoteImageQuality = entries.firstOrNull { it.name == value } ?: Balanced }
}

enum class RemoteImageShape {
    Rounded, Square, Adaptive;
    companion object { fun fromStored(value: String?): RemoteImageShape = entries.firstOrNull { it.name == value } ?: Rounded }
}

enum class RemoteImagePlaceholder {
    Initials, Icon, None;
    companion object { fun fromStored(value: String?): RemoteImagePlaceholder = entries.firstOrNull { it.name == value } ?: Initials }
}

enum class RemoteImageSize {
    Small, Medium, Large;
    companion object { fun fromStored(value: String?): RemoteImageSize = entries.firstOrNull { it.name == value } ?: Medium }
}

enum class RemoteImageAnimation {
    Allow, Block;
    companion object { fun fromStored(value: String?): RemoteImageAnimation = entries.firstOrNull { it.name == value } ?: Allow }
}

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
