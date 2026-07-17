package com.sayanthrock.githubrock.core.util

import com.sayanthrock.githubrock.core.model.GitHubUser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProfileExportFormatter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun toJson(profile: GitHubUser): String = json.encodeToString(profile)

    fun fileName(profile: GitHubUser): String {
        val safeLogin = profile.login
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "github-profile" }
        return "$safeLogin-profile.json"
    }
}
