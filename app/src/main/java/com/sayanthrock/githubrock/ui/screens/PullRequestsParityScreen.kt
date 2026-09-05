package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.PullRequestSummary
import com.sayanthrock.githubrock.ui.components.AppLoadingIndicator
import com.sayanthrock.githubrock.ui.components.GlassCard

/**
 * Native pull-request workspace for the Android parity program.
 * Keeps review work inside GitHub Rock instead of handing normal PR browsing
 * to the GitHub website.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestsParityScreen(
    onBack: () -> Unit,
    viewModel: RepositoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedPull by remember { mutableStateOf<PullRequestSummary?>(null) }
    var reviewDraft by remember { mutableStateOf("") }
    var reviewAction by remember { mutableStateOf("COMMENT") }
    var showMergeConfirmation by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pull Requests", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }

        if (state.loading) AppLoadingIndicator(Modifier.fillMaxWidth(), compact = true)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (!state.loading && state.pulls.isEmpty()) {
            Text(
                "No pull requests found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.pulls, key = { it.id }) { pull ->
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("#${pull.number} ${pull.title}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append(if (pull.merged == true) "Merged" else pull.state.replaceFirstChar { it.uppercase() })
                                append(" • ")
                                if (pull.draft) append("Draft • ")
                                append(pull.user.login)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                selectedPull = pull
                                reviewDraft = ""
                                reviewAction = "COMMENT"
                                viewModel.loadPullReviews(pull.number)
                            }) { Text("Review") }
                            if (pull.state == "open" && pull.draft != true) {
                                OutlinedButton(onClick = {
                                    selectedPull = pull
                                    showMergeConfirmation = true
                                }) { Text("Merge") }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPull?.let { pull ->
        if (!showMergeConfirmation) {
            ModalBottomSheet(onDismissRequest = { selectedPull = null }) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("#${pull.number} ${pull.title}", style = MaterialTheme.typography.titleLarge)
                    Text("Reviews", style = MaterialTheme.typography.titleMedium)
                    if (state.pullReviews.isEmpty() && !state.loading) {
                        Text("No submitted reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.pullReviews.forEach { review ->
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${review.user.login} • ${review.state}")
                                review.body?.takeIf(String::isNotBlank)?.let { body -> Text(body) }
                            }
                        }
                    }
                    Text("Submit review", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = reviewAction == "COMMENT", onClick = { reviewAction = "COMMENT" }, label = { Text("Comment") })
                        FilterChip(selected = reviewAction == "APPROVE", onClick = { reviewAction = "APPROVE" }, label = { Text("Approve") })
                        FilterChip(selected = reviewAction == "REQUEST_CHANGES", onClick = { reviewAction = "REQUEST_CHANGES" }, label = { Text("Changes") })
                    }
                    OutlinedTextField(
                        value = reviewDraft,
                        onValueChange = { reviewDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Review comment") }
                    )
                    Button(
                        onClick = {
                            viewModel.submitPullReview(pull.number, reviewAction, reviewDraft)
                            reviewDraft = ""
                        },
                        enabled = !state.loading && (reviewAction == "APPROVE" || reviewDraft.isNotBlank()),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Submit review") }
                }
            }
        }
    }

    if (showMergeConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMergeConfirmation = false },
            title = { Text("Merge pull request?") },
            text = { Text("GitHub will enforce repository permissions, required checks, reviews, and branch protection. GitHub Rock will not bypass those rules.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPull?.let { viewModel.mergePullRequest(it.number) }
                    showMergeConfirmation = false
                    selectedPull = null
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { showMergeConfirmation = false }) { Text("Cancel") } }
        )
    }
}
