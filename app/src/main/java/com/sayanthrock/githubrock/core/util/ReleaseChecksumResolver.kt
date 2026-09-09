package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.ReleaseAsset

/** Finds a release checksum asset that can verify a package asset. */
object ReleaseChecksumResolver {
    fun findFor(target: ReleaseAsset, assets: List<ReleaseAsset>): ReleaseAsset? {
        val candidates = assets.filter { it.id != target.id && isChecksumAsset(it.name) }
        if (candidates.isEmpty()) return null
        val targetBase = target.name.substringBeforeLast('.', target.name).lowercase()
        return candidates.minByOrNull { score(it.name, targetBase) }
    }

    fun isChecksumAsset(name: String): Boolean {
        val normalized = name.trim().lowercase()
        return normalized.endsWith(".sha256") ||
            normalized.endsWith(".sha256sum") ||
            normalized.endsWith(".sha256sums") ||
            normalized.endsWith(".sha256.txt") ||
            normalized.endsWith(".sha512") ||
            normalized.endsWith(".sha512sum") ||
            normalized.endsWith(".sha512sums") ||
            normalized.endsWith(".sha512.txt") ||
            normalized.endsWith(".checksum") ||
            normalized.endsWith(".checksums")
    }

    private fun score(name: String, targetBase: String): Int {
        val normalized = name.lowercase()
        return when {
            normalized.contains(targetBase) && normalized.contains("sha256") -> 0
            normalized.contains(targetBase) -> 1
            normalized.contains("sha256") -> 2
            normalized.contains("checksum") || normalized.contains("checksums") -> 3
            else -> 10
        }
    }
}
