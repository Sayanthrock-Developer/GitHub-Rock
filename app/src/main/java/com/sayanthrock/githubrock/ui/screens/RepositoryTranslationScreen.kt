package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.ui.components.GlassCard
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RepositoryTranslationScreen(
    repository: GitHubRepositoryModel,
    onBack: () -> Unit,
    viewModel: RepositoryTranslationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showOriginal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository.id) { viewModel.load(repository) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Translate", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Translate this repository's README without changing the original GitHub content.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Target language", style = MaterialTheme.typography.titleMedium)
                        if (state.languagesLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.languages.take(40).chunked(2).forEach { row ->
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { language ->
                                        FilterChip(
                                            selected = language.code == state.targetLanguage,
                                            onClick = { scope.launch { viewModel.selectLanguage(language.code) } },
                                            label = { Text(language.name, maxLines = 1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "Showing languages provided by the configured LibreTranslate service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            state.error?.let { message ->
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(message, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { viewModel.translate() }) { Text("Retry") }
                        }
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("README", style = MaterialTheme.typography.titleMedium)
                            FilterChip(
                                selected = showOriginal,
                                onClick = { showOriginal = !showOriginal },
                                label = { Text(if (showOriginal) "Original" else "Translated") }
                            )
                        }
                        when {
                            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            state.source == null -> Text("README is not available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            state.translating -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            else -> Text(
                                if (showOriginal || state.translated == null) state.source.orEmpty() else state.translated.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (state.source != null && !state.translating && !showOriginal) {
                            Button(onClick = { viewModel.translate() }, enabled = state.targetLanguage.isNotBlank()) {
                                Text(if (state.translated == null) "Translate README" else "Translate again")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to repository") }
            }
        }
    }
}
