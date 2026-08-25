package com.sayanthrock.githubrock.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryShowcaseScreen(repository: GitHubRepositoryModel?, onBack: () -> Unit, viewModel: RepositoryShowcaseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(repository) { viewModel.start(repository) }
    val displayedRepository = state.repository ?: repository
    val openGitHub: () -> Unit = {
        displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(displayedRepository?.name ?: "Repository", maxLines = 1, overflow = TextOverflow.Ellipsis); displayedRepository?.owner?.login?.let { Text("@$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = { if (!displayedRepository?.htmlUrl.isNullOrBlank()) IconButton(onClick = openGitHub) { Icon(Icons.Default.OpenInNew, "Open repository on GitHub") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = .94f))
        )
    }) { padding ->
        RepositoryShowcaseContent(displayedRepository, state.readme, state.loading, state.readmeLoading, state.error, state.readmeError, viewModel::retry, openGitHub, Modifier.padding(padding))
    }
}

@Composable
fun RepositoryShowcaseContent(repository: GitHubRepositoryModel?, readme: String?, loading: Boolean, readmeLoading: Boolean, error: String?, readmeError: String?, onRetry: () -> Unit, onOpenGitHub: () -> Unit, modifier: Modifier = Modifier) {
    val blocks = remember(readme, repository?.htmlUrl, repository?.defaultBranch) { readme?.let(MarkdownRenderer::render).orEmpty() }
    val context = LocalContext.current
    val openLink: (String) -> Unit = remember(context, repository?.htmlUrl, repository?.defaultBranch) {
        { url ->
            val resolved = resolveReadmeUrl(url, repository, image = false)
            if (!resolved.startsWith("#")) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolved)))
        }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (loading && repository == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Text(message, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Try again") } } } } }
        repository?.let { repo ->
            item { RepositoryIdentityHero(repo) }
            item { RepositoryDescriptionCard(repo) }
            item { OutlinedButton(onClick = onOpenGitHub, enabled = repo.htmlUrl.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open on GitHub", fontWeight = FontWeight.Bold) } }
            item { RepositoryDetailsGrid(repo) }
            if (repo.topics.isNotEmpty()) item { RepositoryTopics(repo.topics) }
        }
        item { ReadmeHeader() }
        when {
            readmeLoading -> item { GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Loading README…", fontWeight = FontWeight.SemiBold); LinearProgressIndicator(Modifier.fillMaxWidth()) } } }
            readme != null && blocks.isEmpty() -> item { GlassCard { Text("README.md is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            readme != null -> itemsIndexed(blocks, key = { index, _ -> "readme-block-$index" }) { _, block -> ReadmeBlockCard(block, openLink, repository) }
            readmeError != null -> item { GlassCard { Text(readmeError, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable private fun ReadmeBlockCard(block: MarkdownBlock, onOpenLink: (String) -> Unit, repository: GitHubRepositoryModel?) { GlassCard { ReadmeBlock(block, onOpenLink, repository) } }

@Composable
private fun ReadmeBlock(block: MarkdownBlock, onOpenLink: (String) -> Unit, repository: GitHubRepositoryModel?) {
    when (block.kind) {
        MarkdownBlockKind.Heading -> InlineMarkdownText(block.text, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), onOpenLink)
        MarkdownBlockKind.Paragraph -> InlineMarkdownText(block.text, MaterialTheme.typography.bodyMedium, onOpenLink)
        MarkdownBlockKind.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (block.ordered) "${block.level}." else "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            InlineMarkdownText(block.text, MaterialTheme.typography.bodyMedium, onOpenLink, Modifier.weight(1f))
        }
        MarkdownBlockKind.Quote -> Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .08f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f))) { InlineMarkdownText(block.text, MaterialTheme.typography.bodyMedium, onOpenLink, Modifier.padding(12.dp), MaterialTheme.colorScheme.onSurfaceVariant) }
        MarkdownBlockKind.Code -> Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.background.copy(alpha = .72f)) { Text(block.text, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, softWrap = false) }
        MarkdownBlockKind.Divider -> HorizontalDivider()
        MarkdownBlockKind.Image -> AsyncImage(model = resolveReadmeUrl(block.url.orEmpty(), repository, image = true), contentDescription = block.text, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        MarkdownBlockKind.Table -> ResponsiveMarkdownTable(block.table, onOpenLink)
    }
}

@Composable
@Composable
private fun InlineMarkdownText(text: String, style: TextStyle, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier, color: Color? = null) {
    val resolvedColor = color ?: MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, resolvedColor, linkColor) { buildMarkdownAnnotatedString(text, resolvedColor, linkColor) }
    ClickableText(text = annotated, modifier = modifier, style = style.copy(color = resolvedColor), onClick = { offset -> annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { onOpenLink(it.item) } })
}

private fun buildMarkdownAnnotatedString(text: String, codeBackground: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("(\\*\\*|__)(.+?)(\\1)|(`)(.+?)(\\4)|(~~)(.+?)(\\7)|(?<!\\*)\\*([^*]+)\\*(?!\\*)|(?<!_)_([^_]+)_(?!_)|\\[([^]]+)]\\(([^)]+)\\)")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val g = match.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground.copy(alpha = .10f))) { append(g[5]) }
            g[7].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(g[8]) }
            g[10].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[10]) }
            g[11].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[11]) }
            g[12].isNotEmpty() -> { pushStringAnnotation("URL", g[13]); withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(g[12]) }; pop() }
            else -> append(match.value)
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

@Composable
private fun ResponsiveMarkdownTable(table: MarkdownTable?, onOpenLink: (String) -> Unit) {
    if (table == null) return
    val widths = List(table.headers.size) { 180.dp }
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row { table.headers.forEachIndexed { index, cell -> MarkdownTableCell(cell, widths[index], true, onOpenLink) } }
        table.rows.forEach { row -> Row { table.headers.indices.forEach { index -> MarkdownTableCell(row.getOrElse(index) { "" }, widths[index], false, onOpenLink) } } }
    }
}

@Composable
private fun MarkdownTableCell(text: String, width: Dp, header: Boolean, onOpenLink: (String) -> Unit) {
    Surface(modifier = Modifier.width(width), color = if (header) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        InlineMarkdownText(text, MaterialTheme.typography.bodySmall.copy(fontWeight = if (header) FontWeight.Bold else FontWeight.Normal), onOpenLink, Modifier.padding(10.dp))
    }
}

private fun resolveReadmeUrl(url: String, repository: GitHubRepositoryModel?, image: Boolean): String {
    if (url.isBlank() || url.startsWith("#") || url.startsWith("http://") || url.startsWith("https://")) return url
    if (url.startsWith("//")) return "https:$url"
    val repo = repository ?: return url
    return runCatching {
        val owner = repo.owner.login
        val name = repo.name
        val branch = repo.defaultBranch
        val base = if (image) URI("https://raw.githubusercontent.com/$owner/$name/$branch/") else URI("https://github.com/$owner/$name/blob/$branch/")
        base.resolve(url.removePrefix("./")).toString()
    }.getOrDefault(url)
}

@Composable private fun RepositoryIdentityHero(repository: GitHubRepositoryModel) { GlassCard(contentPadding = PaddingValues(0.dp)) { Column { Box(Modifier.fillMaxWidth()) { RepositoryArtwork(repository, compact = false); RepositoryProjectIcon(repository, Modifier.align(Alignment.BottomStart).offset(x = 18.dp, y = 34.dp)) }; Spacer(Modifier.height(42.dp)); Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text(repository.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(repository.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }; RepositoryTypeBadge(repository) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (repository.private) MiniBadge("Private"); if (repository.fork) MiniBadge("Fork"); if (repository.isTemplate) MiniBadge("Template"); if (!repository.private && !repository.fork && !repository.isTemplate) MiniBadge("Public") } } } } }
@Composable private fun RepositoryProjectIcon(repository: GitHubRepositoryModel, modifier: Modifier = Modifier) { Surface(modifier = modifier.size(76.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)) { when { !repository.previewImageUrl.isNullOrBlank() -> AsyncImage(repository.previewImageUrl, "${repository.name} application icon", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()); repository.owner.avatarUrl.isNotBlank() -> AsyncImage(repository.owner.avatarUrl, "${repository.name} application icon", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()); else -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha=.24f), MaterialTheme.colorScheme.secondary.copy(alpha=.2f)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, "${repository.name} application icon") } } } }
@Composable private fun RepositoryTypeBadge(repository: GitHubRepositoryModel) { Surface(shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.primary.copy(alpha=.14f), border=BorderStroke(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.36f))) { Text(if(repository.isTemplate) "Template" else if(repository.topics.any { it.equals("android",true)||it.equals("app",true)||it.equals("application",true)||it.endsWith("-app",true)||it.endsWith("-application",true) }) "Application" else "Repository", modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp), color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.labelMedium, fontWeight=FontWeight.Bold) } }
@Composable private fun MiniBadge(label:String) { Surface(shape=RoundedCornerShape(999.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.62f)) { Text(label, Modifier.padding(horizontal=10.dp,vertical=5.dp), style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun RepositoryDescriptionCard(repository:GitHubRepositoryModel) { GlassCard { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("About this project",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); Text(repository.description ?: "This repository does not have a description yet.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant); HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.7f)); Text("Default branch · ${repository.defaultBranch}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary) } } }
@Composable private fun RepositoryDetailsGrid(repository:GitHubRepositoryModel) { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("Project details",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DetailTile("Stars",compactCount(repository.stars),Modifier.weight(1f));DetailTile("Forks",compactCount(repository.forks),Modifier.weight(1f))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DetailTile("Open issues",compactCount(repository.openIssues),Modifier.weight(1f));DetailTile("Language",repository.language ?: "Not detected",Modifier.weight(1f))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DetailTile("Branch",repository.defaultBranch,Modifier.weight(1f));DetailTile("Updated",repository.updatedAt.take(10).ifBlank{"Unknown"},Modifier.weight(1f))} } }
@Composable private fun DetailTile(label:String,value:String,modifier:Modifier=Modifier){Surface(modifier=modifier.height(104.dp),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.46f),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.5f))){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.SpaceBetween){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelLarge);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun RepositoryTopics(topics:List<String>){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Topics",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){topics.forEach{topic->Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.11f)){Text(topic,Modifier.padding(horizontal=12.dp,vertical=7.dp),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)}}}}}
@Composable private fun ReadmeHeader(){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Row(verticalAlignment=Alignment.CenterVertically){Surface(CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(48.dp)){Icon(Icons.AutoMirrored.Filled.MenuBook,null,Modifier.padding(12.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)};Spacer(Modifier.width(16.dp));Column{Text("README.md",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Project documentation",color=MaterialTheme.colorScheme.onSurfaceVariant)}};Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)){Text("Complete",Modifier.padding(horizontal=11.dp,vertical=6.dp),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)}}}
private fun compactCount(value:Int):String=when{value>=1_000_000->{val w=value/1_000_000;val d=(value%1_000_000)/100_000;if(d==0)"${w}M" else "$w.${d}M"};value>=1_000->{val w=value/1_000;val d=(value%1_000)/100;if(d==0)"${w}k" else "$w.${d}k"};else->value.toString()}
