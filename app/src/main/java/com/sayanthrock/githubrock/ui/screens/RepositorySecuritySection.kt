package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.model.SecurityAdvisory
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
internal fun RepositorySecuritySection(repository: GitHubRepositoryModel?, advisories: List<SecurityAdvisory>, securityPolicy: String?, onOpenUrl: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Published security advisories and this repository's SECURITY.md policy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (advisories.isEmpty()) {
            GlassCard { Text("No published security advisories.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            advisories.forEach { advisory -> SecurityAdvisoryCard(advisory, onOpenUrl) }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Security policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (securityPolicy == null) Text("No SECURITY.md policy was found in the default branch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else SecurityPolicyMarkdown(securityPolicy, repository, onOpenUrl)
            }
        }
    }
}

@Composable
private fun SecurityAdvisoryCard(advisory: SecurityAdvisory, onOpenUrl: (String) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(advisory.summary.ifBlank { advisory.ghsaId }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                advisory.severity?.takeIf(String::isNotBlank)?.let { severity -> AssistChip(onClick = {}, label = { Text(severity.uppercase()) }) }
                Text(advisory.ghsaId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                advisory.cveId?.takeIf(String::isNotBlank)?.let { cve -> Text(cve, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            advisory.description?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (advisory.htmlUrl.isNotBlank()) TextButton(onClick = { onOpenUrl(advisory.htmlUrl) }) { Text("Open advisory") }
        }
    }
}

@Composable
private fun SecurityPolicyMarkdown(markdown: String, repository: GitHubRepositoryModel?, onOpenUrl: (String) -> Unit) {
    val blocks = androidx.compose.runtime.remember(markdown) { com.sayanthrock.githubrock.core.util.MarkdownRenderer.render(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block.kind) {
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Heading -> Text(block.text, style = when (block.level) { 1 -> MaterialTheme.typography.headlineSmall; 2 -> MaterialTheme.typography.titleLarge; else -> MaterialTheme.typography.titleMedium }, fontWeight = FontWeight.Bold)
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Bullet -> Text("• ${block.text}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Task -> Text(if (block.checked) "☑ ${block.text}" else "☐ ${block.text}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Quote, com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Alert -> Text(block.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Code -> GlassCard { Text(block.text, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
                com.sayanthrock.githubrock.core.util.MarkdownBlockKind.Divider -> androidx.compose.material3.HorizontalDivider()
                else -> Text(block.text)
            }
        }
    }
}