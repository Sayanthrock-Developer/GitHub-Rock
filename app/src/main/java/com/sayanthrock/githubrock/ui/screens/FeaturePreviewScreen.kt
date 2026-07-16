package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.navigation.GITHUB_HOME_URL
import com.sayanthrock.githubrock.core.navigation.GitHubWebDestination
import com.sayanthrock.githubrock.core.navigation.GitHubWebSection
import com.sayanthrock.githubrock.core.navigation.githubWebSections
import com.sayanthrock.githubrock.ui.components.GlassCard

private enum class FeatureAvailability(val label: String) {
    Ready("Ready"),
    Connected("Connected"),
    Roadmap("Roadmap")
}

private enum class FeatureFilter(val label: String) {
    All("All"),
    Ready("Ready"),
    Connected("Connected"),
    Roadmap("Roadmap")
}

private data class GitHubFeature(
    val title: String,
    val description: String,
    val availability: FeatureAvailability
)

private data class FeatureCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val features: List<GitHubFeature>
)

private data class CustomerWorkflow(
    val title: String,
    val description: String,
    val tools: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePreviewScreen(
    login: String?,
    onOpenGitHubUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(FeatureFilter.All) }
    val categories = featureCategories
    val webSections = githubWebSections(login)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("All GitHub") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 44.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    FeaturePreviewHero(
                        webDestinationCount = webSections.sumOf { it.destinations.size },
                        onOpenGitHubUrl = onOpenGitHubUrl
                    )
                }
                item { GitHubWebAccessNote() }

                webSections.forEach { section ->
                    item(key = "web-${section.id}") {
                        GitHubWebSectionCard(
                            section = section,
                            onOpenGitHubUrl = onOpenGitHubUrl
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Native product coverage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "See which workflows are native today, require a connected account, or remain on the native roadmap.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item { FeatureFilterRow(selected = filter, onSelected = { filter = it }) }

                categories.forEach { category ->
                    val visible = category.features.filter { feature ->
                        when (filter) {
                            FeatureFilter.All -> true
                            FeatureFilter.Ready -> feature.availability == FeatureAvailability.Ready
                            FeatureFilter.Connected -> feature.availability == FeatureAvailability.Connected
                            FeatureFilter.Roadmap -> feature.availability == FeatureAvailability.Roadmap
                        }
                    }
                    if (visible.isNotEmpty()) {
                        item(key = category.title) {
                            FeatureCategoryCard(category = category, features = visible)
                        }
                    }
                }

                item { CustomerWorkflowSection() }
                item { HonestPreviewNote() }
            }
        }
    }
}

@Composable
private fun FeaturePreviewHero(
    webDestinationCount: Int,
    onOpenGitHubUrl: (String) -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .26f))
        ) {
            Text(
                text = "GITHUB ROCK · PRODUCT PREVIEW",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "Native tools plus the complete GitHub website",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Use GitHub Rock's native developer workflows, then open any remaining GitHub service securely on its official website without hunting through browser menus.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = { onOpenGitHubUrl(GITHUB_HOME_URL) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Open GitHub.com", fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Default.OpenInNew, contentDescription = null)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { PreviewMetric("5", "main tabs") }
            item { PreviewMetric("6", "workspaces") }
            item { PreviewMetric(webDestinationCount.toString(), "website tools") }
            item { PreviewMetric("3", "access modes") }
            item { PreviewMetric("Secure", "Device Flow") }
        }
    }
}

@Composable
private fun PreviewMetric(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GitHubWebAccessNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = .09f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .24f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Complete access, safely", fontWeight = FontWeight.Bold)
                Text(
                    "Native workflows stay inside GitHub Rock. Billing, passkeys, tokens, Marketplace purchases, Codespaces, and other website-only tools open in a trusted GitHub Custom Tab. GitHub Rock never sees credentials or payment details.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GitHubWebSectionCard(
    section: GitHubWebSection,
    onOpenGitHubUrl: (String) -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = .13f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        section.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    section.destinations.size.toString(),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            section.destinations.forEachIndexed { index, destination ->
                if (index > 0) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .64f))
                    )
                }
                GitHubWebDestinationRow(
                    destination = destination,
                    onOpenGitHubUrl = onOpenGitHubUrl
                )
            }
        }
    }
}

@Composable
private fun GitHubWebDestinationRow(
    destination: GitHubWebDestination,
    onOpenGitHubUrl: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGitHubUrl(destination.url) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                destination.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                destination.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .24f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GitHub web",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open ${destination.title} on GitHub",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureFilterRow(selected: FeatureFilter, onSelected: (FeatureFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Explore capabilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FeatureFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}

@Composable
private fun FeatureCategoryCard(category: FeatureCategory, features: List<GitHubFeature>) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        category.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "${features.size}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            features.forEachIndexed { index, feature ->
                if (index > 0) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .64f))
                    )
                }
                FeatureRow(feature)
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: GitHubFeature) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(availabilityColor(feature.availability), CircleShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                feature.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                feature.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        AvailabilityPill(feature.availability)
    }
}

@Composable
private fun AvailabilityPill(availability: FeatureAvailability) {
    val color = availabilityColor(availability)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = .12f),
        border = BorderStroke(1.dp, color.copy(alpha = .24f))
    ) {
        Text(
            availability.label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun availabilityColor(availability: FeatureAvailability): Color = when (availability) {
    FeatureAvailability.Ready -> MaterialTheme.colorScheme.tertiary
    FeatureAvailability.Connected -> MaterialTheme.colorScheme.primary
    FeatureAvailability.Roadmap -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun CustomerWorkflowSection() {
    val workflows = customerWorkflows
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("Customer workflows", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            "Designed to serve individual developers, open-source maintainers, teams, and Android release managers.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        workflows.forEach { workflow -> CustomerWorkflowCard(workflow) }
    }
}

@Composable
private fun CustomerWorkflowCard(workflow: CustomerWorkflow) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .38f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(workflow.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(workflow.title, fontWeight = FontWeight.Bold)
                Text(workflow.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(workflow.tools, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun HonestPreviewNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Clear feature status", fontWeight = FontWeight.Bold)
                Text(
                    "Ready features are available in the current app. Connected features require GitHub authorization and repository permissions. Roadmap items are preview concepts and are not presented as finished functionality.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private val featureCategories: List<FeatureCategory> = listOf(
    FeatureCategory(
        title = "Access & identity",
        subtitle = "Secure entry points for every customer mode",
        icon = Icons.Default.AccountCircle,
        features = listOf(
            GitHubFeature("GitHub Device Flow", "Password-free browser authorization with a one-time code.", FeatureAvailability.Ready),
            GitHubFeature("Connected profile", "Account identity, avatar, profile details, and API health.", FeatureAvailability.Connected),
            GitHubFeature("Guest browsing", "Explore public repositories without connecting an account.", FeatureAvailability.Ready),
            GitHubFeature("Isolated demo mode", "Preview the app safely with sample repositories and workflows.", FeatureAvailability.Ready),
            GitHubFeature("Organization switching", "Browse GitHub App installations and organization workspaces.", FeatureAvailability.Roadmap)
        )
    ),
    FeatureCategory(
        title = "Repositories & code",
        subtitle = "A mobile repository workspace with review-safe editing",
        icon = Icons.Default.Folder,
        features = listOf(
            GitHubFeature("Repository dashboard", "Search repositories and view language, topics, stars, forks, and issues.", FeatureAvailability.Ready),
            GitHubFeature("Files and folders", "Browse repository directories and open supported source files.", FeatureAvailability.Connected),
            GitHubFeature("Mobile code editor", "Edit text files with syntax highlighting and change previews.", FeatureAvailability.Connected),
            GitHubFeature("Create, move, and delete files", "Perform file operations through a review branch instead of overwriting the default branch.", FeatureAvailability.Connected),
            GitHubFeature("Star, unstar, and fork", "Manage repository relationships directly from the workspace.", FeatureAvailability.Connected),
            GitHubFeature("README and Markdown", "Render repository documentation in a readable mobile layout.", FeatureAvailability.Ready),
            GitHubFeature("Branches and commit history", "Dedicated branch browser, commit timeline, and comparison tools.", FeatureAvailability.Roadmap)
        )
    ),
    FeatureCategory(
        title = "Issues & pull requests",
        subtitle = "Collaboration tools for maintainers and teams",
        icon = Icons.Default.Description,
        features = listOf(
            GitHubFeature("Issue management", "Create issues, open discussions, add comments, and change issue state.", FeatureAvailability.Connected),
            GitHubFeature("Labels, assignees, and milestones", "Organize issue ownership and delivery targets.", FeatureAvailability.Connected),
            GitHubFeature("Pull request creation", "Open pull requests from safe review branches.", FeatureAvailability.Connected),
            GitHubFeature("Reviews and merge controls", "Read reviews, comment, manage drafts, and merge eligible pull requests.", FeatureAvailability.Connected),
            GitHubFeature("Review-first code changes", "Protected branches automatically use a new branch and pull request.", FeatureAvailability.Ready),
            GitHubFeature("Discussions and Projects", "Community discussions, project boards, and planning views.", FeatureAvailability.Roadmap)
        )
    ),
    FeatureCategory(
        title = "Actions & Android builds",
        subtitle = "Automation, logs, artifacts, and release pipelines",
        icon = Icons.Default.Build,
        features = listOf(
            GitHubFeature("Workflow browser", "View workflows, recent runs, states, branches, and conclusions.", FeatureAvailability.Connected),
            GitHubFeature("Run, cancel, and rerun", "Dispatch workflows and control active or failed runs.", FeatureAvailability.Connected),
            GitHubFeature("Jobs, steps, and logs", "Inspect job execution and open detailed workflow logs.", FeatureAvailability.Connected),
            GitHubFeature("Artifact downloads", "Download available workflow artifacts into the managed download queue.", FeatureAvailability.Connected),
            GitHubFeature("APK and AAB build control", "Start Android build workflows from a mobile developer dashboard.", FeatureAvailability.Ready),
            GitHubFeature("Build deep links", "Open a repository build or run directly from a supported link.", FeatureAvailability.Ready)
        )
    ),
    FeatureCategory(
        title = "Releases & distribution",
        subtitle = "Choose, inspect, download, and distribute project releases",
        icon = Icons.Default.Download,
        features = listOf(
            GitHubFeature("Release management", "Create draft releases and edit or delete existing release metadata.", FeatureAvailability.Connected),
            GitHubFeature("Release notes and assets", "Read release details and download individual assets.", FeatureAvailability.Connected),
            GitHubFeature("Managed downloads", "Track artifacts and release files in one download workspace.", FeatureAvailability.Ready),
            GitHubFeature("APK inspection", "Review package details, permissions, version data, and signing information before install.", FeatureAvailability.Ready),
            GitHubFeature("Secure Android install handoff", "Open inspected APK files through the Android package installer.", FeatureAvailability.Ready),
            GitHubFeature("Cross-platform download picker", "Select Android, Windows, Linux, iOS, or macOS assets with format, architecture, and protection guidance.", FeatureAvailability.Ready)
        )
    ),
    FeatureCategory(
        title = "Security, service & growth",
        subtitle = "Trustworthy controls with room for the wider GitHub platform",
        icon = Icons.Default.Security,
        features = listOf(
            GitHubFeature("No password collection", "GitHub credentials are never entered or stored inside GitHub Rock.", FeatureAvailability.Ready),
            GitHubFeature("Permission-aware actions", "Connected operations depend on GitHub authorization and repository access.", FeatureAvailability.Connected),
            GitHubFeature("Protected-branch workflow", "Code changes are proposed through branches and pull requests.", FeatureAvailability.Ready),
            GitHubFeature("Notifications and inbox", "Unified mentions, review requests, assignments, and workflow alerts.", FeatureAvailability.Roadmap),
            GitHubFeature("Security advisories and Dependabot", "Repository vulnerability alerts and dependency update workflows.", FeatureAvailability.Roadmap),
            GitHubFeature("Packages, Gists, and Codespaces", "Additional GitHub services inside the mobile control centre.", FeatureAvailability.Roadmap)
        )
    )
)

private val customerWorkflows: List<CustomerWorkflow> = listOf(
    CustomerWorkflow(
        title = "Individual developer",
        description = "Manage personal repositories, edit code, review workflows, and download builds from a phone.",
        tools = "Repositories · Code · Actions · Downloads",
        icon = Icons.Default.Code
    ),
    CustomerWorkflow(
        title = "Open-source maintainer",
        description = "Triage issues, review pull requests, manage contributors, and publish releases.",
        tools = "Issues · Pull requests · Reviews · Releases",
        icon = Icons.Default.Description
    ),
    CustomerWorkflow(
        title = "Team and organization",
        description = "Coordinate shared repositories, protected branches, approvals, and automation.",
        tools = "Permissions · Reviews · Workflows · Roadmap",
        icon = Icons.Default.CloudQueue
    ),
    CustomerWorkflow(
        title = "Android release manager",
        description = "Trigger APK or AAB builds, inspect artifacts, prepare releases, and install verified builds.",
        tools = "Builds · Artifacts · APK inspection · Releases",
        icon = Icons.Default.Build
    )
)
