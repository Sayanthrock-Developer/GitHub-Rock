package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.ui.DeviceAuthState
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.vector

@Composable
fun LoginScreenV2(
    configured: Boolean,
    loading: Boolean,
    auth: DeviceAuthState,
    onLogin: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onCheckAuthorization: () -> Unit,
    onGuest: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val code = auth.code

    LaunchedEffect(code?.deviceCode) {
        code?.verificationUri?.let { onOpenGitHubUrl(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.surface, colors.background)))
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            RockLogoHeader()
            when {
                code != null -> AuthorizationCard(code, loading, auth.status, onCheckAuthorization, onOpenGitHubUrl, context, onLogin, onGuest)
                auth.error != null -> ErrorCard(auth.error, onLogin)
                else -> WelcomeCard(configured, loading, onLogin, onGuest)
            }
            Text("GitHub Rock · Secure developer access", color = colors.onSurfaceVariant.copy(alpha = .72f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RockLogoHeader() {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(modifier = Modifier.size(76.dp), shape = RoundedCornerShape(26.dp), color = colors.primary.copy(alpha = .12f), border = BorderStroke(1.dp, colors.primary.copy(alpha = .32f))) {
            Box(contentAlignment = Alignment.Center) {
                Icon(RockIcon.Code.vector(), contentDescription = "GitHub Rock", tint = colors.primary, modifier = Modifier.size(36.dp))
            }
        }
        Text("Welcome to GitHub Rock", color = colors.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Your developer workspace, redesigned.", color = colors.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeCard(configured: Boolean, loading: Boolean, onLogin: () -> Unit, onGuest: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = colors.surfaceContainer.copy(alpha = .94f), border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Sign in to continue", color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Connect your GitHub account without entering your GitHub password inside the app.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            SecurityRow()
            Button(onClick = onLogin, enabled = configured && !loading, modifier = Modifier.fillMaxWidth().height(62.dp).semantics { contentDescription = "Sign in to GitHub" }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                else Icon(RockIcon.ArrowForward.vector(), contentDescription = null)
                Spacer(Modifier.width(10.dp)); Text(if (loading) "Preparing secure sign-in…" else "Continue with GitHub", fontWeight = FontWeight.Black)
            }
            Surface(modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onGuest).semantics { contentDescription = "Continue with public repositories" }, shape = RoundedCornerShape(17.dp), color = colors.surface.copy(alpha = .30f), border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .70f))) {
                Box(contentAlignment = Alignment.Center) { Text("Explore public repositories", color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
            }
            if (!configured) Text("GitHub sign-in is not configured in this build. Public repository access remains available.", color = colors.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SecurityRow() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerHigh, border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = colors.primary.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) { Icon(RockIcon.Security.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp)) }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Secure browser authorization", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("OAuth / PKCE · no GitHub password stored", color = colors.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(RockIcon.Check.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AuthorizationCard(code: DeviceCodeResponse, loading: Boolean, status: String?, onCheckAuthorization: () -> Unit, onOpenGitHubUrl: (String) -> Unit, context: Context, onRestart: () -> Unit, onGuest: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = colors.surfaceContainer.copy(alpha = .96f), border = BorderStroke(1.dp, colors.primary.copy(alpha = .18f))) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Authorize GitHub Rock", color = colors.onSurface, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(status ?: "Complete the one-time authorization in GitHub.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            Surface(shape = RoundedCornerShape(22.dp), color = colors.surfaceContainerLowest.copy(alpha = .72f), border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ONE-TIME CODE", color = colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(code.userCode, color = colors.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    TextButton(onClick = { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("GitHub verification code", code.userCode)) }) {
                        Icon(RockIcon.Copy.vector(), contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Copy code")
                    }
                }
            }
            Button(onClick = { onOpenGitHubUrl(code.verificationUri) }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(19.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                Icon(RockIcon.OpenInBrowser.vector(), contentDescription = null); Spacer(Modifier.width(9.dp)); Text("Open GitHub authorization", fontWeight = FontWeight.Black)
            }
            Button(onClick = onCheckAuthorization, enabled = !loading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = colors.primary) else Icon(RockIcon.Check.vector(), contentDescription = null)
                Spacer(Modifier.width(8.dp)); Text(if (loading) "Checking authorization…" else "I've authorized GitHub", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRestart, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Start over", color = colors.onSurfaceVariant) }
            TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Continue without an account", color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = colors.surfaceContainer, border = BorderStroke(1.dp, colors.error.copy(alpha = .22f))) {
        Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = colors.error.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) { Icon(RockIcon.Error.vector(), contentDescription = null, tint = colors.error, modifier = Modifier.size(30.dp)) }
            }
            Text("Sign-in needs another try", color = colors.onSurface, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(message, color = colors.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) { Text("Try again", fontWeight = FontWeight.Black) }
        }
    }
}
