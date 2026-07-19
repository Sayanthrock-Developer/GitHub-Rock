package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.RepositoryCreationForm
import com.sayanthrock.githubrock.core.model.RepositoryLicenseTemplate
import com.sayanthrock.githubrock.core.model.RepositoryOwnerOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepositorySheet(
    onDismiss: () -> Unit,
    onCreated: (GitHubRepositoryModel) -> Unit,
    viewModel: CreateRepositoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf(RepositoryCreationForm()) }

    LaunchedEffect(Unit) { viewModel.loadOptions() }
    LaunchedEffect(state.owners) {
        if (form.ownerLogin.isBlank() && state.owners.isNotEmpty()) {
            form = form.copy(ownerLogin = state.owners.first().login)
        }
    }
    LaunchedEffect(state.createdRepository?.id, state.successWarning) {
        val repository = state.createdRepository
        if (repository != null && state.successWarning == null) {
            viewModel.consumeCreatedRepository()
            onCreated(repository)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        CreateRepositoryFormContent(
            state = state,
            form = form,
            onFormChange = {
                form = it
                viewModel.dismissError()
            },
            onCreate = { viewModel.create(form) },
            onCancel = onDismiss,
            onOpenCreated = { repository ->
                viewModel.consumeCreatedRepository()
                onCreated(repository)
            }
        )
    }
}

@Composable
internal fun CreateRepositoryFormContent(
    state: CreateRepositoryState,
    form: RepositoryCreationForm,
    onFormChange: (RepositoryCreationForm) -> Unit,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
    onOpenCreated: (GitHubRepositoryModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Create repository",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create and configure a GitHub repository without leaving the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close repository creation")
            }
        }

        when {
            state.loadingOptions -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.width(22.dp))
                Text("Loading owner accounts and GitHub templates…")
            }
            state.owners.isEmpty() -> StatusSurface(
                text = state.error ?: "No repository owner account is available. Sign in to GitHub and retry.",
                isError = true
            )
        }

        if (state.optionWarning != null) {
            StatusSurface(text = state.optionWarning, isError = false)
        }

        OwnerDropdown(
            owners = state.owners,
            selectedLogin = form.ownerLogin,
            enabled = !state.loadingOptions && !state.submitting,
            onSelected = { owner -> onFormChange(form.copy(ownerLogin = owner.login)) }
        )

        OutlinedTextField(
            value = form.name,
            onValueChange = { onFormChange(form.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            singleLine = true,
            label = { Text("Repository name") },
            supportingText = { Text("Letters, numbers, dots, hyphens, and underscores") }
        )

        OutlinedTextField(
            value = form.description,
            onValueChange = { onFormChange(form.copy(description = it.take(350))) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            minLines = 2,
            maxLines = 4,
            label = { Text("Description") },
            supportingText = { Text("${form.description.length} / 350") }
        )

        SettingSwitchRow(
            title = "Private repository",
            subtitle = if (form.privateRepository) {
                "Only people you grant access can view this repository."
            } else {
                "Anyone can view this repository."
            },
            checked = form.privateRepository,
            enabled = !state.submitting,
            icon = if (form.privateRepository) Icons.Default.Lock else Icons.Default.Public,
            onCheckedChange = { onFormChange(form.copy(privateRepository = it)) }
        )

        HorizontalDivider()

        SettingSwitchRow(
            title = "Initialize README",
            subtitle = "Create the first commit so templates and a custom default branch can be applied.",
            checked = form.initializeReadme,
            enabled = !state.submitting,
            icon = Icons.Default.Add,
            onCheckedChange = { initialize ->
                onFormChange(
                    form.copy(
                        initializeReadme = initialize,
                        gitignoreTemplate = if (initialize) form.gitignoreTemplate else null,
                        licenseTemplate = if (initialize) form.licenseTemplate else null
                    )
                )
            }
        )

        TemplateDropdown(
            label = ".gitignore template",
            selectedKey = form.gitignoreTemplate,
            options = state.gitignoreTemplates.map { it to it },
            enabled = form.initializeReadme && !state.submitting,
            onSelected = { onFormChange(form.copy(gitignoreTemplate = it)) }
        )

        TemplateDropdown(
            label = "License template",
            selectedKey = form.licenseTemplate,
            options = state.licenses.map { it.key to it.displayName() },
            enabled = form.initializeReadme && !state.submitting,
            onSelected = { onFormChange(form.copy(licenseTemplate = it)) }
        )

        OutlinedTextField(
            value = form.defaultBranch,
            onValueChange = { onFormChange(form.copy(defaultBranch = it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = form.initializeReadme && !state.submitting,
            singleLine = true,
            label = { Text("Default branch") },
            supportingText = {
                Text(
                    if (form.initializeReadme) {
                        "GitHub Rock renames the initialized branch after repository creation when needed."
                    } else {
                        "The first pushed branch becomes the default branch."
                    }
                )
            }
        )

        if (state.error != null && state.owners.isNotEmpty()) {
            StatusSurface(text = state.error, isError = true)
        }

        val createdRepository = state.createdRepository
        if (createdRepository != null && state.successWarning != null) {
            StatusSurface(text = state.successWarning, isError = false)
            Button(
                onClick = { onOpenCreated(createdRepository) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open ${createdRepository.fullName}")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !state.submitting
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onCreate,
                    modifier = Modifier.weight(1f),
                    enabled = !state.loadingOptions && state.owners.isNotEmpty() && !state.submitting
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Creating…")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OwnerDropdown(
    owners: List<RepositoryOwnerOption>,
    selectedLogin: String,
    enabled: Boolean,
    onSelected: (RepositoryOwnerOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = owners.firstOrNull { it.login == selectedLogin }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Owner", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && owners.isNotEmpty()
        ) {
            Text(
                text = selected?.login ?: "Select owner",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            owners.forEach { owner ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(owner.login)
                            Text(
                                owner.type.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(owner)
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateDropdown(
    label: String,
    selectedKey: String?,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedKey }?.second ?: "None"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(
                text = selectedLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    expanded = false
                    onSelected(null)
                }
            )
            options.forEach { (key, displayName) ->
                DropdownMenuItem(
                    text = { Text(displayName) },
                    onClick = {
                        expanded = false
                        onSelected(key)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun StatusSurface(text: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun RepositoryLicenseTemplate.displayName(): String =
    if (spdxId.isNullOrBlank() || spdxId == key) name else "$name ($spdxId)"
