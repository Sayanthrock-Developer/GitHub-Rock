package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.Release
import com.sayanthrock.githubrock.core.util.ApkInspector
import com.sayanthrock.githubrock.data.local.DownloadEntity
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** APK and installed-package state associated with one repository release. */
internal data class RepositoryAppPackageState(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkPath: String,
    val installed: Boolean,
    val openable: Boolean,
    val icon: Drawable? = null
) {
    val statusLabel: String
        get() = if (installed) "Installed" else "Ready to install"
}

/**
 * Finds the newest completed APK whose filename is present in this repository's releases.
 * Filename matching remains reliable even when a configured mirror rewrites the download URL.
 */
internal fun findRepositoryDownloadedApk(
    downloads: List<DownloadEntity>,
    releases: List<Release>
): DownloadEntity? {
    val apkNames = releases
        .asSequence()
        .flatMap { it.assets.asSequence() }
        .map { it.name.trim().lowercase(Locale.ROOT) }
        .filter { it.endsWith(".apk") }
        .toSet()
    if (apkNames.isEmpty()) return null

    return downloads
        .asSequence()
        .filter { it.status == "completed" }
        .filter { it.localPath?.endsWith(".apk", ignoreCase = true) == true }
        .filter { it.fileName.trim().lowercase(Locale.ROOT) in apkNames }
        .maxByOrNull(DownloadEntity::createdAt)
}

@Composable
internal fun rememberRepositoryAppPackageState(
    downloads: List<DownloadEntity>,
    releases: List<Release>
): RepositoryAppPackageState? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by produceState<RepositoryAppPackageState?>(
        initialValue = null,
        key1 = downloads,
        key2 = releases,
        key3 = resumeTick
    ) {
        value = withContext(Dispatchers.IO) {
            resolveRepositoryAppPackageState(context.applicationContext, downloads, releases)
        }
    }
    return state
}

@Suppress("DEPRECATION")
private fun resolveRepositoryAppPackageState(
    context: Context,
    downloads: List<DownloadEntity>,
    releases: List<Release>
): RepositoryAppPackageState? {
    val download = findRepositoryDownloadedApk(downloads, releases) ?: return null
    val apk = download.localPath?.let(::File)?.takeIf(File::isFile) ?: return null
    val inspection = ApkInspector.inspect(context, apk) ?: return null
    val packageManager = context.packageManager
    val installedPackage = runCatching {
        packageManager.getPackageInfo(inspection.packageName, 0)
    }.getOrNull()
    val launchIntent = packageManager.getLaunchIntentForPackage(inspection.packageName)
    val installed = installedPackage != null || launchIntent != null

    val archiveIcon = runCatching {
        packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?.applicationInfo
            ?.also {
                it.sourceDir = apk.absolutePath
                it.publicSourceDir = apk.absolutePath
            }
            ?.loadIcon(packageManager)
    }.getOrNull()
    val installedIcon = if (installed) {
        runCatching { packageManager.getApplicationIcon(inspection.packageName) }.getOrNull()
    } else {
        null
    }

    return RepositoryAppPackageState(
        appName = inspection.appName,
        packageName = inspection.packageName,
        versionName = inspection.versionName,
        apkPath = apk.absolutePath,
        installed = installed,
        openable = launchIntent != null,
        icon = installedIcon ?: archiveIcon
    )
}

@Composable
internal fun RepositoryAppInstallPanel(
    state: RepositoryAppPackageState,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    when (val artwork = state.icon) {
                        null -> Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> AsyncImage(
                            model = artwork,
                            contentDescription = "${state.appName} application icon",
                            modifier = Modifier.clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        state.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            state.versionName.takeIf(String::isNotBlank)?.let { "Version $it" },
                            state.packageName
                        ).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (state.installed) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = .12f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (state.installed) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = .36f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = .34f)
                        }
                    )
                ) {
                    Text(
                        state.statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (state.installed) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.installed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onUninstall,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Uninstall", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onOpen,
                        enabled = state.openable,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Open", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Install downloaded APK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

internal fun installRepositoryApk(
    context: Context,
    state: RepositoryAppPackageState
): Result<Unit> = runCatching {
    val file = File(state.apkPath)
    require(file.isFile) { "The downloaded APK is no longer available." }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        file
    )
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

internal fun openRepositoryApp(
    context: Context,
    state: RepositoryAppPackageState
): Result<Unit> = runCatching {
    val launchIntent = requireNotNull(
        context.packageManager.getLaunchIntentForPackage(state.packageName)
    ) { "This application does not expose a launcher activity." }
    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

internal fun requestRepositoryAppUninstall(
    context: Context,
    state: RepositoryAppPackageState
): Result<Unit> = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${state.packageName}")).apply {
            putExtra(Intent.EXTRA_RETURN_RESULT, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
