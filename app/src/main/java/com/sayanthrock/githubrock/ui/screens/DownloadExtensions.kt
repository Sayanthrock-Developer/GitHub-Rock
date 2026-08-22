package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.data.local.DownloadEntity

/** Shared download classification used by multiple download screens. */
internal fun DownloadEntity.isApkDownload(): Boolean =
    fileName.endsWith(".apk", ignoreCase = true)
