package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sayanthrock.githubrock.data.settings.AppearancePreferences
import com.sayanthrock.githubrock.data.settings.ThemeMode
import com.sayanthrock.githubrock.data.settings.ThemeStyle
import com.sayanthrock.githubrock.ui.icons.RockIcon

private val LuxuryShape = RoundedCornerShape(26.dp)
private val LuxuryCardShape = RoundedCornerShape(22.dp)

@Composable
fun PremiumSetupScreen(
    appearance: AppearancePreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onThemeStyle: (ThemeStyle) -> Unit,
    onTrueBlack: (Boolean) -> Unit,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var canInstallPackages by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        canInstallPackages = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }
    val permissionsReady = notificationGranted && canInstallPackages

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    radius = 900f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = RockIcon.GitHub.vector(),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("GitHub Rock", fontWeight = FontWeight.Bold)
                    Text(
                        "Setup",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (page < 2) {
                    Text(
                        "Skip",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSetupComplete)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(if (index == page) 1.8f else 1f)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (index <= page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "setup-page"
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage()
                    1 -> AppearancePage(
                        appearance = appearance,
                        onThemeMode = onThemeMode,
                        onThemeStyle = onThemeStyle,
                        onTrueBlack = onTrueBlack
                    )
                    else -> PermissionsPage(
                        notificationGranted = notificationGranted,
                        canInstallPackages = canInstallPackages,
                        onNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onInstallPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    )
                }
            }

            if (page == 0) {
                Button(
                    onClick = { page = 1 },
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuxuryShape
                ) { Text("Continue  →") }
            } else if (page == 1) {
                Button(
                    onClick = { page = 2 },
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuxuryShape
                ) { Text("Continue  →") }
                Text(
                    "You can refine every visual detail later in Settings → Appearance.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuxuryShape,
                    enabled = permissionsReady
                ) { Text("Start GitHub Rock  →") }
                OutlinedButton(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = LuxuryShape
                ) { Text("Continue without permissions") }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Your GitHub.\nYour style.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "A native GitHub experience designed around your workflow — clean, focused and unmistakably Rock.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = LuxuryShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Liquid GitHub Luxury", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Graphite surfaces · selective glass · strong typography · fast motion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LuxuryPill("20–28dp")
                    LuxuryPill("Dark first")
                    LuxuryPill("Native")
                }
            }
        }
    }
}

@Composable
private fun AppearancePage(
    appearance: AppearancePreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onThemeStyle: (ThemeStyle) -> Unit,
    onTrueBlack: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Choose your atmosphere", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Start with a premium foundation. Everything remains editable later.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ThemeCard("System", "Follows Android", appearance.themeMode == ThemeMode.System) {
            onThemeMode(ThemeMode.System); onTrueBlack(false)
        }
        ThemeCard("Light", "Bright, clean and native", appearance.themeMode == ThemeMode.Light) {
            onThemeMode(ThemeMode.Light); onTrueBlack(false)
        }
        ThemeCard("Dark", "Graphite · Glass · GitHub", appearance.themeMode == ThemeMode.Dark && !appearance.trueBlack) {
            onThemeMode(ThemeMode.Dark); onTrueBlack(false)
        }
        ThemeCard("True Black", "AMOLED · #000000 foundation", appearance.themeMode == ThemeMode.Dark && appearance.trueBlack) {
            onThemeMode(ThemeMode.Dark); onTrueBlack(true)
        }

        Text("Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StyleChip("Liquid Glass", ThemeStyle.LiquidGlass, appearance.themeStyle, onThemeStyle, Modifier.weight(1f))
            StyleChip("Clean", ThemeStyle.Clean, appearance.themeStyle, onThemeStyle, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PermissionsPage(
    notificationGranted: Boolean,
    canInstallPackages: Boolean,
    onNotification: () -> Unit,
    onInstallPermission: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("One last step", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Allow the capabilities you want. You can change Android permissions later.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PermissionCard(
            icon = RockIcon.Notifications.vector(),
            title = "Notifications",
            description = "Builds, downloads and important activity.",
            granted = notificationGranted,
            action = onNotification,
            actionLabel = "Allow"
        )
        PermissionCard(
            icon = RockIcon.Download.vector(),
            title = "Install downloaded apps",
            description = "Allow Android to install APKs downloaded by GitHub Rock.",
            granted = canInstallPackages,
            action = onInstallPermission,
            actionLabel = "Open settings"
        )
    }
}

@Composable
private fun ThemeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LuxuryCardShape)
            .border(1.dp, borderColor, LuxuryCardShape)
            .clickable(onClick = onClick),
        shape = LuxuryCardShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StyleChip(
    label: String,
    value: ThemeStyle,
    selected: ThemeStyle,
    onSelected: (ThemeStyle) -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable { onSelected(value) },
        shape = RoundedCornerShape(18.dp),
        color = if (selected == value) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    action: () -> Unit,
    actionLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LuxuryCardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Icon(RockIcon.Check.vector(), contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = action, shape = RoundedCornerShape(14.dp)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun LuxuryPill(label: String) {
    Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
    }
}
