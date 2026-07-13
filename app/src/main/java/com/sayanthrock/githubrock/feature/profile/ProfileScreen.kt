package com.sayanthrock.githubrock.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.ui.theme.GlassCard

/**
 * Displays the user's profile, account status, API rate limit, and security actions.
 *
 * @param demoMode Whether the screen is displaying demo mode data.
 * @param guestMode Whether the screen is displaying guest mode data.
 * @param onLogout Called when the user selects the logout or exit action.
 * @param viewModel Provides profile state and data-loading operations.
 */
@Composable
fun ProfileScreen(
    demoMode: Boolean,
    guestMode: Boolean,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(demoMode, guestMode) { viewModel.load(demoMode, guestMode) }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                when {
                    demoMode -> "Demo mode"
                    guestMode -> "Guest mode"
                    else -> "Connected GitHub account"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.loading) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        }
        state.user?.let { user ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (user.avatarUrl != null) {
                            AsyncImage(model = user.avatarUrl, contentDescription = null)
                        } else {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.padding(8.dp))
                        Column {
                            Text(user.name ?: user.login, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("@${user.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            user.bio?.let { Text(it) }
                        }
                    }
                }
            }
        }
        state.rateLimit?.let { rate ->
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("GitHub API rate limit", fontWeight = FontWeight.SemiBold)
                        Text("${rate.remaining} of ${rate.limit} requests remaining")
                        Text("Used: ${rate.used}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        state.message?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { viewModel.load(demoMode, guestMode) }) { Text("Retry") }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Security", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tokens are stored using Android Keystore-backed encryption. Sensitive headers are redacted from network logs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Logout, null)
                        Text(if (demoMode || guestMode) " Exit mode" else " Logout and delete token")
                    }
                }
            }
        }
    }
}
