package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubContributionDay
import com.sayanthrock.githubrock.core.model.GitHubProfileDetails
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.GitHubUser
import com.sayanthrock.githubrock.ui.components.GlassCard
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class ProfileRepositoryFilter(val title: String) {
    All("All"),
    Sources("Sources"),
    Forks("Forks"),
    Apps("Apps")
}

internal fun filterProfileRepositories(
    repositories: List<GitHubRepositoryModel>,
    query: String,
    filter: ProfileRepositoryFilter
): List<GitHubRepositoryModel> {
    val normalizedQuery = query.trim()
    return repositories.asSequence()
        .filter { repository ->
            normalizedQuery.isBlank() || listOfNotNull(
                repository.name,
                repository.fullName,
                repository.description,
                repository.language,
                repository.topics.joinToString(" ")
            ).any { it.contains(normalizedQuery, ignoreCase = true) }
        }
        .filter { repository ->
            when (filter) {
                ProfileRepositoryFilter.All -> true
                ProfileRepositoryFilter.Sources -> !repository.fork
                ProfileRepositoryFilter.Forks -> repository.fork
                ProfileRepositoryFilter.Apps -> repository.isInstallableProfileRepository()
            }
        }
        .sortedByDescending { it.updatedAt }
        .toList()
}

internal fun GitHubRepositoryModel.isInstallableProfileRepository(): Boolean {
    val searchable = buildList {
        add(name)
        add(description.orEmpty())
        add(language.orEmpty())
        addAll(topics)
    }.joinToString(" ").lowercase(Locale.ROOT)
    return listOf("android", "apk", "application", "mobile-app", "compose").any(searchable::contains)
}

@Composable
internal fun ProfileDashboardHeader(
    profile: GitHubUser?,
    isOwnProfile: Boolean,
    canFollow: Boolean,
    followStateLoaded: Boolean,
    isFollowing: Boolean,
    followUpdating: Boolean,
    onToggleFollow: () -> Unit,
    onRepositories: (() -> Unit)?,
    onFollowers: (() -> Unit)?,
    onFollowing: (() -> Unit)?
) {
    GlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "${profile?.login} profile picture",
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(22.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(84.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                profile?.login?.take(2)?.uppercase(Locale.getDefault()) ?: "GH",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        profile?.name ?: profile?.login ?: "GitHub profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    profile?.login?.let {
                        Text(
                            "@$it",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            profile?.bio?.takeIf(String::isNotBlank)?.let { bio ->
                Text(
                    bio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetric(
                    value = profile?.publicRepos ?: 0,
                    label = "Repos",
                    onClick = onRepositories,
                    modifier = Modifier.weight(1f)
                )
                ProfileMetric(
                    value = profile?.followers ?: 0,
                    label = "Followers",
                    onClick = onFollowers,
                    modifier = Modifier.weight(1f)
                )
                ProfileMetric(
                    value = profile?.following ?: 0,
                    label = "Following",
                    onClick = onFollowing,
                    modifier = Modifier.weight(1f)
                )
            }

            when {
                canFollow -> Button(
                    onClick = onToggleFollow,
                    enabled = followStateLoaded && !followUpdating,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (!followStateLoaded || followUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFollowing) "Unfollow" else "Follow", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(
    value: Int,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ContributionActivityCard(details: GitHubProfileDetails?) {
    val days = details?.contributionDays.orEmpty()
    val total = details?.contributionsLastYear ?: days.sumOf(GitHubContributionDay::count)
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Contribution activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "$total this year",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (days.isEmpty()) {
                Text(
                    "Contribution details are unavailable for this profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ContributionGrid(days)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    repeat(5) { index ->
                        Surface(
                            modifier = Modifier.size(13.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = contributionColor(index)
                        ) {}
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ContributionGrid(days: List<GitHubContributionDay>) {
    val visibleDays = days.takeLast(182)
    val weeks = visibleDays.chunked(7)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val level = contributionLevel(day)
                    Surface(
                        modifier = Modifier
                            .size(14.dp)
                            .semantics { contentDescription = "${day.date}: ${day.count} contributions" },
                        shape = RoundedCornerShape(4.dp),
                        color = contributionColor(level)
                    ) {}
                }
                repeat(7 - week.size) {
                    Spacer(Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun contributionLevel(day: GitHubContributionDay): Int = when (day.level.uppercase(Locale.ROOT)) {
    "FOURTH_QUARTILE" -> 4
    "THIRD_QUARTILE" -> 3
    "SECOND_QUARTILE" -> 2
    "FIRST_QUARTILE" -> 1
    else -> if (day.count > 0) 1 else 0
}

@Composable
private fun contributionColor(level: Int): Color = when (level.coerceIn(0, 4)) {
    0 -> MaterialTheme.colorScheme.surfaceContainerHighest
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = .24f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = .42f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = .68f)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
internal fun ProfileIdentitySummary(
    profile: GitHubUser?,
    details: GitHubProfileDetails?,
    onOpenUrl: (String) -> Unit
) {
    val localTime = remember {
        ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a · 'GMT'XXX", Locale.getDefault()))
    }
    val items = buildList {
        profile?.company?.takeIf(String::isNotBlank)?.let { add(IdentityItem(Icons.Default.Business, it, null)) }
        profile?.location?.takeIf(String::isNotBlank)?.let { add(IdentityItem(Icons.Default.LocationOn, it, null)) }
        add(IdentityItem(Icons.Default.Schedule, localTime, null))
        details?.pronouns?.takeIf(String::isNotBlank)?.let { add(IdentityItem(Icons.Default.Check, it, null)) }
    }
    val blog = profile?.blog?.takeIf(String::isNotBlank)
    val orcid = details?.orcid

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item -> IdentityChip(item) }
            }
            blog?.let { value ->
                ProfileLinkRow(
                    label = value,
                    onClick = { onOpenUrl(value.asHttpsUrl()) }
                )
            }
            orcid?.let { account ->
                ProfileLinkRow(
                    label = "ORCID · ${account.displayName}",
                    onClick = { onOpenUrl(account.url) }
                )
            }
        }
    }
}

private data class IdentityItem(val icon: ImageVector, val label: String, val url: String?)

@Composable
private fun IdentityChip(item: IdentityItem) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileLinkRow(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = .08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ProfileRepositoryToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: ProfileRepositoryFilter,
    onFilterChange: (ProfileRepositoryFilter) -> Unit,
    shown: Int,
    total: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search repositories…") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileRepositoryFilter.entries.forEach { filter ->
                Surface(
                    onClick = { onFilterChange(filter) },
                    shape = MaterialTheme.shapes.large,
                    color = if (selectedFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selectedFilter == filter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, if (selectedFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(filter.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(filter.title, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            "Showing $shown of $total repositories",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun ProfileRepositoryFilter.icon(): ImageVector = when (this) {
    ProfileRepositoryFilter.All -> Icons.Default.Code
    ProfileRepositoryFilter.Sources -> Icons.Default.Code
    ProfileRepositoryFilter.Forks -> Icons.Default.ForkRight
    ProfileRepositoryFilter.Apps -> Icons.Default.InstallMobile
}

@Composable
internal fun ProfileRepositoryCard(repository: GitHubRepositoryModel, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repository.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (repository.isInstallableProfileRepository()) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                    ) {
                        Text(
                            "APP",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Text(
                repository.description ?: "No repository description.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repository.language?.takeIf(String::isNotBlank)?.let { RepositoryTag(it) }
                RepositoryTag("★ ${repository.stars}")
                RepositoryTag("Forks ${repository.forks}")
                if (repository.fork) RepositoryTag("Fork")
                if (repository.isTemplate) RepositoryTag("Template")
            }
        }
    }
}

@Composable
private fun RepositoryTag(label: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun ProfilePersonCard(person: GitHubUser, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = person.avatarUrl,
                contentDescription = "${person.login} profile picture",
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name ?: person.login, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("@${person.login}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                person.bio?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun String.asHttpsUrl(): String = if (startsWith("https://", ignoreCase = true)) this else "https://$this"