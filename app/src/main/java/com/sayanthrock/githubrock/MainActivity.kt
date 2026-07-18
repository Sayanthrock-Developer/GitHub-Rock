package com.sayanthrock.githubrock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.GitHubUrlPolicy
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.ui.GitHubRockRoot
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectNonRepositoryGitHubUrl(intent)) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val appearance by appPreferences.appearance.collectAsStateWithLifecycle(
                initialValue = AppearancePreferences(showImages = false)
            )
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appearance.themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
            GitHubRockTheme(
                darkTheme = useDarkTheme,
                dynamicColor = appearance.dynamicColor,
                trueBlack = appearance.trueBlack,
                accentColor = appearance.accentColor,
                themeStyle = appearance.themeStyle,
                displaySize = appearance.displaySize,
                fontSize = appearance.fontSize,
                fontWeight = appearance.fontWeight,
                fontFamily = appearance.fontFamily,
                loadingStyle = appearance.loadingStyle,
                codeColorStyle = appearance.codeColorStyle,
                reduceMotion = appearance.reduceMotion,
                showImages = appearance.showImages
            ) {
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
