package com.sayanthrock.githubrock

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.GitHubUrlPolicy
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.ui.GitHubRockRoot
import com.sayanthrock.githubrock.ui.MainViewModel
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthCallback(intent)
        if (redirectNonRepositoryGitHubUrl(intent)) { finish(); return }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val appearanceState = appPreferences.appearance.collectAsStateWithLifecycle(initialValue = AppearancePreferences(showImages = false))
            val appearance = appearanceState.value
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appearance.themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val view = LocalView.current
            SideEffect { WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !useDarkTheme; isAppearanceLightNavigationBars = !useDarkTheme } }
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
                logDisplayStyle = appearance.logDisplayStyle,
                reduceMotion = appearance.reduceMotion,
                showImages = appearance.showImages,
            ) { GitHubRockRoot(viewModel) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
        if (!redirectNonRepositoryGitHubUrl(intent)) setIntent(intent)
    }

    private fun handleOAuthCallback(incomingIntent: Intent) {
        val uri = incomingIntent.data ?: return
        if (uri.scheme.equals("githubrock", ignoreCase = true) && uri.host.equals("oauth", ignoreCase = true) && uri.path == "/callback") {
            viewModel.handleWebOAuthCallback(uri)
            incomingIntent.data = null
        }
    }

    private fun redirectNonRepositoryGitHubUrl(incomingIntent: Intent): Boolean {
        val url = incomingIntent.dataString ?: return false
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(url) || GitHubUrlPolicy.isRepositoryUrl(url)) return false
        val opened = GitHubExternalLinkLauncher.open(this, url)
        if (opened) incomingIntent.data = null
        return opened
    }
}
