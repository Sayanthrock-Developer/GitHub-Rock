package com.sayanthrock.githubrock.download

/**
 * Canonical persisted download state machine. The database intentionally stores
 * the wire value so older records remain readable while all new transitions use
 * this single definition.
 */
enum class DownloadState(val wire: String) {
    QUEUED("queued"),
    STARTING("starting"),
    DOWNLOADING("downloading"),
    PAUSED("paused"),
    RETRYING("retrying"),
    COMPLETED("completed"),
    VERIFYING("verifying"),
    VERIFIED("verified"),
    INSTALLING("installing"),
    INSTALLED("installed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    UNAVAILABLE("unavailable");

    companion object {
        fun fromWire(value: String): DownloadState =
            entries.firstOrNull { it.wire == value } ?: FAILED
    }
}
