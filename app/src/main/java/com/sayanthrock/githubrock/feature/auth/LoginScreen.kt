package com.sayanthrock.githubrock.feature.auth

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sayanthrock.githubrock.ui.theme.GlassCard
import com.sayanthrock.githubrock.ui.theme.LiquidBackground

/**
 * Displays the GitHub authentication screen with login, verification, guest, and demo options.
 *
 * @param state The current authentication UI state.
 * @param onLogin Invoked when the user starts GitHub login.
 * @param onGuest Invoked when the user continues as a guest.
 * @param onDemo Invoked when the user opens demo mode.
 */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: () -> Unit,
    onGuest: () -> Unit,
    onDemo: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "GitHub Rock",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Your mobile GitHub developer control centre",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    state.userCode?.let { code ->
                        Text("Verification code", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                                androidx.compose.material3.Icon(Icons.Default.ContentCopy, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Copy")
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    state.verificationUri?.toUri()?.let { uri ->
                                        CustomTabsIntent.Builder().build().launchUrl(context, uri)
                                    }
                                }
                            ) {
                                androidx.compose.material3.Icon(Icons.Default.OpenInBrowser, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open GitHub")
                            }
                        }
                    } ?: Button(
                        onClick = onLogin,
                        enabled = state.clientConfigured && !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login with GitHub")
                    }

                    if (!state.clientConfigured) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Add GITHUB_CLIENT_ID to local.properties to enable Device Flow.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    state.statusMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.errorMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) {
                Text("Continue as guest")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Explore demo mode")
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "GitHub Rock never asks for your GitHub password.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
