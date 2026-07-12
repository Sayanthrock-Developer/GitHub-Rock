package com.sayanthrock.githubrock.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build as AndroidBuild
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.WorkflowArtifact
import com.sayanthrock.githubrock.core.model.WorkflowDisplayState
import com.sayanthrock.githubrock.core.model.WorkflowRun
import com.sayanthrock.githubrock.core.model.displayState
import com.sayanthrock.githubrock.core.util.AndroidArtifactType
import com.sayanthrock.githubrock.core.util.AndroidWorkflowGenerator
import com.sayanthrock.githubrock.core.util.BuildRunTracker
import com.sayanthrock.githubrock.ui.AppMode
import com.sayanthrock.githubrock.ui.components.GlassCard
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BuildsScreen(
    mode: AppMode,
    repositories: List<GitHubRepositoryModel>,
    runs: List<WorkflowRun>,
    onSelectRepository: (GitHubRepositoryModel) -> Unit,
    initialRepository: GitHubRepositoryModel? = null,
    initialRunId: Long? = null,
    viewModel: BuildsViewModel = hiltViewModel()
) {
    val actionState by viewModel.state.collectAsStateWithLifecycle()
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    var selectedRepository by remember(repositories, initialRepository?.id) {
        val requested = initialRepository?.let { initial ->
            repositories.firstOrNull { it.id == initial.id } ?: initial
        }
        mutableStateOf(requested ?: repositories.firstOrNull())
    }
    val requestedRunId = initialRunId.takeIf { selectedRepository?.id == initialRepository?.id }
    LaunchedEffect(mode, selectedRepository?.id, requestedRunId) {
        if (mode == AppMode.Connected) {
            selectedRepository?.let { viewModel.loadAndroidBuild(it, requestedRunId) }
        } else {
            viewModel.resetBuild()
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Android Builds", style = MaterialTheme.typography.headlineSmall) }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CloudQueue, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Cloud builds with GitHub Actions", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Select a repository to inspect its workflows and runs. GitHub Rock never compiles large projects on your phone or stores signing secrets.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mode == AppMode.Guest) Text("Connect GitHub to dispatch workflows.", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text("Choose a repository", style = MaterialTheme.typography.titleMedium) }
        items(repositories.take(8), key = { it.id }) { repo ->
            OutlinedButton(
                onClick = { selectedRepository = repo },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selectedRepository?.id == repo.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text(if (selectedRepository?.id == repo.id) "Selected • ${repo.fullName}" else repo.fullName)
            }
        }
        selectedRepository?.let { repo ->
            item { TextButton(onClick = { onSelectRepository(repo) }) { Text("Open ${repo.fullName}") } }
        }
        actionState.message?.let { message ->
            item { GlassCard { Text(message, color = MaterialTheme.colorScheme.tertiary) } }
        }
        actionState.error?.let { error ->
            item { GlassCard { Text(error, color = MaterialTheme.colorScheme.error) } }
        }
        item {
            WorkflowPreviewCard(
                mode = mode,
                repository = selectedRepository,
                actionState = actionState,
                onCreatePullRequest = { repo, branch, yaml, artifact ->
                    viewModel.createWorkflowPullRequest(repo, branch, yaml, artifact)
                }
            )
        }
        item {
            BuildExecutionCard(
                mode = mode,
                repository = selectedRepository,
                actionState = actionState,
                onRefresh = { selectedRepository?.let { viewModel.loadAndroidBuild(it) } },
                onDispatch = { ref -> selectedRepository?.let { viewModel.dispatchAndroidBuild(it, ref) } },
                onDownload = { artifact ->
                    selectedRepository?.let { repo ->
                        downloadsViewModel.enqueue(
                            artifact.archiveDownloadUrl,
                            "${repo.name}-${artifact.name}-${artifact.id}.zip"
                        )
                    }
                }
            )
        }
        item { Text("Recent runs", style = MaterialTheme.typography.titleMedium) }
        if (runs.isEmpty()) item {
            GlassCard { Text("No workflow runs loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(runs, key = { it.id }) { run ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(run.displayTitle.ifBlank { run.name ?: "Workflow run" }, fontWeight = FontWeight.SemiBold)
                    Text("${run.event} • ${run.headBranch.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.displayState().name, color = when (run.displayState().name) {
                        "Success" -> MaterialTheme.colorScheme.tertiary
                        "Failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    })
                }
            }
        }
    }
}

@Composable
private fun WorkflowPreviewCard(
    mode: AppMode,
    repository: GitHubRepositoryModel?,
    actionState: BuildsActionState,
    onCreatePullRequest: (GitHubRepositoryModel, String, String, AndroidArtifactType) -> Unit
) {
    var artifact by remember { mutableStateOf(AndroidArtifactType.DebugApk) }
    var module by remember { mutableStateOf("app") }
    var featureBranch by remember(repository?.id) { mutableStateOf("github-rock/android-build-${System.currentTimeMillis() / 1000}") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val yamlResult = remember(module, artifact) { runCatching { AndroidWorkflowGenerator.generate(module, artifact) } }
    val yaml = yamlResult.getOrDefault("")
    val workflowExists = repository?.id == actionState.selectedRepositoryId && actionState.workflow != null
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Workflow preview", style = MaterialTheme.typography.titleLarge)
            Text("Review the exact YAML before it is committed to a new branch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Repository: ${repository?.fullName ?: "Select a repository"}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = module, onValueChange = { module = it }, label = { Text("Android application module") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = featureBranch, onValueChange = { featureBranch = it }, label = { Text("Review branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AndroidArtifactType.entries.forEach { option ->
                    FilterChip(selected = artifact == option, onClick = { artifact = option }, label = { Text(option.name) })
                }
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background.copy(alpha = .75f)) {
                Text(yamlResult.exceptionOrNull()?.message ?: yaml, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, maxLines = 12, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(yaml)) }, Modifier.fillMaxWidth(), enabled = yaml.isNotBlank()) { Text("Copy workflow YAML") }
            Button(
                onClick = { repository?.let { onCreatePullRequest(it, featureBranch, yaml, artifact) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = mode == AppMode.Connected &&
                    repository != null &&
                    yaml.isNotBlank() &&
                    !actionState.loading &&
                    !workflowExists
            ) {
                Text(
                    when {
                        actionState.creatingPullRequest -> "Creating pull request…"
                        workflowExists -> "Workflow already exists"
                        actionState.loading -> "Checking workflow…"
                        else -> "Create branch and pull request"
                    }
                )
            }
            if (workflowExists) {
                Text("Use the run card below for the active workflow.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mode != AppMode.Connected) Text("Connect GitHub to commit this workflow.", color = MaterialTheme.colorScheme.primary)
            actionState.pullRequestUrl?.let { url ->
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Text("Open pull request") }
            }
        }
    }
}

@Composable
private fun BuildExecutionCard(
    mode: AppMode,
    repository: GitHubRepositoryModel?,
    actionState: BuildsActionState,
    onRefresh: () -> Unit,
    onDispatch: (String) -> Unit,
    onDownload: (WorkflowArtifact) -> Unit
) {
    var ref by remember(repository?.id) { mutableStateOf(repository?.defaultBranch ?: "main") }
    val refIsSafe = remember(ref) { BuildRunTracker.isSafeRef(ref) }
    val context = LocalContext.current
    val notificationManager = remember(context) { NotificationManagerCompat.from(context) }
    val hasNotificationPermission = AndroidBuild.VERSION.SDK_INT < AndroidBuild.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var notificationsAllowed by remember(context) {
        mutableStateOf(hasNotificationPermission && notificationManager.areNotificationsEnabled())
    }
    var pendingDispatchRef by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && notificationManager.areNotificationsEnabled()
        pendingDispatchRef?.let(onDispatch)
        pendingDispatchRef = null
    }
    val dispatchWithNotificationPermission: (String) -> Unit = { selectedRef ->
        if (AndroidBuild.VERSION.SDK_INT >= AndroidBuild.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDispatchRef = selectedRef
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsAllowed = notificationManager.areNotificationsEnabled()
            onDispatch(selectedRef)
        }
    }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Run merged workflow", style = MaterialTheme.typography.titleLarge)
            Text(
                "GitHub Rock detects the reviewed workflow, dispatches it, follows the exact new run, and exposes verified artifact downloads.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                mode != AppMode.Connected -> Text("Connect GitHub to start and track a build.", color = MaterialTheme.colorScheme.primary)
                repository == null -> Text("Select a repository first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                actionState.loading && actionState.workflow == null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Detecting Android build workflow…")
                    }
                }
                actionState.workflow == null -> {
                    Text("No merged .github/workflows/android-build.yml is active yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh detection") }
                }
                else -> {
                    val workflow = requireNotNull(actionState.workflow)
                    Text("${workflow.name} • ${workflow.state}", fontWeight = FontWeight.SemiBold)
                    Text(workflow.path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = ref,
                        onValueChange = { ref = it },
                        label = { Text("Branch or tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { dispatchWithNotificationPermission(ref) },
                            enabled = refIsSafe && !actionState.loading && !actionState.tracking,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (actionState.tracking) "Tracking build…" else "Run Android build") }
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !actionState.loading && !actionState.tracking
                        ) { Text("Refresh") }
                    }
                    if (!refIsSafe) {
                        Text("Use a valid branch or tag.", color = MaterialTheme.colorScheme.error)
                    }
                    if (!notificationsAllowed) {
                        Text(
                            "Background tracking will continue, but Android notifications are currently disabled.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (actionState.tracking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            actionState.run?.let { run ->
                val displayState = run.displayState()
                val color = when (displayState) {
                    WorkflowDisplayState.Success -> MaterialTheme.colorScheme.tertiary
                    WorkflowDisplayState.Failed -> MaterialTheme.colorScheme.error
                    WorkflowDisplayState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                }
                HorizontalDivider()
                Text(run.displayTitle.ifBlank { run.name ?: "Android build" }, fontWeight = FontWeight.SemiBold)
                Text("${displayState.name} • ${run.headBranch.orEmpty()} • run ${run.id}", color = color)
                if (run.htmlUrl.isNotBlank()) {
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl))) }) {
                        Text("Open run on GitHub")
                    }
                }
            }
            actionState.jobs.forEach { job ->
                val completedSteps = job.steps.count { it.status == "completed" }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(job.name, fontWeight = FontWeight.Medium)
                        Text(
                            "${job.status} • ${job.conclusion ?: "pending"} • $completedSteps/${job.steps.size} steps",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            actionState.artifacts.forEach { artifact ->
                OutlinedButton(
                    onClick = { onDownload(artifact) },
                    enabled = !artifact.expired,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (artifact.expired) "${artifact.name} expired"
                        else "Queue ${artifact.name} in Downloads"
                    )
                }
            }
        }
    }
}
