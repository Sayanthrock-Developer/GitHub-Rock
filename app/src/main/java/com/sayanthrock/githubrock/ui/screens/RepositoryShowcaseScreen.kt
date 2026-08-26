package com.sayanthrock.githubrock.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.core.util.MarkdownTable
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryArtwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryShowcaseScreen(repository: GitHubRepositoryModel?, onBack: () -> Unit, viewModel: RepositoryShowcaseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(repository) { viewModel.start(repository) }
    val displayedRepository = state.repository ?: repository
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayedRepository?.name ?: "Repository", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url -> IconButton(onClick = { openHttpsBrowser(context, url) }) { Icon(Icons.Default.OpenInNew, "Open repository on GitHub") } } }
            )
        }
    ) { padding ->
        RepositoryShowcaseContent(
            repository = displayedRepository,
            readme = state.readme,
            loading = state.loading,
            readmeLoading = state.readmeLoading,
            error = state.error,
            readmeError = state.readmeError,
            onRetry = viewModel::retry,
            onOpenGitHub = { displayedRepository?.htmlUrl?.let { openHttpsBrowser(context, it) } },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun RepositoryShowcaseContent(
    repository: GitHubRepositoryModel?,
    readme: String?,
    loading: Boolean,
    readmeLoading: Boolean,
    error: String?,
    readmeError: String?,
    onRetry: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(readme) { readme?.let(MarkdownRenderer::render).orEmpty() }
    val context = LocalContext.current
    val openLink: (String) -> Unit = remember(context, repository) {
        { raw -> resolveReadmeUrl(raw, repository)?.let { openHttpsBrowser(context, it) } }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (loading && repository == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message ->
            item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(message, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = onRetry) { Text("Try again") } } } }
        }
        repository?.let { repo ->
            item { RepositoryIdentityHero(repo) }
            item { RepositoryDescriptionCard(repo) }
            item { Button(onClick = onOpenGitHub, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open on GitHub") } }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("README.md", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Project documentation", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            readmeLoading -> item { GlassCard { LinearProgressIndicator(Modifier.fillMaxWidth()) } }
            readme != null -> itemsIndexed(blocks, key = { index, _ -> index }) { _, block -> GlassCard { RenderMarkdownBlock(block, openLink, repository) } }
            readmeError != null -> item { GlassCard { Text(readmeError, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable
private fun RenderMarkdownBlock(block: MarkdownBlock, openLink: (String) -> Unit, repository: GitHubRepositoryModel?) {
    when (block.kind) {
        MarkdownBlockKind.Heading -> InlineMarkdownText(block.text, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), openLink)
        MarkdownBlockKind.Paragraph -> InlineMarkdownText(block.text, MaterialTheme.typography.bodyLarge, openLink)
        MarkdownBlockKind.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (block.ordered) "${block.level}." else "•"); InlineMarkdownText(block.text, MaterialTheme.typography.bodyMedium, openLink, Modifier.weight(1f)) }
        MarkdownBlockKind.Quote -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) { InlineMarkdownText(block.text, MaterialTheme.typography.bodyMedium, openLink, Modifier.padding(12.dp)) }
        MarkdownBlockKind.Code -> Text(block.text, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace, softWrap = false)
        MarkdownBlockKind.Divider -> HorizontalDivider()
        MarkdownBlockKind.Image -> AsyncImage(model = resolveReadmeUrl(block.url.orEmpty(), repository, image = true), contentDescription = block.text, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        MarkdownBlockKind.Table -> ResponsiveMarkdownTable(block.table, openLink)
    }
}

@Composable
private fun InlineMarkdownText(text: String, style: TextStyle, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) { buildMarkdownAnnotatedString(text, linkColor) }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        onClick = { offset -> annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { onOpenLink(it.item) } }
    )
}

private fun buildMarkdownAnnotatedString(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("(\\*\\*|__)(.+?)(\\1)|(`)(.+?)(\\4)|(~~)(.+?)(\\7)|(?<!\\*)\\*([^*]+)\\*(?!\\*)|(?<!_)_([^_]+)_(?!_)|\\[([^]]+)]\\(([^)]+)\\)")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val g = match.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(g[5]) }
            g[7].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(g[8]) }
            g[9].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[9]) }
            g[10].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[10]) }
            g[11].isNotEmpty() -> {
                pushStringAnnotation("URL", g[12])
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(g[11]) }
                pop()
            }
            else -> append(match.value)
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

@Composable
private fun ResponsiveMarkdownTable(table: MarkdownTable?, openLink: (String) -> Unit) {
    if (table == null) return
    val width = 180.dp
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row { table.headers.forEach { MarkdownTableCell(it, width, true, openLink) } }
        table.rows.forEach { row -> Row { table.headers.indices.forEach { i -> MarkdownTableCell(row.getOrElse(i) { "" }, width, false, openLink) } } }
    }
}

@Composable
private fun MarkdownTableCell(text: String, width: Dp, header: Boolean, openLink: (String) -> Unit) {
    Surface(modifier = Modifier.width(width), color = if (header) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        InlineMarkdownText(text, MaterialTheme.typography.bodySmall.copy(fontWeight = if (header) FontWeight.Bold else FontWeight.Normal), openLink, Modifier.padding(10.dp))
    }
}

@Composable
private fun RepositoryIdentityHero(repository: GitHubRepositoryModel) {
    GlassCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            RepositoryArtwork(repository, compact = false)
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(repository.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Application", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RepositoryDescriptionCard(repository: GitHubRepositoryModel) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("About this project", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(repository.description ?: "This repository does not have a description yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun resolveReadmeUrl(url: String, repository: GitHubRepositoryModel?, image: Boolean = false): String? {
    if (url.isBlank() || url.startsWith("#")) return null
    if (url.startsWith("https://")) return url
    if (url.startsWith("http://")) return null
    if (url.startsWith("//")) return "https:$url"
    repository ?: return null
    val prefix = if (image) "https://raw.githubusercontent.com/${repository.owner.login}/${repository.name}/${repository.defaultBranch}/" else "https://github.com/${repository.owner.login}/${repository.name}/blob/${repository.defaultBranch}/"
    return runCatching { java.net.URI(prefix).resolve(url.removePrefix("./")).toString() }.getOrNull()
}

private fun openHttpsBrowser(context: Context, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.userInfo != null) return false
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}
