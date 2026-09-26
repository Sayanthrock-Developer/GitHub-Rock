package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

private data class PermissionItem(val title: String, val description: String)

private val permissionItems = listOf(
    PermissionItem("Repositories", "Access the repositories you authorize GitHub Rock to use."),
    PermissionItem("Issues & pull requests", "Work with issue and pull request data."),
    PermissionItem("Actions / Builds", "View and manage supported GitHub Actions workflows."),
    PermissionItem("Releases & downloads", "Access release assets and download information."),
    PermissionItem("Profile & notifications", "Read your profile and supported notification data.")
)

@Composable
fun LoginScreenV2(
    configured: Boolean,
    loading: Boolean,
    auth: DeviceAuthState,
    onLogin: () -> Unit,
    onDeviceCodeLogin: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    onCheckAuthorization: () -> Unit,
    onGuest: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val code = auth.code
    val authorizationUrl = auth.authorizationUrl
    var copiedDeviceCode by remember { mutableStateOf<String?>(null) }
    var remainingSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(code?.deviceCode) {
        val deviceCode = code?.deviceCode ?: return@LaunchedEffect
        if (copiedDeviceCode == deviceCode) return@LaunchedEffect
        val userCode = code.userCode.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub verification code", userCode))
        copiedDeviceCode = deviceCode
    }

    LaunchedEffect(code?.deviceCode, code?.expiresIn) {
        val current = code ?: run { remainingSeconds = 0L; return@LaunchedEffect }
        val deadline = System.currentTimeMillis() + current.expiresIn.coerceAtLeast(0) * 1_000L
        while (true) {
            val remaining = ((deadline - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L)
            remainingSeconds = remaining
            if (remaining == 0L) break
            delay(1_000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            RockLogoHeader()
            AnimatedVisibility(visible = authorizationUrl != null, enter = fadeIn(), exit = fadeOut()) {
                BrowserAuthorizationCard(
                    authorizationUrl = authorizationUrl,
                    status = auth.status,
                    onOpen = onOpenGitHubUrl,
                    onRestart = onLogin,
                    onCancel = onCancel
                )
            }
            AnimatedVisibility(visible = code != null, enter = fadeIn(), exit = fadeOut()) {
                if (code != null) {
                    AuthorizationCard(
                        code = code,
                        loading = loading,
                        status = auth.status,
                        onCheckAuthorization = onCheckAuthorization,
                        onOpenGitHubUrl = onOpenGitHubUrl,
                        context = context,
                        copied = copiedDeviceCode == code.deviceCode,
                        remainingSeconds = remainingSeconds,
                        onRestart = onLogin,
                        onGuest = onGuest,
                        onCancel = onCancel,
                        onReset = onReset
                    )
                }
            }
            AnimatedVisibility(visible = authorizationUrl == null && code == null, enter = fadeIn(), exit = fadeOut()) {
                when {
                    auth.error != null -> ErrorCard(auth.error, auth.resetRequired, onReset, onLogin)
                    else -> WelcomeCard(auth, configured, loading, onLogin, onDeviceCodeLogin, onGuest, onCancel)
                }
            }
            Text(
                "GitHub Rock · secure developer access",
                color = colors.onSurfaceVariant.copy(alpha = .72f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RockLogoHeader() {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(26.dp),
            color = colors.primary.copy(alpha = .12f),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = .32f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(RockIcon.Code.vector(), contentDescription = "GitHub Rock", tint = colors.primary, modifier = Modifier.size(36.dp))
            }
        }
        Text("GitHub Rock", color = colors.onBackground, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("Your GitHub. Your style.", color = colors.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeCard(auth: DeviceAuthState, configured: Boolean, loading: Boolean, onLogin: () -> Unit, onDeviceCodeLogin: () -> Unit, onGuest: () -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainer.copy(alpha = .94f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Application Login", color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Sign in to GitHub Rock from the app. GitHub Rock starts the secure Device Flow and never asks for your GitHub password.",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp
            )
            SecurityRow()
            Text("Supported login options", color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onDeviceCodeLogin,
                enabled = configured && !loading,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Continue with GitHub" },
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                else Icon(RockIcon.Security.vector(), contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text(if (loading) "Preparing sign-in…" else "Continue with GitHub", fontWeight = FontWeight.Black)
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .clickable(enabled = configured && !loading, onClick = onDeviceCodeLogin)
                    .semantics { contentDescription = "Sign in with a one-time code" },
                shape = RoundedCornerShape(19.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .65f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(RockIcon.Security.vector(), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(21.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("One-time code", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Enter the code on GitHub", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(RockIcon.ArrowForward.vector(), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .clickable(onClick = onGuest)
                    .semantics { contentDescription = "Continue with public repositories" },
                shape = RoundedCornerShape(17.dp),
                color = colors.surface.copy(alpha = .30f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .70f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Continue without an account", color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onLogin,
                enabled = auth.authorizationUrl != null && !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                )
            ) {
                Icon(RockIcon.OpenInBrowser.vector(), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Use browser login", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now", color = colors.onSurfaceVariant)
            }
            if (!configured) {
                Text(
                    "GitHub authorization is not configured in this build. Public repository access remains available.",
                    color = colors.error,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionList() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What authorization enables", color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        permissionItems.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(modifier = Modifier.padding(top = 2.dp).size(20.dp), shape = CircleShape, color = colors.primary.copy(alpha = .12f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(RockIcon.Check.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.title, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.description, color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SecurityRow() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = colors.primary.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(RockIcon.Security.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Secure application login", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Device Flow · no OAuth client secret in the app", color = colors.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(RockIcon.Check.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BrowserAuthorizationCard(
    authorizationUrl: String?,
    status: String?,
    onOpen: (String) -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val url = authorizationUrl ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainer.copy(alpha = .96f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = .20f))
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = colors.primary.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(RockIcon.Security.vector(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                }
            }
            Text("Waiting for GitHub", color = colors.onSurface, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                status ?: "Approve GitHub Rock in your browser. Return to the app when authorization is complete.",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                    Text("Authorization is waiting for your GitHub approval.", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = { onOpen(url) },
                modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Open GitHub authorization page again" },
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                Icon(RockIcon.OpenInBrowser.vector(), contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Open GitHub authorization", fontWeight = FontWeight.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = colors.onSurfaceVariant) }
                TextButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("Start over", color = colors.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AuthorizationCard(
    code: DeviceCodeResponse,
    loading: Boolean,
    status: String?,
    onCheckAuthorization: () -> Unit,
    onOpenGitHubUrl: (String) -> Unit,
    context: Context,
    copied: Boolean,
    remainingSeconds: Long,
    onRestart: () -> Unit,
    onGuest: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainer.copy(alpha = .96f),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = .18f))
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Authorize GitHub Rock", color = colors.onSurface, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(status ?: "Complete the one-time authorization in GitHub.", color = colors.onSurfaceVariant, fontSize = 14.sp)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surfaceContainerLowest.copy(alpha = .72f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ONE-TIME CODE", color = colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(code.userCode, color = colors.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Text(
                        if (remainingSeconds > 0L) "Expires in %02d:%02d".format(minutes, seconds) else "Code expired",
                        color = if (remainingSeconds > 0L) colors.onSurfaceVariant else colors.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("GitHub verification code", code.userCode))
                        },
                        modifier = Modifier.semantics { contentDescription = "Copy GitHub verification code" }
                    ) {
                        Icon(RockIcon.Copy.vector(), contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copied automatically" else "Copy code")
                    }
                }
            }
            Button(
                onClick = { onOpenGitHubUrl(code.verificationUri) },
                modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = "Open GitHub authorization page" },
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                Icon(RockIcon.OpenInBrowser.vector(), contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Open GitHub authorization", fontWeight = FontWeight.Black)
            }
            Button(
                onClick = onCheckAuthorization,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Check GitHub authorization" },
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                )
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = colors.primary)
                else Icon(RockIcon.Check.vector(), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Checking authorization…" else "I already authorized", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = colors.onSurfaceVariant) }
                TextButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("New code", color = colors.onSurfaceVariant) }
            }
            TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Continue without an account", color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, resetRequired: Boolean, onReset: () -> Unit, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = colors.surfaceContainer.copy(alpha = .96f),
        border = BorderStroke(1.dp, colors.error.copy(alpha = .22f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = colors.error.copy(alpha = .12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(RockIcon.Error.vector(), contentDescription = null, tint = colors.error, modifier = Modifier.size(30.dp))
                }
            }
            Text(if (resetRequired) "One-Time Password Reset Required" else "Authorization needs another try", color = colors.onSurface, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (resetRequired) "Your previous one-time password has expired. Generate a new code to continue." else message, color = colors.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(
                onClick = if (resetRequired) onReset else onRetry,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Retry GitHub authorization" },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
            ) {
                Text(if (resetRequired) "Generate New Code" else "Try again", fontWeight = FontWeight.Black)
            }
        }
    }
}
