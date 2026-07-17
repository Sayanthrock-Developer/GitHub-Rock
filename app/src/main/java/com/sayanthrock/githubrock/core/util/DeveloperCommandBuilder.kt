package com.sayanthrock.githubrock.core.util

import java.util.Locale

/** Builds validated commands for explicit handoff to an Android terminal. */
object DeveloperCommandBuilder {
    private val ownerPattern = Regex("^[A-Za-z0-9-]{1,39}$")
    private val repositoryPattern = Regex("^[A-Za-z0-9._-]{1,100}$")
    private val environmentVariablePattern = Regex("^[A-Z][A-Z0-9_]{1,63}$")

    const val INSTALL_TOOLCHAIN = "pkg update -y && pkg install -y git gh openssh"
    const val GITHUB_LOGIN = "gh auth login --hostname github.com --git-protocol https --web"
    const val GITHUB_SETUP_GIT = "gh auth setup-git && gh auth status"
    const val ENABLE_TERMUX_BRIDGE =
        "mkdir -p \"\$HOME/.termux\" && touch \"\$HOME/.termux/termux.properties\" && " +
            "(grep -qx 'allow-external-apps=true' \"\$HOME/.termux/termux.properties\" || " +
            "printf '\\nallow-external-apps=true\\n' >> \"\$HOME/.termux/termux.properties\") && " +
            "termux-reload-settings"

    fun repository(owner: String, name: String): String? {
        val safeOwner = owner.trim().takeIf(ownerPattern::matches) ?: return null
        val safeName = name.trim().takeIf(repositoryPattern::matches) ?: return null
        return "$safeOwner/$safeName"
    }

    fun pullRequest(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it > 0 }

    fun environmentVariable(value: String): String? =
        value.trim().uppercase(Locale.ROOT).takeIf(environmentVariablePattern::matches)

    fun checkout(owner: String, name: String, pullRequest: String): String {
        val repository = repository(owner, name) ?: return ""
        val number = pullRequest(pullRequest) ?: return ""
        return "gh pr checkout $number --repo $repository"
    }

    fun viewPullRequest(owner: String, name: String, pullRequest: String): String {
        val repository = repository(owner, name) ?: return ""
        val number = pullRequest(pullRequest) ?: return ""
        return "gh pr view $number --repo $repository --web"
    }

    fun pullRequestApi(owner: String, name: String, pullRequest: String): String {
        val repository = repository(owner, name) ?: return ""
        val number = pullRequest(pullRequest) ?: return ""
        return "gh api repos/$repository/pulls/$number"
    }

    fun clone(owner: String, name: String): String =
        repository(owner, name)?.let { "gh repo clone $it" }.orEmpty()

    fun fullGitHubSetup(): String = listOf(
        INSTALL_TOOLCHAIN,
        GITHUB_LOGIN,
        GITHUB_SETUP_GIT
    ).joinToString(" && ")

    /** Prompts inside Termux so the secret is never embedded in the generated command. */
    fun sessionApiKey(variable: String): String {
        val safeVariable = environmentVariable(variable) ?: return ""
        return "read -rsp 'Paste $safeVariable: ' $safeVariable; export $safeVariable; echo"
    }

    /**
     * Saves a prompted key in a mode-600 shell file. The key is entered only in Termux and
     * never appears in GitHub Rock, clipboard command text, or shell history.
     */
    fun persistentApiKey(variable: String): String {
        val safeVariable = environmentVariable(variable) ?: return ""
        val fileName = safeVariable.lowercase(Locale.ROOT).replace('_', '-')
        val file = "\$HOME/.config/github-rock/$fileName.env"
        return "mkdir -p \"\$HOME/.config/github-rock\" && " +
            "umask 077 && read -rsp 'Paste $safeVariable: ' _GR_KEY && " +
            "printf 'export $safeVariable=%q\\n' \"\$_GR_KEY\" > \"$file\" && " +
            "chmod 600 \"$file\" && unset _GR_KEY && echo && source \"$file\" && " +
            "echo '$safeVariable configured for this shell'"
    }

    fun loadPersistentApiKey(variable: String): String {
        val safeVariable = environmentVariable(variable) ?: return ""
        val fileName = safeVariable.lowercase(Locale.ROOT).replace('_', '-')
        return "source \"\$HOME/.config/github-rock/$fileName.env\""
    }
}