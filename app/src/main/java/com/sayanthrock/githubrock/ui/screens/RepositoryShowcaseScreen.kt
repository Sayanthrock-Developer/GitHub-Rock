package com.sayanthrock.githubrock.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import coil.compose.SubcomposeAsyncImage
import com.sayanthrock.githubrock.core.model.GitHubRepositoryModel
import com.sayanthrock.githubrock.core.translation.GoogleTranslationService
import com.sayanthrock.githubrock.data.settings.AppLanguages
import com.sayanthrock.githubrock.core.util.MarkdownBlock
import com.sayanthrock.githubrock.core.util.MarkdownBlockKind
import com.sayanthrock.githubrock.core.util.MarkdownRenderer
import com.sayanthrock.githubrock.core.util.MarkdownTable
import com.sayanthrock.githubrock.ui.components.GlassCard
import com.sayanthrock.githubrock.ui.components.RepositoryArtwork
import com.sayanthrock.githubrock.ui.icons.RockIcon
import com.sayanthrock.githubrock.ui.icons.vector
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryShowcaseScreen(
    repository: GitHubRepositoryModel?,
    onBack: () -> Unit,
    onOpenNativeLink: (String) -> Boolean = { false },
    viewModel: RepositoryShowcaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(repository) { viewModel.start(repository) }
    val displayedRepository = state.repository ?: repository
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayedRepository?.name ?: "Repository", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(RockIcon.Back.vector(), "Back") } },
                actions = {
                    displayedRepository?.htmlUrl?.takeIf(String::isNotBlank)?.let { url ->
                        IconButton(onClick = { openHttpsBrowser(context, url) }) {
                            Icon(RockIcon.OpenInNew.vector(), "Open repository on GitHub")
                        }
                    }
                }
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
            onOpenNativeLink = onOpenNativeLink,
            translationTarget = state.translationTarget,
            translatedBlocks = state.translatedBlocks,
            translationLoading = state.translationLoading,
            translationError = state.translationError,
            onTranslate = viewModel::translateReadme,
            onClearTranslation = viewModel::clearTranslation,
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
    onOpenNativeLink: (String) -> Boolean = { false },
    translationTarget: String? = null,
    translatedBlocks: Map<Int, String> = emptyMap(),
    translationLoading: Boolean = false,
    translationError: String? = null,
    onTranslate: (List<MarkdownBlock>, String) -> Unit = { _, _ -> },
    onClearTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val blocks = remember(readme) { readme?.let(MarkdownRenderer::render).orEmpty() }
    val context = LocalContext.current
    var showTranslationPicker by remember { mutableStateOf(false) }
    val openLink: (String) -> Unit = remember(context, repository, onOpenNativeLink) {
        { raw ->
            val resolved = resolveReadmeUrl(raw, repository)
            if (resolved != null && !onOpenNativeLink(resolved)) openHttpsBrowser(context, resolved)
        }
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (loading && repository == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message ->
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry) { Text("Try again") }
                    }
                }
            }
        }
        repository?.let { repo ->
            item { RepositoryIdentityHero(repo) }
            item { RepositoryDescriptionCard(repo) }
            item {
                Button(onClick = onOpenGitHub, modifier = Modifier.fillMaxWidth()) {
                    Icon(RockIcon.OpenInNew.vector(), null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open on GitHub")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text("README.md", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        translationTarget?.let { "Translated with Google • $it" } ?: "Project documentation",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showTranslationPicker = true }, enabled = readme != null && !translationLoading) {
                    Text(if (translationTarget == null) "Translate" else "Change")
                }
            }
        }
        if (translationLoading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        translationError?.let { message ->
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Translation unavailable", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        translationTarget?.let { target ->
                            OutlinedButton(onClick = { onTranslate(blocks, target) }, enabled = !translationLoading) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
        when {
            readmeLoading -> item { GlassCard { LinearProgressIndicator(Modifier.fillMaxWidth()) } }
            readme != null && blocks.isNotEmpty() -> {
                itemsIndexed(blocks, key = { index, _ -> index }) { index, block ->
                    RenderMarkdownBlock(block, openLink, repository, translatedBlocks[index] ?: block.text)
                }
            }
            readme != null -> item {
                GlassCard {
                    Text("This README is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            readmeError != null -> item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Couldn't load README.md", color = MaterialTheme.colorScheme.error)
                        Text(readmeError, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
    }
    if (showTranslationPicker) {
        TranslationPickerDialog(
            selectedLanguage = translationTarget,
            onDismiss = { showTranslationPicker = false },
            onSelect = { language ->
                if (language == null) onClearTranslation() else onTranslate(blocks, language)
                showTranslationPicker = false
            }
        )
    }
}

@Composable
private fun TranslationPickerDialog(
    selectedLanguage: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val appLanguages = remember(currentLocale) { AppLanguages.available(currentLocale) }
    val mlKitCodes = remember { GoogleTranslationService.supportedLanguages.map { it.code }.toSet() }
    val languages = remember(appLanguages, mlKitCodes) {
        appLanguages.filter { it.tag.substringBefore('-').lowercase() in mlKitCodes }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Translate README") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Google translation runs on-device after the language model is downloaded.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Original", modifier = Modifier.weight(1f))
                            if (selectedLanguage == null) Text("✓")
                        }
                    }
                    items(
                        languages,
                        key = { it.tag }
                    ) { language ->
                        val code = language.tag.substringBefore('-').lowercase()
                        TextButton(onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth()) {
                            Text(language.displayName, modifier = Modifier.weight(1f))
                            Text(language.tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (selectedLanguage == code) Text("✓")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun RenderMarkdownBlock(
    block: MarkdownBlock,
    openLink: (String) -> Unit,
    repository: GitHubRepositoryModel?,
    displayText: String = block.text
) {
    when (block.kind) {
        MarkdownBlockKind.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            InlineMarkdownText(displayText, style.copy(fontWeight = FontWeight.Bold), openLink)
        }
        MarkdownBlockKind.Paragraph -> InlineMarkdownText(displayText, MaterialTheme.typography.bodyLarge, openLink)
        MarkdownBlockKind.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (block.ordered) "${block.level}." else "•")
            InlineMarkdownText(displayText, MaterialTheme.typography.bodyLarge, openLink, Modifier.weight(1f))
        }
        MarkdownBlockKind.Task -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (block.checked) RockIcon.CheckBox.vector() else RockIcon.CheckBoxOutlineBlank.vector(),
                contentDescription = if (block.checked) "Completed task" else "Task"
            )
            InlineMarkdownText(displayText, MaterialTheme.typography.bodyLarge, openLink, Modifier.weight(1f))
        }
        MarkdownBlockKind.Quote -> Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            InlineMarkdownText(displayText, MaterialTheme.typography.bodyMedium, openLink, Modifier.padding(12.dp))
        }
        MarkdownBlockKind.Alert -> Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("GitHub alert", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                InlineMarkdownText(displayText, MaterialTheme.typography.bodyMedium, openLink)
            }
        }
        MarkdownBlockKind.Code -> CodeBlock(block, repository)
        MarkdownBlockKind.Divider -> HorizontalDivider()
        MarkdownBlockKind.Image -> {
            val model = resolveReadmeUrl(block.url.orEmpty(), repository, image = true)
            if (model == null) {
                Text(block.text.ifBlank { "Unavailable image" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = block.text,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    loading = { Box(Modifier.fillMaxWidth().heightIn(min = 72.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
                    error = { Text("Image unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
        }
        MarkdownBlockKind.Table -> ResponsiveMarkdownTable(block.table, openLink)
    }
}

@Composable
private fun CodeBlock(block: MarkdownBlock, repository: GitHubRepositoryModel?) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(block.codeLanguage ?: "Code", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("README code", block.text))
                }) {
                    Icon(RockIcon.Copy.vector(), null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
            }
            Text(
                block.text,
                Modifier.fillMaxWidth().horizontalScroll(scroll),
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun InlineMarkdownText(
    text: String,
    style: TextStyle,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
    val pattern = Regex("(\\*\\*|__)(.+?)(\\1)|(`)(.+?)(\\4)|(~~)(.+?)(\\6)|(?<!\\*)\\*([^*]+)\\*(?!\\*)|(?<!_)_([^_]+)_(?!_)|\\[([^]]+)]\\(([^)]+)\\)")
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
        table.rows.forEach { row ->
            Row { table.headers.indices.forEach { i -> MarkdownTableCell(row.getOrElse(i) { "" }, width, false, openLink) } }
        }
    }
}

@Composable
private fun MarkdownTableCell(text: String, width: Dp, header: Boolean, openLink: (String) -> Unit) {
    Surface(
        modifier = Modifier.width(width),
        color = if (header) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        InlineMarkdownText(
            text,
            MaterialTheme.typography.bodySmall.copy(fontWeight = if (header) FontWeight.Bold else FontWeight.Normal),
            openLink,
            Modifier.padding(10.dp)
        )
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

internal fun resolveReadmeUrl(url: String, repository: GitHubRepositoryModel?, image: Boolean = false): String? {
    if (url.isBlank() || url.startsWith("#")) return null
    if (url.startsWith("http://")) return null
    if (url.startsWith("https://")) return url
    if (url.startsWith("//")) return "https:$url"
    repository ?: return null
    val base = if (image) {
        "https://raw.githubusercontent.com/${repository.owner.login}/${repository.name}/${repository.defaultBranch}/"
    } else {
        "https://github.com/${repository.owner.login}/${repository.name}/blob/${repository.defaultBranch}/"
    }
    return runCatching { URI(base).resolve(url.removePrefix("./")).toString() }.getOrNull()
}

private fun openHttpsBrowser(context: Context, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.userInfo != null) return false
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}
