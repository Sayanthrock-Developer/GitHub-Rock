package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Single icon-first download workspace for applications, files, build artifacts, and APK inspection. */
@Composable
fun DownloadsHubScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("APK Inspector", fontWeight = FontWeight.Bold)
                Text("Inspect a local APK before installing it: package, version, SDK, permissions, signatures, and SHA-256 integrity.")
                OutlinedButton(onClick = { ApkInspectorEntry.open() }) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Text("  Inspect APK")
                }
            }
        }
        DownloadsApplicationsScreen(viewModel = viewModel)
    }
}

private object ApkInspectorEntry {
    var open: () -> Unit = {}
}
