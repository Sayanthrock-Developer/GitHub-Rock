package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.data.local.DownloadEntity

/** Shared download classification used by download screens outside the redesign screen. */
internal fun DownloadEntity.isApkDownload(@Suppress("UNUSED_PARAMETER") shared: Unit = Unit): Boolean =
    fileName.endsWith(".apk", ignoreCase = true)
