package com.sayanthrock.githubrock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.GitHubUrlPolicy
import com.sayanthrock.githubrock.ui.GitHubRockRoot
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectNonRepositoryGitHubUrl(intent)) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            GitHubRockTheme {
                GitHubRockRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!redirectNonRepositoryGitHubUrl(intent)) {
            setIntent(intent)
        }
    }

    private fun redirectNonRepositoryGitHubUrl(incomingIntent: Intent): Boolean {
        val url = incomingIntent.dataString ?: return false
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(url) || GitHubUrlPolicy.isRepositoryUrl(url)) {
            return false
        }
        val opened = GitHubExternalLinkLauncher.open(this, url)
        if (opened) incomingIntent.data = null
        return opened
    }
}

