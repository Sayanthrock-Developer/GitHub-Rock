package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
    var hasOpenedVerificationUri by rememberSaveable(code?.deviceCode) {
        mutableStateOf(false)
    }
    var showAccountSetup by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(code?.deviceCode) {
        val verificationUri = code?.verificationUri
        if (verificationUri != null && !hasOpenedVerificationUri) {
            hasOpenedVerificationUri = true
            onOpenGitHubUrl(verificationUri)
        }
    }

    Box(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues()), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .16f)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            null,
                            Modifier.padding(20.dp).size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("GitHub Rock", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Your GitHub repositories, workflows and Android builds — from one secure mobile control centre.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TrustPoint(Icons.Default.Security, "Device Flow")
                        TrustPoint(Icons.Default.Code, "No password")
                        TrustPoint(Icons.Default.AccountCircle, "GitHub App")
                    }
                }
            }

            if (code == null) {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (showAccountSetup) "Create, then connect" else "Connect your GitHub account",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            if (showAccountSetup) {
                                "GitHub signup opens securely in your browser. After creating the account, return here and connect it with Device Flow."
                            } else {
                                "GitHub Rock never asks for your password. GitHub authorizes this app in your browser."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (showAccountSetup) {
                            Button(
                                onClick = onLogin,
                                enabled = configured && !loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .semantics { contentDescription = "Connect new GitHub account" }
                            ) {
                                Icon(Icons.Default.ArrowForward, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (loading) "Preparing connection…" else "I created an account — connect")
                            }
                            OutlinedButton(
                                onClick = { onOpenGitHubUrl(GITHUB_SIGN_UP_URL) },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open GitHub signup again")
                            }
                            TextButton(
                                onClick = { showAccountSetup = false },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) { Text("I already have an account") }
                        } else {
                            Button(
                                onClick = onLogin,
                                enabled = configured && !loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .semantics { contentDescription = "Login with GitHub" }
                            ) {
                                Icon(Icons.Default.AccountCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (loading) "Connecting…" else "Connect with GitHub")
                            }
                            OutlinedButton(
                                onClick = {
                                    showAccountSetup = true
                                    onOpenGitHubUrl(GITHUB_SIGN_UP_URL)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .semantics { contentDescription = "Create GitHub account" }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create a GitHub account")
                            }
                        }

                        if (!configured) {
                            Text(
                                "This build is missing its public GitHub App client ID, so account connection is unavailable. Guest and demo modes still work.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedButton(
                            onClick = onGuest,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Continue with public repositories") }
                        TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                            Text("Explore isolated demo mode")
                        }
                    }
                }
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
private fun TrustPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            Text(
                "GitHub shows the approximate city and IP that requested this code. Authorize only if it matches the network you are using; otherwise cancel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
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
                "After GitHub says you’re all set, return with Android Back or the app switcher. GitHub Rock checks automatically; the button below is a backup.",
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
