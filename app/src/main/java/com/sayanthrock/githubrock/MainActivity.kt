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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
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
        if (consumeOAuthCallback(intent)) setIntent(Intent())
        if (redirectNonRepositoryGitHubUrl(intent)) {
            finish()
            return
        }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val appearance = appPreferences.appearance.collectAsStateWithLifecycle(
                initialValue = AppearancePreferences(showImages = false)
            ).value
            val useDarkTheme = when (appearance.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Dynamic wallpaper colors belong to System mode only. Explicit Dark/Light
            // selections remain deterministic and are never overridden by wallpaper colors.
            val useSystemDynamicColors = appearance.themeMode == ThemeMode.System && appearance.dynamicColor
            val view = LocalView.current
            GitHubRockTheme(
                darkTheme = useDarkTheme,
                dynamicColor = useSystemDynamicColors,
                // Explicit Dark mode is always true black. Keep the stored preference for
                // backwards compatibility without allowing it to weaken the Dark contract.
                trueBlack = useDarkTheme || appearance.trueBlack,
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
                showImages = appearance.showImages
            ) {
                // Keep the system status bar visually continuous with the active app surface.
                val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    window.statusBarColor = surfaceColor
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !useDarkTheme
                        isAppearanceLightNavigationBars = !useDarkTheme
                    }
                }
                GitHubRockRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (consumeOAuthCallback(intent)) {
            setIntent(Intent())
        } else if (!redirectNonRepositoryGitHubUrl(intent)) {
            setIntent(intent)
        }
    }

    private fun consumeOAuthCallback(incomingIntent: Intent): Boolean {
        val uri = incomingIntent.data ?: return false
        if (uri.scheme.equals("githubrock", true) &&
            uri.host.equals("oauth", true) &&
            uri.path == "/callback"
        ) {
            viewModel.handleWebOAuthCallback(uri)
            incomingIntent.data = null
            return true
        }
        return false
    }

    private fun redirectNonRepositoryGitHubUrl(incomingIntent: Intent): Boolean {
        val url = incomingIntent.dataString ?: return false
        if (!GitHubUrlPolicy.isGitHubHttpsUrl(url) || GitHubUrlPolicy.isRepositoryUrl(url)) return false
        val opened = GitHubExternalLinkLauncher.open(this, url)
        if (opened) incomingIntent.data = null
        return opened
    }
}
