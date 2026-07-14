package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.core.navigation.GITHUB_SIGN_UP_URL
import com.sayanthrock.githubrock.ui.DeviceAuthState
import kotlinx.coroutines.delay

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

    LaunchedEffect(code?.deviceCode, onOpenGitHubUrl) {
        val verificationUri = code?.verificationUri
        if (verificationUri != null && !hasOpenedVerificationUri) {
            hasOpenedVerificationUri = true
            onOpenGitHubUrl(verificationUri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = .08f)
                    )
                )
            )
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        AmbientGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-100).dp),
            color = MaterialTheme.colorScheme.primary
        )
        AmbientGlow(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 100.dp),
            color = MaterialTheme.colorScheme.secondary
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AuthBrandHeader()

            if (code == null) {
                AccountAccessPanel(
                    configured = configured,
                    loading = loading,
                    showAccountSetup = showAccountSetup,
                    onShowAccountSetup = {
                        showAccountSetup = true
                        onOpenGitHubUrl(GITHUB_SIGN_UP_URL)
                    },
                    onHideAccountSetup = { showAccountSetup = false },
                    onLogin = onLogin,
                    onOpenSignup = { onOpenGitHubUrl(GITHUB_SIGN_UP_URL) },
                    onGuest = onGuest,
                    onDemo = onDemo
                )
            } else {
                DeviceCodeExperience(
                    auth = auth,
                    checking = loading,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("GitHub verification code", code.userCode)
                        )
                    },
                    onOpen = { onOpenGitHubUrl(code.verificationUri) },
                    onCheck = onCheckAuthorization,
                    onRestart = onLogin,
                    onGuest = onGuest
                )
            }
        }
    }
}

@Composable
private fun AmbientGlow(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .size(280.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = .24f), Color.Transparent)
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun AuthBrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .38f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "GitHub Rock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "Secure developer access",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SecureStatusPill(label = "Protected")
    }
}

@Composable
private fun AccountAccessPanel(
    configured: Boolean,
    loading: Boolean,
    showAccountSetup: Boolean,
    onShowAccountSetup: () -> Unit,
    onHideAccountSetup: () -> Unit,
    onLogin: () -> Unit,
    onOpenSignup: () -> Unit,
    onGuest: () -> Unit,
    onDemo: () -> Unit
) {
    AuthGlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    if (showAccountSetup) "Create, then connect" else "Connect your GitHub account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (showAccountSetup) {
                        "GitHub signup opens securely in your browser. After creating the account, return here and connect it with Device Flow."
                    } else {
                        "Authorize GitHub Rock in your trusted browser. Your GitHub password is never entered or stored inside this app."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SecuritySummaryCard()

            if (showAccountSetup) {
                Button(
                    onClick = onLogin,
                    enabled = configured && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics { contentDescription = "Connect new GitHub account" },
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (loading) "Preparing connection…" else "I created an account — connect",
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onOpenSignup,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open GitHub signup again")
                }
                TextButton(
                    onClick = onHideAccountSetup,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("I already have an account")
                }
            } else {
                Button(
                    onClick = onLogin,
                    enabled = configured && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .semantics { contentDescription = "Login with GitHub" },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .16f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (loading) "Connecting…" else "Connect with GitHub",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .4.sp
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
                OutlinedButton(
                    onClick = onShowAccountSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .semantics { contentDescription = "Create GitHub account" },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create a GitHub account")
                }
            }

            if (!configured) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = .10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .28f))
                ) {
                    Text(
                        "This build is missing its public GitHub App client ID, so account connection is unavailable. Guest and demo modes still work.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f))

            OutlinedButton(
                onClick = onGuest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Continue with public repositories")
            }
            TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Explore isolated demo mode")
            }
        }
    }
}

@Composable
private fun SecuritySummaryCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("GitHub Device Flow", fontWeight = FontWeight.Bold)
                Text(
                    "Browser authorization · no password sharing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SecureStatusPill(label = "Secure")
        }
    }
}

@Composable
private fun SecureStatusPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = .14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeviceCodeExperience(
    auth: DeviceAuthState,
    checking: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
    onRestart: () -> Unit,
    onGuest: () -> Unit
) {
    val code = requireNotNull(auth.code)
    val expireAtEpochSeconds by rememberSaveable(code.deviceCode) {
        mutableStateOf(currentEpochSeconds() + code.expiresIn.coerceAtLeast(0).toLong())
    }
    var remainingSeconds by remember(code.deviceCode, expireAtEpochSeconds) {
        mutableIntStateOf(
            (expireAtEpochSeconds - currentEpochSeconds())
                .coerceAtLeast(0L)
                .toInt()
        )
    }

    LaunchedEffect(code.deviceCode, expireAtEpochSeconds) {
        while (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds = (expireAtEpochSeconds - currentEpochSeconds())
                .coerceAtLeast(0L)
                .toInt()
        }
    }

    val expiryProgress = if (code.expiresIn > 0) {
        (remainingSeconds.toFloat() / code.expiresIn.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isExpired = remainingSeconds <= 0

    AuthGlassPanel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Authorize GitHub Rock",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Use this one-time code in GitHub. It is linked only to this secure sign-in request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DeviceCodeField(code = code.userCode, onCopy = onCopy)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { expiryProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    formatDuration(remainingSeconds),
                    color = if (isExpired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    AuthGlassPanel(contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Secure authentication via GitHub", fontWeight = FontWeight.Bold)
                Text(
                    "The code is completed only on GitHub’s official website.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SecureStatusPill(label = "Secure")
        }
    }

    Button(
        onClick = onOpen,
        enabled = !isExpired,
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .16f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            "OPEN GITHUB",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(28.dp))
    }

    AuthGlassPanel {
        Column(
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Text(
                "After GitHub says you’re all set, return with Android Back or the app switcher. GitHub Rock checks automatically; the button below is a backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isExpired) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = .10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .28f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Code expired — request a new one",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onRestart, enabled = !checking) {
                            Text("Get a new verification code")
                        }
                    }
                }
            }

            Button(
                onClick = onCheck,
                enabled = !checking && !isExpired,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (checking) "Checking GitHub…" else "I’ve authorized — check now")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!isExpired) {
                    TextButton(onClick = onRestart, enabled = !checking) {
                        Text("Get a new verification code")
                    }
                }
                TextButton(onClick = onGuest, enabled = !checking) {
                    Text("Use guest mode instead")
                }
            }

            val statusText = auth.error ?: auth.status.orEmpty()
            if (statusText.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (auth.error != null) {
                        MaterialTheme.colorScheme.error.copy(alpha = .10f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = .08f)
                    }
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(13.dp),
                        color = if (auth.error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (auth.error == null && checking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DeviceCodeField(code: String, onCopy: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = .62f),
                shape = shape
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = code,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
        Surface(
            modifier = Modifier.width(64.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
        ) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.semantics {
                    contentDescription = "Copy GitHub verification code"
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthGlassPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .68f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = .56f),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

private fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}

private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000L
