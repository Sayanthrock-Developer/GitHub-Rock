package com.sayanthrock.githubrock

import android.app.LocaleManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sayanthrock.githubrock.core.navigation.GitHubExternalLinkLauncher
import com.sayanthrock.githubrock.core.navigation.GitHubUrlPolicy
import com.sayanthrock.githubrock.data.settings.AppPreferences
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.ui.GitHubRockRoot
import com.sayanthrock.githubrock.ui.MainViewModel
import com.sayanthrock.githubrock.ui.theme.GitHubRockTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Explicitly keep the system navigation region transparent on devices/ROMs
        // that may otherwise apply a legacy navigation-bar surface.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (consumeOAuthCallback(intent)) setIntent(Intent())
        if (redirectNonRepositoryGitHubUrl(intent)) {
            finish()
            return
        }
        lifecycleScope.launch {
            val initialLanguageTag = appPreferences.appLanguageTag.first()
            applyAppLanguage(initialLanguageTag)
            var appliedLanguageTag = initialLanguageTag
            appPreferences.appLanguageTag
                .distinctUntilChanged()
                .collect { languageTag ->
                    if (languageTag == appliedLanguageTag) return@collect
                    appliedLanguageTag = languageTag
                    applyAppLanguage(languageTag)
                    recreate()
                }
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
            // Dynamic wallpaper colors are enabled only by the explicit System Dynamic accent choice.
            val useSystemDynamicColors = appearance.themeMode == ThemeMode.System && appearance.dynamicColor
            val view = LocalView.current
            GitHubRockTheme(
                darkTheme = useDarkTheme,
                dynamicColor = useSystemDynamicColors,
                // True black is an explicit appearance preference. Do not force it for every dark theme.
                trueBlack = appearance.trueBlack,
                accentColor = appearance.accentColor,
                customAccentHex = appearance.customAccentHex,
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
                SideEffect {
                    // enableEdgeToEdge() owns transparent system bars. Only the icon appearance is
                    // updated here so explicit Light/Dark/AMOLED choices remain synchronized with
                    // the resolved Compose theme without painting a separate system-bar surface.
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !useDarkTheme
                        isAppearanceLightNavigationBars = !useDarkTheme
                    }
                }
                GitHubRockRoot(viewModel)
            }
        }
    }

    private fun applyAppLanguage(tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = if (tag.isNullOrBlank()) {
                android.os.LocaleList.getEmptyLocaleList()
            } else {
                android.os.LocaleList.forLanguageTags(tag)
            }
            return
        }

        val configuration = Configuration(resources.configuration)
        if (tag.isNullOrBlank()) {
            configuration.setLocale(Locale.getDefault())
        } else {
            configuration.setLocale(Locale.forLanguageTag(tag))
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (consumeOAuthCallback(intent)) setIntent(Intent()) else if (!redirectNonRepositoryGitHubUrl(intent)) setIntent(intent)
    }

    private fun consumeOAuthCallback(incomingIntent: Intent): Boolean {
        val uri = incomingIntent.data ?: return false
        if (uri.scheme.equals("githubrock", true) && uri.host.equals("oauth", true) && uri.path == "/callback") {
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