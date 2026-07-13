package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.navigation.GITHUB_SIGN_UP_URL
import com.sayanthrock.githubrock.ui.DeviceAuthState
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun LoginScreen(
    configured: Boolean,
    loading: Boolean,
    auth: DeviceAuthState,
    onLogin: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onCheckAuthorization: () -> Unit,
    onGuest: () -> Unit,
    onDemo: () -> Unit
) {
    val context = LocalContext.current
    val code = auth.code
    var hasOpenedVerificationUri by rememberSaveable(code?.verificationUri) {
        mutableStateOf(false)
    }
    LaunchedEffect(code?.verificationUri) {
        val verificationUri = code?.verificationUri
        if (verificationUri != null && !hasOpenedVerificationUri) {
            hasOpenedVerificationUri = true
            onOpenGitHubUrl(verificationUri)
        }
    }

    Box(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = .16f)) {
                Icon(Icons.Default.Code, null, Modifier.padding(20.dp).size(42.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text("GitHub Rock", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Your GitHub repositories, workflows and Android builds — from one secure mobile control centre.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (code == null) {
                Button(
                    onClick = onLogin,
                    enabled = configured && !loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Login with GitHub" }
                ) { Text(if (loading) "Connecting…" else "Login with GitHub") }
                if (!configured) {
                    Text(
                        "This build is missing its public GitHub App client ID, so sign-in is unavailable. Guest and demo modes still work.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    onClick = { onOpenGitHubUrl(GITHUB_SIGN_UP_URL) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).semantics { contentDescription = "Create GitHub account" }
                ) { Text("Create GitHub account") }
                OutlinedButton(onClick = onGuest, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue as guest") }
                TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) { Text("Explore isolated demo mode") }
            } else {
                DeviceCodeCard(
                    auth = auth,
                    checking = loading,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub verification code", code.userCode))
                    },
                    onOpen = {
                        onOpenGitHubUrl(code.verificationUri)
                    },
                    onCheck = onCheckAuthorization,
                    onRestart = onLogin,
                    onGuest = onGuest
                )
            }
        }
    }
}

@Composable
private fun DeviceCodeCard(
    auth: DeviceAuthState,
    checking: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
    onRestart: () -> Unit,
    onGuest: () -> Unit
) {
    val code = requireNotNull(auth.code)
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verify on GitHub", style = MaterialTheme.typography.titleLarge)
            Text(code.userCode, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(
                "You can enter this code at github.com/login/device in any trusted browser.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy code")
                }
                Button(onClick = onOpen) {
                    Icon(Icons.Default.OpenInBrowser, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open GitHub")
                }
            }
            Text(
                "After GitHub says you’re all set, return here with Android Back or the app switcher. The browser cannot reopen GitHub Rock automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCheck,
                enabled = !checking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (checking) "Checking GitHub…" else "I’ve authorized — check now")
            }
            TextButton(onClick = onRestart, enabled = !checking) {
                Text("Get a new verification code")
            }
            TextButton(onClick = onGuest, enabled = !checking) {
                Text("Use guest mode instead")
            }
            Text(
                auth.error ?: auth.status.orEmpty(),
                color = if (auth.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
            if (auth.error == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
