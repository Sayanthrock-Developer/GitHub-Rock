package com.sayanthrock.githubrock.data.local

/** Persisted download lifecycle exposed as a typed domain state. */
enum class DownloadState(val wireValue: String) {
    QUEUED("queued"),
    DOWNLOADING("downloading"),
    PAUSED("paused"),
    VERIFYING("verifying"),
    COMPLETED("completed"),
    INSTALLABLE("installable"),
    FAILED("failed"),
    RETRYING("retrying"),
    CANCELLED("cancelled");

    companion object {
        fun fromWireValue(value: String): DownloadState =
            entries.firstOrNull { it.wireValue == value } ?: FAILED
    }
}

val DownloadEntity.state: DownloadState
    get() = DownloadState.fromWireValue(status)
