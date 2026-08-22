package com.sayanthrock.githubrock.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val OBTAINIUM_SCHEME = "obtainium"
private const val OBTAINIUM_ADD_PATH = "add"

/** Public GitHub Store fallback page supplied by GitHub Rock for Obtainium-managed repositories. */
internal fun obtainiumStoreUrl(repositoryUrl: String): String =
    "https://github-store.org/app?repo=${repositoryUrl.removePrefix("https://github.com/")}"

internal fun openInObtainium(
    context: Context,
    repositoryUrl: String
): Result<Boolean> = runCatching {
    val normalizedUrl = repositoryUrl.trim().removeSuffix("/")
    require(normalizedUrl.startsWith("https://github.com/")) {
        "Only public GitHub repository URLs can be added to Obtainium."
    }

    val deepLink = Uri.Builder()
        .scheme(OBTAINIUM_SCHEME)
        .authority(OBTAINIUM_ADD_PATH)
        .appendPath(normalizedUrl)
        .build()

    val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(obtainiumStoreUrl(normalizedUrl))).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
        false
    }
}

internal fun isObtainiumInstalled(context: Context): Boolean = runCatching {
    context.packageManager.getLaunchIntentForPackage("dev.imranr.obtainium") != null
}.getOrDefault(false)

@Composable
internal fun ObtainiumUpdateCard(
    obtainiumInstalled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Automatic updates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (obtainiumInstalled) {
                            "Open this repository in Obtainium to track new releases and manage updates automatically."
                        } else {
                            "Add this repository to Obtainium to track new releases and manage updates automatically."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (obtainiumInstalled) {
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("Open in Obtainium")
                }
            } else {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("Open in Obtainium")
                }
            }
        }
    }
}
