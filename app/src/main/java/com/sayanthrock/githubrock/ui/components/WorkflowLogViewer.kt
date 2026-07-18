package com.sayanthrock.githubrock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sayanthrock.githubrock.core.util.WorkflowLogHighlighter
import com.sayanthrock.githubrock.core.util.WorkflowLogTokenKind
import com.sayanthrock.githubrock.data.settings.LogDisplayStyle

private val TerminalBackground = Color(0xFF0D1117)
private val TerminalSurface = Color(0xFF161B22)
private val TerminalText = Color(0xFFC9D1D9)
private val TerminalMuted = Color(0xFF7D8590)
private val TerminalBlue = Color(0xFF79C0FF)
private val TerminalPurple = Color(0xFFD2A8FF)
private val TerminalGreen = Color(0xFF7EE787)
private val TerminalYellow = Color(0xFFE3B341)
private val TerminalRed = Color(0xFFFF7B72)

private data class LogPalette(
    val normal: Color,
    val timestamp: Color,
    val command: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val section: Color
)

@Composable
fun WorkflowLogViewer(
    title: String,
    log: String?,
    style: LogDisplayStyle,
    onDismiss: () -> Unit
) {
    val displayLog = log ?: "Loading workflow logs…"
    when (style) {
        LogDisplayStyle.Dialog -> WorkflowLogDialog(title, displayLog, onDismiss)
        LogDisplayStyle.Terminal -> WorkflowLogTerminal(title, displayLog, onDismiss)
    }
}

@Composable
private fun WorkflowLogDialog(title: String, log: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                LogHeader(
                    title = title,
                    log = log,
                    darkTerminal = false,
                    onCopy = { clipboard.setText(AnnotatedString(log)) },
                    onDismiss = onDismiss
                )
                HorizontalDivider()
                SelectionContainer {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            highlightedLog(log, terminal = false),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.widthIn(min = 560.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun WorkflowLogTerminal(title: String, log: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val lines = remember(log) { log.lines() }
    val palette = logPalette(terminal = true)
    val horizontal = rememberScrollState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = TerminalBackground) {
            Column {
                LogHeader(
                    title = title,
                    log = log,
                    darkTerminal = true,
                    onCopy = { clipboard.setText(AnnotatedString(log)) },
                    onDismiss = onDismiss
                )
                HorizontalDivider(color = Color(0xFF30363D))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontal)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        Row(
                            modifier = Modifier
                                .widthIn(min = 760.dp)
                                .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = .012f))
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = (index + 1).toString().padStart(4),
                                color = TerminalMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.width(56.dp).padding(end = 12.dp)
                            )
                            Text(
                                text = highlightedLine(line, palette),
                                color = TerminalText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogHeader(
    title: String,
    log: String,
    darkTerminal: Boolean,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val summary = remember(log) { WorkflowLogHighlighter.summary(log) }
    val primary = if (darkTerminal) Color.White else MaterialTheme.colorScheme.onSurface
    val secondary = if (darkTerminal) TerminalMuted else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (darkTerminal) TerminalSurface else Color.Transparent)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(TerminalRed, TerminalYellow, TerminalGreen).forEach { color ->
                Surface(Modifier.size(9.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {}
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = primary, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "${summary.lines} lines · ${summary.warnings} warnings · ${summary.errors} errors",
                color = secondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy complete log", tint = primary)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close logs", tint = primary)
        }
    }
}

@Composable
private fun highlightedLog(log: String, terminal: Boolean): AnnotatedString {
    val palette = logPalette(terminal)
    val lines = remember(log) { log.lines() }
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            append(highlightedLine(line, palette))
            if (index != lines.lastIndex) append('\n')
        }
    }
}

private fun highlightedLine(line: String, palette: LogPalette): AnnotatedString {
    val spans = WorkflowLogHighlighter.highlight(line)
    return buildAnnotatedString {
        var cursor = 0
        spans.forEach { span ->
            if (span.start > cursor) withStyle(SpanStyle(color = palette.normal)) {
                append(line.substring(cursor, span.start))
            }
            withStyle(SpanStyle(color = tokenColor(span.kind, palette), fontWeight = tokenWeight(span.kind))) {
                append(line.substring(span.start, span.end))
            }
            cursor = span.end
        }
        if (cursor < line.length) withStyle(SpanStyle(color = palette.normal)) {
            append(line.substring(cursor))
        }
    }
}

@Composable
private fun logPalette(terminal: Boolean): LogPalette = if (terminal) {
    LogPalette(TerminalText, TerminalMuted, TerminalPurple, TerminalGreen, TerminalYellow, TerminalRed, TerminalBlue)
} else {
    LogPalette(
        normal = MaterialTheme.colorScheme.onSurface,
        timestamp = MaterialTheme.colorScheme.onSurfaceVariant,
        command = MaterialTheme.colorScheme.primary,
        success = MaterialTheme.colorScheme.tertiary,
        warning = Color(0xFFB78103),
        error = MaterialTheme.colorScheme.error,
        section = MaterialTheme.colorScheme.secondary
    )
}

private fun tokenColor(kind: WorkflowLogTokenKind, palette: LogPalette): Color = when (kind) {
    WorkflowLogTokenKind.Timestamp -> palette.timestamp
    WorkflowLogTokenKind.Command -> palette.command
    WorkflowLogTokenKind.Success -> palette.success
    WorkflowLogTokenKind.Warning -> palette.warning
    WorkflowLogTokenKind.Error -> palette.error
    WorkflowLogTokenKind.Section -> palette.section
}

private fun tokenWeight(kind: WorkflowLogTokenKind): FontWeight? = when (kind) {
    WorkflowLogTokenKind.Error, WorkflowLogTokenKind.Warning, WorkflowLogTokenKind.Section -> FontWeight.SemiBold
    else -> null
}
