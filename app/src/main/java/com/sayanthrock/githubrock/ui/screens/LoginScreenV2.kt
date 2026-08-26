package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.ui.DeviceAuthState

private val RockInk = Color(0xFFF7F9FC)
private val RockMuted = Color(0xFFAAB4C3)
private val RockSurface = Color(0xFF151A22)
private val RockSurfaceRaised = Color(0xFF1B222D)
private val RockAccent = Color(0xFF63E2D8)

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
    val code = auth.code

    LaunchedEffect(code?.deviceCode) {
        code?.verificationUri?.let { onOpenGitHubUrl(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF111721), Color(0xFF0A0E13))))
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).align(Alignment.Center)
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            RockLogoHeader()
            when {
                code != null -> AuthorizationCard(code, loading, auth.status, onCheckAuthorization, onOpenGitHubUrl, context, onLogin, onGuest)
                auth.error != null -> ErrorCard(auth.error, onLogin)
                else -> WelcomeCard(configured, loading, onLogin, onGuest)
            }
            Text("GitHub Rock · Secure developer access", color = RockMuted.copy(alpha = .72f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RockLogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(modifier = Modifier.size(76.dp), shape = RoundedCornerShape(26.dp), color = RockAccent.copy(alpha = .12f), border = BorderStroke(1.dp, RockAccent.copy(alpha = .32f))) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Code, contentDescription = "GitHub Rock", tint = RockAccent, modifier = Modifier.size(36.dp)) }
        }
        Text("Welcome to GitHub Rock", color = RockInk, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Your developer workspace, redesigned.", color = RockMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeCard(configured: Boolean, loading: Boolean, onLogin: () -> Unit, onGuest: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = RockSurface.copy(alpha = .94f), border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Sign in to continue", color = RockInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Connect your GitHub account without entering your GitHub password inside the app.", color = RockMuted, fontSize = 14.sp)
            SecurityRow()
            Button(onClick = onLogin, enabled = configured && !loading, modifier = Modifier.fillMaxWidth().height(62.dp).semantics { contentDescription = "Sign in to GitHub" }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = RockAccent, contentColor = Color(0xFF0B1116))) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF0B1116)) else Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(10.dp)); Text(if (loading) "Preparing secure sign-in…" else "Continue with GitHub", fontWeight = FontWeight.Black)
            }
            Surface(modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onGuest).semantics { contentDescription = "Continue with public repositories" }, shape = RoundedCornerShape(17.dp), color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))) {
                Box(contentAlignment = Alignment.Center) { Text("Explore public repositories", color = RockMuted, fontWeight = FontWeight.SemiBold) }
            }
            if (!configured) Text("GitHub sign-in is not configured in this build. Public repository access remains available.", color = Color(0xFFFFA8A8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SecurityRow() {
    Surface(shape = RoundedCornerShape(18.dp), color = RockSurfaceRaised, border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = RockAccent.copy(alpha = .10f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Security, contentDescription = null, tint = RockAccent, modifier = Modifier.size(22.dp)) } }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Secure browser authorization", color = RockInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("OAuth / PKCE · no GitHub password stored", color = RockMuted, fontSize = 12.sp)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RockAccent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AuthorizationCard(code: DeviceCodeResponse, loading: Boolean, status: String?, onCheckAuthorization: () -> Unit, onOpenGitHubUrl: (String) -> Unit, context: Context, onRestart: () -> Unit, onGuest: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = RockSurface.copy(alpha = .96f), border = BorderStroke(1.dp, RockAccent.copy(alpha = .18f))) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Authorize GitHub Rock", color = RockInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(status ?: "Complete the one-time authorization in GitHub.", color = RockMuted, fontSize = 14.sp)
            Surface(shape = RoundedCornerShape(22.dp), color = Color.Black.copy(alpha = .24f), border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ONE-TIME CODE", color = RockMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(code.userCode, color = RockInk, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    TextButton(onClick = { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("GitHub verification code", code.userCode)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Copy code")
                    }
                }
            }
            Button(onClick = { onOpenGitHubUrl(code.verificationUri) }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(19.dp), colors = ButtonDefaults.buttonColors(containerColor = RockAccent, contentColor = Color(0xFF0B1116))) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null); Spacer(Modifier.width(9.dp)); Text("Open GitHub authorization", fontWeight = FontWeight.Black)
            }
            Button(onClick = onCheckAuthorization, enabled = !loading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = RockSurfaceRaised, contentColor = RockInk)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = RockAccent) else Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp)); Text(if (loading) "Checking authorization…" else "I've authorized GitHub", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRestart, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Start over", color = RockMuted) }
            TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Continue without an account", color = RockMuted) }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = RockSurface, border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = .22f))) {
        Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = Color(0xFFFF6B6B).copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A8A), modifier = Modifier.size(30.dp)) } }
            Text("Sign-in needs another try", color = RockInk, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(message, color = RockMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = RockAccent, contentColor = Color(0xFF0B1116))) { Text("Try again", fontWeight = FontWeight.Black) }
        }
    }
}
