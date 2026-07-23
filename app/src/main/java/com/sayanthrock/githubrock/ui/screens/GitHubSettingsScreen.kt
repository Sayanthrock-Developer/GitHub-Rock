package com.sayanthrock.githubrock.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sayanthrock.githubrock.core.navigation.GitHubSettingOpenMode
import com.sayanthrock.githubrock.core.navigation.GitHubWebDestination
import com.sayanthrock.githubrock.core.navigation.GitHubWebSection
import com.sayanthrock.githubrock.core.navigation.filterGitHubWebSections
import com.sayanthrock.githubrock.core.navigation.githubSettingOpenMode
import com.sayanthrock.githubrock.core.navigation.githubWebSections
import com.sayanthrock.githubrock.core.navigation.isTrustedGitHubSettingsUrl
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader
import com.sayanthrock.githubrock.ui.components.StandardSettingsDivider
import com.sayanthrock.githubrock.ui.components.StandardSettingsGroup
import com.sayanthrock.githubrock.ui.components.StandardSettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSettingsScreen(
    login: String?,
    onOpenAppSettings: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var inAppDestinationId by rememberSaveable { mutableStateOf<String?>(null) }
    val sections = remember(login) { githubWebSections(login) }
    val allDestinations = remember(sections) { sections.flatMap(GitHubWebSection::destinations) }
    val inAppDestination = remember(allDestinations, inAppDestinationId) {
        allDestinations.firstOrNull { it.id == inAppDestinationId }
    }

    if (inAppDestination != null) {
        GitHubInAppSettingsBrowser(
            destination = inAppDestination,
            onBack = { inAppDestinationId = null }
        )
        return
    }

    val visibleSections = remember(sections, query) { filterGitHubWebSections(sections, query) }
    val totalDestinations = sections.sumOf { it.destinations.size }
    val visibleDestinations = visibleSections.sumOf { it.destinations.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("GitHub settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardSectionHeader(
                    title = "GitHub Rock",
                    supporting = "Native mobile controls"
                )
            }
            item {
                StandardSettingsGroup {
                    StandardSettingsRow(
                        icon = Icons.Default.Palette,
                        title = "App appearance & interface",
                        subtitle = "Themes, colors, text, display size, loading, code, and logs",
                        onClick = onOpenAppSettings,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open app appearance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Find a GitHub setting or tool") },
                    placeholder = { Text("Security, notifications, tokens, billing…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }
            item {
                Text(
                    if (query.isBlank()) {
                        "$totalDestinations GitHub destinations available"
                    } else {
                        "$visibleDestinations of $totalDestinations destinations"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            visibleSections.forEach { section ->
                item(key = "header-${section.id}") { StandardSectionHeader(section.title) }
                item(key = section.id) {
                    GitHubSettingsSection(section) { destination ->
                        when (githubSettingOpenMode(destination)) {
                            GitHubSettingOpenMode.NativeProfile,
                            GitHubSettingOpenMode.NativeRepositories -> onOpenGitHubUrl(destination.url)
                            GitHubSettingOpenMode.InAppGitHub -> inAppDestinationId = destination.id
                        }
                    }
                }
            }

            if (visibleSections.isEmpty()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("No matching GitHub setting", fontWeight = FontWeight.Bold)
                            Text(
                                "Try a shorter search such as security, apps, repositories, or billing.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubSettingsSection(
    section: GitHubWebSection,
    onOpenDestination: (GitHubWebDestination) -> Unit
) {
    StandardSettingsGroup {
        section.destinations.forEachIndexed { index, destination ->
            val mode = githubSettingOpenMode(destination)
            StandardSettingsRow(
                icon = iconForSection(section.id),
                title = destination.title,
                subtitle = destination.description,
                onClick = { onOpenDestination(destination) },
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (mode == GitHubSettingOpenMode.InAppGitHub) "IN APP" else "NATIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mode == GitHubSettingOpenMode.InAppGitHub) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Icon(
                            if (mode == GitHubSettingOpenMode.InAppGitHub) Icons.Default.Lock else Icons.Default.ChevronRight,
                            contentDescription = "Open ${destination.title} inside GitHub Rock",
                            tint = if (mode == GitHubSettingOpenMode.InAppGitHub) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
            if (index < section.destinations.lastIndex) StandardSettingsDivider()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GitHubInAppSettingsBrowser(
    destination: GitHubWebDestination,
    onBack: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by rememberSaveable(destination.id) { mutableStateOf(0) }
    var loading by rememberSaveable(destination.id) { mutableStateOf(true) }
    var errorMessage by rememberSaveable(destination.id) { mutableStateOf<String?>(null) }

    BackHandler {
        val active = webView
        if (active?.canGoBack() == true) active.goBack() else onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(destination.title)
                        Text(
                            "Secure GitHub panel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val active = webView
                            if (active?.canGoBack() == true) active.goBack() else onBack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = "Trusted GitHub connection",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(onClick = { errorMessage = null; webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "github.com · OAuth token not shared with this page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                javaScriptCanOpenWindowsAutomatically = false
                                setSupportMultipleWindows(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                safeBrowsingEnabled = true
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                    loading = newProgress < 100
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val nextUrl = request?.url?.toString().orEmpty()
                                    val trusted = isTrustedGitHubSettingsUrl(nextUrl)
                                    if (!trusted) {
                                        errorMessage = "GitHub Rock blocked a link outside trusted GitHub domains."
                                    }
                                    return !trusted
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    loading = true
                                    errorMessage = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    loading = false
                                    errorMessage = "GitHub's secure connection could not be verified. The page was blocked."
                                }
                            }
                            if (isTrustedGitHubSettingsUrl(destination.url)) {
                                loadUrl(destination.url)
                            } else {
                                loading = false
                                errorMessage = "This destination is not a trusted GitHub URL."
                            }
                        }
                    },
                    update = { active ->
                        if (active.url.isNullOrBlank() && errorMessage == null) {
                            active.loadUrl(destination.url)
                        }
                    }
                )

                errorMessage?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = .96f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Page blocked for safety",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = {
                                    errorMessage = null
                                    webView?.loadUrl(destination.url)
                                }
                            ) {
                                Text("Return to GitHub")
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(destination.id) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

private fun iconForSection(sectionId: String): ImageVector = when (sectionId) {
    "security" -> Icons.Default.Security
    "create-code", "automate-extend" -> Icons.Default.Code
    "your-github", "plan-collaborate" -> Icons.Default.AccountCircle
    else -> Icons.Default.Settings
}
