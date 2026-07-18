package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.AppInformation
import com.sayanthrock.githubrock.core.util.AppInformationProvider
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.StandardScreenHeader
import com.sayanthrock.githubrock.ui.components.StandardSectionHeader

@Composable
fun AppInformationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val information = remember(context) { AppInformationProvider.read(context) }
    AppInformationContent(
        information = information,
        onBack = onBack,
        onOpenSystemSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInformationContent(
    information: AppInformation,
    onBack: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App information") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                    title = information.appName,
                    subtitle = "Application, Android SDK, device, and installation details"
                )
            }
            item { StandardSectionHeader("Application") }
            item {
                InformationCard(
                    listOf(
                        "Version" to "${information.versionName} (${information.versionCode})",
                        "Application ID" to information.applicationId,
                        "Build type" to information.buildType,
                        "Requested permissions" to information.requestedPermissions.toString(),
                        "First installed" to information.firstInstalled,
                        "Last updated" to information.lastUpdated
                    )
                )
            }
            item { StandardSectionHeader("SDK information") }
            item {
                InformationCard(
                    listOf(
                        "Minimum Android" to "API ${information.minimumSdk}",
                        "Target Android" to "API ${information.targetSdk}",
                        "Current device" to "Android ${information.androidVersion} · API ${information.deviceSdk}",
                        "Security patch" to information.securityPatch
                    )
                )
            }
            item { StandardSectionHeader("Device") }
            item {
                InformationCard(
                    listOf(
                        "Model" to information.device,
                        "Supported ABIs" to information.supportedAbis.joinToString().ifBlank { "Not reported" }
                    )
                )
            }
            item {
                OutlinedButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Android app settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InformationCard(rows: List<Pair<String, String>>) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
