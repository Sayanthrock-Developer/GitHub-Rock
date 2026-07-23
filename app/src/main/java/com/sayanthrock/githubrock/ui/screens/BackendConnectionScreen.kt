package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sayanthrock.githubrock.data.backend.BackendConnectionSnapshot
import com.sayanthrock.githubrock.data.backend.BackendGateway
import com.sayanthrock.githubrock.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackendConnectionUiState(
    val endpoint: String = "",
    val snapshot: BackendConnectionSnapshot? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BackendConnectionViewModel @Inject constructor(
    private val gateway: BackendGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(
        BackendConnectionUiState(endpoint = gateway.configuredEndpoint.orEmpty())
    )
    val state: StateFlow<BackendConnectionUiState> = _state.asStateFlow()

    init {
        if (gateway.isConfigured) refresh()
    }

    fun connect(rawEndpoint: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, endpoint = rawEndpoint.trim()) }
            try {
                val snapshot = gateway.saveAndCheck(rawEndpoint)
                _state.value = BackendConnectionUiState(
                    endpoint = snapshot.endpoint,
                    snapshot = snapshot,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        snapshot = null,
                        error = problem.message?.takeIf(String::isNotBlank)
                            ?: "The backend could not be reached.",
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = gateway.check()
                _state.value = BackendConnectionUiState(
                    endpoint = snapshot.endpoint,
                    snapshot = snapshot,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        snapshot = null,
                        error = problem.message?.takeIf(String::isNotBlank)
                            ?: "The backend could not be reached.",
                    )
                }
            }
        }
    }

    fun disconnect() {
        gateway.disconnect()
        _state.value = BackendConnectionUiState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendConnectionScreen(
    onBack: () -> Unit,
    viewModel: BackendConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var endpointText by remember { mutableStateOf(state.endpoint) }

    LaunchedEffect(state.endpoint) {
        if (state.endpoint.isNotBlank()) endpointText = state.endpoint
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backend connection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BackendStatusCard(state)
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Server URL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "Enter the deployed HTTPS address for Sayanthrock-Developer/GitHub-Rock-Backend.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = endpointText,
                            onValueChange = { endpointText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("https://api.example.com") },
                            enabled = !state.loading,
                        )
                        Button(
                            onClick = { viewModel.connect(endpointText) },
                            enabled = endpointText.isNotBlank() && !state.loading,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.Dns, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Save and test connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            state.snapshot?.let { snapshot ->
                item {
                    BackendRuntimeCard(snapshot)
                }
                item {
                    BackendFeatureCard(snapshot)
                }
            }

            item {
                GlassCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Safe fallback", fontWeight = FontWeight.Bold)
                            Text(
                                "GitHub Rock uses the backend for Device Flow and token refresh when available. Direct GitHub API access remains enabled so repository work continues during backend maintenance.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (state.endpoint.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = viewModel::refresh,
                            enabled = !state.loading,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Retest")
                        }
                        TextButton(
                            onClick = viewModel::disconnect,
                            enabled = !state.loading,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Disconnect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendStatusCard(state: BackendConnectionUiState) {
    val connected = state.snapshot != null
    val accent = if (connected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = accent.copy(alpha = .08f),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (connected) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when {
                        state.loading -> "Checking backend…"
                        connected -> "Backend connected"
                        state.endpoint.isBlank() -> "Backend not configured"
                        else -> "Backend unavailable"
                    },
                    fontWeight = FontWeight.Black,
                    color = accent,
                )
                Text(
                    state.error ?: state.snapshot?.endpoint ?: "Add a deployed HTTPS endpoint to connect the app.",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BackendRuntimeCard(snapshot: BackendConnectionSnapshot) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Runtime health", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            BackendValueRow("Backend", "${snapshot.health.status} · ${snapshot.health.version}")
            BackendValueRow("PostgreSQL", snapshot.health.postgres)
            BackendValueRow("Redis", snapshot.health.redis)
            BackendValueRow("Meilisearch", snapshot.health.meilisearch)
            BackendValueRow("Checked", snapshot.health.timestamp)
        }
    }
}

@Composable
private fun BackendFeatureCard(snapshot: BackendConnectionSnapshot) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mobile contract", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            BackendValueRow("API", snapshot.config.apiVersion)
            BackendValueRow("Minimum app", snapshot.config.minSupportedAppVersion)
            BackendValueRow("Latest app", snapshot.config.latestAppVersion)
            BackendValueRow("Maintenance", if (snapshot.config.maintenanceMode) "Enabled" else "Off")
            HorizontalDivider()
            snapshot.config.features.toSortedMap().forEach { (feature, enabled) ->
                BackendValueRow(feature, if (enabled) "Available" else "Not available")
            }
        }
    }
}

@Composable
private fun BackendValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
