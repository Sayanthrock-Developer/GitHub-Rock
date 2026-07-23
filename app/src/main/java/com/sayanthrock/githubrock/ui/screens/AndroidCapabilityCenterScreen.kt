package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader

internal data class AndroidCapabilityState(
    val notificationsEnabled: Boolean,
    val apkInstallAllowed: Boolean,
    val batteryUnrestricted: Boolean,
    val termuxAvailable: Boolean
) {
    val readyCount: Int
        get() = listOf(
            true, // Network and foreground download service are install-time capabilities.
            notificationsEnabled,
            apkInstallAllowed,
            batteryUnrestricted,
            termuxAvailable
        ).count { it }
}

@Composable
fun AndroidCapabilityCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(readAndroidCapabilityState(context)) }

    fun refresh() {
        state = readAndroidCapabilityState(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refresh() }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidCapabilityCenterContent(
        state = state,
        onBack = onBack,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onOpenInstallPermission = { openUnknownSourcesSettings(context) },
        onOpenBatterySettings = { openBatterySettings(context) },
        onOpenAppSettings = { openAppDetails(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidCapabilityCenterContent(
    state: AndroidCapabilityState,
    onBack: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android capabilities") },
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
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = "Permission & capability center",
                    subtitle = "See what GitHub Rock can use and complete the approvals Android requires"
                )
            }
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(58.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.padding(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "${state.readyCount} of 5 ready",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "GitHub Rock requests only capabilities that are connected to implemented features.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { StandardSectionHeader("Core capabilities") }
            item {
                CapabilityCard(
                    icon = Icons.Default.Download,
                    title = "Downloads & background work",
                    description = "Network access, wake lock, foreground data-sync service, resumable WorkManager jobs, and reboot recovery.",
                    ready = true,
                    status = "Ready"
                )
            }
            item {
                CapabilityCard(
                    icon = Icons.Default.Notifications,
                    title = "Download notifications",
                    description = "Shows live APK, artifact, image, and release-download progress.",
                    ready = state.notificationsEnabled,
                    status = if (state.notificationsEnabled) "Allowed" else "Approval needed",
                    actionLabel = if (state.notificationsEnabled) null else "Allow notifications",
                    onAction = onRequestNotifications
                )
            }
            item {
                CapabilityCard(
                    icon = Icons.Default.InstallMobile,
                    title = "Install downloaded APKs",
                    description = "Opens Android's package installer. Android still requires confirmation for every installation.",
                    ready = state.apkInstallAllowed,
                    status = if (state.apkInstallAllowed) "Allowed" else "Approval needed",
                    actionLabel = if (state.apkInstallAllowed) null else "Allow APK installs",
                    onAction = onOpenInstallPermission
                )
            }
            item {
                CapabilityCard(
                    icon = Icons.Default.BatterySaver,
                    title = "Battery & background reliability",
                    description = "WorkManager already handles normal background scheduling. Unrestricted battery mode is optional for very large downloads.",
                    ready = state.batteryUnrestricted,
                    status = if (state.batteryUnrestricted) "Unrestricted" else "System managed",
                    actionLabel = "Open battery settings",
                    onAction = onOpenBatterySettings
                )
            }
            item {
                CapabilityCard(
                    icon = Icons.Default.Terminal,
                    title = "Termux command bridge",
                    description = "Optional command execution through the existing targeted Termux integration.",
                    ready = state.termuxAvailable,
                    status = if (state.termuxAvailable) "Termux available" else "Termux not installed"
                )
            }

            item { StandardSectionHeader("Protected by Android") }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProtectedAction("Silent APK installation or deletion")
                        ProtectedAction("Reading another app's private files or tokens")
                        ProtectedAction("Granting permissions without user approval")
                        ProtectedAction("Root, device-owner, or system-signed operations")
                        Text(
                            "These limits cannot be removed by adding manifest permissions. Shizuku or root would require a separate, explicit feature with its own consent and safety design.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Android app settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    icon: ImageVector,
    title: String,
    description: String,
    ready: Boolean,
    status: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (ready) {
                MaterialTheme.colorScheme.primary.copy(alpha = .24f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    if (ready) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = status,
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ProtectedAction(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
}

internal fun readAndroidCapabilityState(context: Context): AndroidCapabilityState {
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
    val apkInstallAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        context.packageManager.canRequestPackageInstalls()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    val termuxAvailable = runCatching {
        context.packageManager.getPackageInfo("com.termux", 0)
    }.isSuccess

    return AndroidCapabilityState(
        notificationsEnabled = notificationsEnabled,
        apkInstallAllowed = apkInstallAllowed,
        batteryUnrestricted = batteryUnrestricted,
        termuxAvailable = termuxAvailable
    )
}

private fun openUnknownSourcesSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    } else {
        Intent(Settings.ACTION_SECURITY_SETTINGS)
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun openBatterySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openAppDetails(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
