package com.sayanthrock.githubrock.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URI

const val GITHUB_SIGN_UP_URL = "https://github.com/signup"
const val GITHUB_ADD_ACCOUNT_URL = "https://github.com/login?add_account=1"
const val GITHUB_ACCOUNT_SECURITY_URL = "https://github.com/settings/security"

internal data class GitHubSignupLaunchPlan(
    val primaryUrl: String,
    val fallbackUrl: String,
    val useEphemeralTab: Boolean
)

/**
 * The signup action must always remain a signup action.
 *
 * Ephemeral Custom Tabs are preferred because they avoid an existing GitHub browser session.
 * When a browser cannot provide an ephemeral tab, the official signup page is still opened
 * instead of silently changing the action to GitHub's add-account login page.
 */
internal fun githubSignupLaunchPlan(ephemeralBrowsingSupported: Boolean): GitHubSignupLaunchPlan =
    GitHubSignupLaunchPlan(
        primaryUrl = GITHUB_SIGN_UP_URL,
        fallbackUrl = GITHUB_SIGN_UP_URL,
        useEphemeralTab = ephemeralBrowsingSupported
    )

object GitHubUrlPolicy {
    private val repositorySegment = Regex("[A-Za-z0-9_.-]+")
    private val repositoryHosts = setOf("github.com", "www.github.com")
    private val trustedHosts = repositoryHosts + "gist.github.com"
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
        if (uri.host?.lowercase() !in repositoryHosts) return false
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.size < 2 || segments.first().lowercase() in reservedRoots) return false
        return repositorySegment.matches(segments[0]) &&
            repositorySegment.matches(segments[1])
    }
}

internal fun isExternalBrowserPackage(
    candidatePackage: String?,
    applicationPackage: String
): Boolean = !candidatePackage.isNullOrBlank() && candidatePackage != applicationPackage

object GitHubExternalLinkLauncher {
    fun open(context: Context, rawUrl: String): Boolean {
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(rawUrl)) return false
        return if (rawUrl == GITHUB_SIGN_UP_URL) {
            openSignup(context)
        } else {
            openStandard(context, rawUrl)
        }
    }

    private fun openSignup(context: Context): Boolean {
        val customTabsPackage = CustomTabsClient.getPackageName(context, emptyList())
        val supportsEphemeralBrowsing = customTabsPackage
            ?.takeIf { isExternalBrowserPackage(it, context.packageName) }
            ?.let { provider ->
                runCatching {
                    CustomTabsClient.isEphemeralBrowsingSupported(context, provider)
                }.getOrDefault(false)
            }
            ?: false
        val plan = githubSignupLaunchPlan(supportsEphemeralBrowsing)

        if (
            plan.useEphemeralTab &&
            customTabsPackage != null &&
            launchCustomTab(
                context = context,
                rawUrl = plan.primaryUrl,
                browserPackage = customTabsPackage,
                ephemeral = true
            )
        ) {
            return true
        }

        return openStandard(context, plan.fallbackUrl)
    }

    private fun openStandard(context: Context, rawUrl: String): Boolean {
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(rawUrl)) return false

        val customTabsPackage = CustomTabsClient.getPackageName(context, emptyList())
        if (
            isExternalBrowserPackage(customTabsPackage, context.packageName) &&
            launchCustomTab(
                context = context,
                rawUrl = rawUrl,
                browserPackage = requireNotNull(customTabsPackage),
                ephemeral = false
            )
        ) {
            return true
        }

        return launchBrowserIntent(context, rawUrl)
    }

    private fun launchCustomTab(
        context: Context,
        rawUrl: String,
        browserPackage: String,
        ephemeral: Boolean
    ): Boolean {
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .apply {
                if (ephemeral) setEphemeralBrowsingEnabled(true)
            }
            .build()
            .apply {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.setPackage(browserPackage)
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

        return runCatching {
            customTab.launchUrl(context, Uri.parse(rawUrl))
            true
        }.getOrDefault(false)
    }

    private fun launchBrowserIntent(context: Context, rawUrl: String): Boolean {
        val uri = Uri.parse(rawUrl)
        val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val browserIntent = Intent(baseIntent).apply {
            selector = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(browserIntent)
            true
        }.getOrDefault(false)
    }
}
