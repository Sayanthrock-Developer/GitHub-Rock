package com.sayanthrock.githubrock.core.navigation

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

const val GITHUB_SIGN_UP_URL = "https://github.com/signup"

object GitHubUrlPolicy {
    private val repositorySegment = Regex("[A-Za-z0-9_.-]+")
    private val trustedHosts = setOf("github.com", "www.github.com")
    private val reservedRoots = setOf(
        "about", "account", "apps", "collections", "contact", "customer-stories",
        "enterprise", "events", "explore", "features", "gist", "issues", "login",
        "marketplace", "new", "notifications", "organizations", "orgs", "pricing",
        "pulls", "readme", "security", "settings", "signup", "site", "sponsors",
        "team", "topics", "trending", "users"
    )

    fun isGitHubHttpsUrl(rawUrl: String): Boolean {
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return scheme == "https" && host in trustedHosts
    }

    fun isRepositoryUrl(rawUrl: String): Boolean {
        if (!isGitHubHttpsUrl(rawUrl)) return false
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.size < 2 || segments.first().lowercase() in reservedRoots) return false
        return repositorySegment.matches(segments[0]) &&
            repositorySegment.matches(segments[1])
    }
}

object GitHubExternalLinkLauncher {
    fun open(context: Context, rawUrl: String): Boolean {
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(rawUrl)) return false

        val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val roleManager = context.getSystemService(RoleManager::class.java)
        val defaultBrowser = runCatching {
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_BROWSER) == true) {
                roleManager.getRoleHolders(RoleManager.ROLE_BROWSER)
                    .firstOrNull { it != context.packageName }
            } else {
                null
            }
        }.getOrNull()

        val launchIntent = if (defaultBrowser != null) {
            Intent(baseIntent).setPackage(defaultBrowser)
        } else {
            val browserOnlyIntent = Intent(baseIntent).apply {
                selector = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
            Intent.createChooser(browserOnlyIntent, "Open GitHub in browser")
        }.apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }
}
