package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.ui.components.GlassCard

enum class ProfileUpdateSection(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    WhatsNew(
        route = "whats-new",
        title = "What's new",
        subtitle = "Recent GitHub Rock improvements",
        icon = Icons.Default.AutoAwesome
    ),
    Announcements(
        route = "announcements",
        title = "Announcements",
        subtitle = "Important app and account notices",
        icon = Icons.Default.Announcement
    );

    companion object {
        fun fromRoute(value: String?): ProfileUpdateSection =
            entries.firstOrNull { it.route.equals(value, ignoreCase = true) } ?: WhatsNew
    }
}

private data class ProfileUpdateNotice(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUpdatesScreen(
    section: ProfileUpdateSection,
    onBack: () -> Unit
) {
    val notices = when (section) {
        ProfileUpdateSection.WhatsNew -> listOf(
            ProfileUpdateNotice(
                icon = Icons.Default.Person,
                title = "New profile experience",
                detail = "A compact profile menu now opens the full native dashboard, followers, following, and repository library.",
                badge = "PROFILE"
            ),
            ProfileUpdateNotice(
                icon = Icons.Default.Download,
                title = "Icon-first downloads",
                detail = "Downloaded APKs show their real application artwork, label, package name, progress, and install actions.",
                badge = "DOWNLOADS"
            ),
            ProfileUpdateNotice(
                icon = Icons.Default.Security,
                title = "Android capability centre",
                detail = "Review notification, package-install, background-download, battery, and Termux readiness in one place.",
                badge = "ANDROID"
            ),
            ProfileUpdateNotice(
                icon = Icons.Default.CheckCircle,
                title = "Native GitHub settings",
                detail = "Profile and repositories stay native, while unsupported sensitive GitHub pages use the protected in-app panel.",
                badge = "SETTINGS"
            )
        )

        ProfileUpdateSection.Announcements -> listOf(
            ProfileUpdateNotice(
                icon = Icons.Default.Security,
                title = "Sensitive GitHub changes may require sign-in",
                detail = "GitHub can request authentication again for passwords, passkeys, tokens, billing, and other protected account actions.",
                badge = "SECURITY"
            ),
            ProfileUpdateNotice(
                icon = Icons.Default.Person,
                title = "One OAuth account is stored at a time",
                detail = "Organizations and public profiles can be browsed without replacing the connected account. Replacing it securely removes the current token.",
                badge = "ACCOUNT"
            ),
            ProfileUpdateNotice(
                icon = Icons.Default.Download,
                title = "APK installation always uses Android confirmation",
                detail = "GitHub Rock opens the system package installer. Android does not permit a normal app to install or delete packages silently.",
                badge = "ANDROID"
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(section.title, fontWeight = FontWeight.Black)
                        Text(
                            section.subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileUpdateSummary(section = section, noticeCount = notices.size)
            }
            items(notices, key = ProfileUpdateNotice::title) { notice ->
                ProfileUpdateNoticeCard(notice)
            }
        }
    }
}

@Composable
private fun ProfileUpdateSummary(section: ProfileUpdateSection, noticeCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "GitHub Rock ${BuildConfig.VERSION_NAME} · $noticeCount items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileUpdateNoticeCard(notice: ProfileUpdateNotice) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(notice.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(notice.badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text(notice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(notice.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
