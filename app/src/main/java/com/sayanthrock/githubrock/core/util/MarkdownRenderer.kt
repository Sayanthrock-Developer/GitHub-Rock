package com.sayanthrock.githubrock.core.util

import java.util.Locale

/**
 * Small native GitHub-flavoured Markdown parser used by README screens.
 *
 * It intentionally keeps the parsed representation independent from Compose so the
 * same parser can be exercised by JVM unit tests and rendered by the UI layer.
 */
object MarkdownRenderer {

    fun render(markdown: String): List<MarkdownBlock> {
        val source = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val lines = source.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val table = mutableListOf<String>()
        var inFence = false
        var fenceLanguage: String? = null
        val fenceLines = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                val text = paragraph.joinToString("\n").trim()
                if (text.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, text)
                paragraph.clear()
            }
        }

        fun flushTable() {
            if (table.isEmpty()) return
            val rows = table.map { it.split('|').dropWhile { cell -> cell.isBlank() }.dropLastWhile { cell -> cell.isBlank() }.map(String::trim) }
            if (rows.size >= 2 && rows[1].all { row -> row.isNotEmpty() && row.all { cell -> cell.matches(Regex(":?-{3,}:?")) } }) {
                blocks += MarkdownBlock(MarkdownBlockKind.Table, rows.drop(1).joinToString("\n") { it.joinToString(" | ") })
            } else {
                paragraph += table
            }
            table.clear()
        }

        fun isBlockHtmlWrapper(line: String): Boolean =
            line.trim().matches(Regex("</?(div|p|center|section|article|aside|header|footer|main|figure|figcaption)(\\s[^>]*)?/?>", RegexOption.IGNORE_CASE))

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            if (inFence) {
                if (line.trim().startsWith("```")) {
                    blocks += MarkdownBlock(MarkdownBlockKind.Code, fenceLines.joinToString("\n"), fenceLanguage)
                    fenceLines.clear()
                    inFence = false
                    fenceLanguage = null
                } else fenceLines += line
                continue
            }

            val fence = Regex("^\\s*```(.*)$").find(line)
            if (fence != null) {
                flushTable(); flushParagraph()
                inFence = true
                fenceLanguage = fence.groupValues[1].trim().ifBlank { null }
                continue
            }

            if (line.isBlank()) {
                flushTable(); flushParagraph(); continue
            }
            if (isBlockHtmlWrapper(line)) continue

            val heading = Regex("^\\s*(#{1,6})\\s+(.+?)\\s*$").find(line)
            if (heading != null) {
                flushTable(); flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockKind.Heading, heading.groupValues[2], heading.groupValues[1].length)
                continue
            }

            if (line.matches(Regex("^\\s*((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$"))) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Divider, ""); continue
            }

            val task = Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+(.+)$").find(line)
            if (task != null) {
                flushTable(); flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockKind.Task, task.groupValues[2], if (task.groupValues[1].equals("x", true)) 1 else 0)
                continue
            }

            val unordered = Regex("^\\s*[-*+]\\s+(.+)$").find(line)
            if (unordered != null) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.UnorderedList, unordered.groupValues[1]); continue
            }

            val ordered = Regex("^\\s*\\d+[.)]\\s+(.+)$").find(line)
            if (ordered != null) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.OrderedList, ordered.groupValues[1]); continue
            }

            val quote = Regex("^\\s*>\\s?(.*)$").find(line)
            if (quote != null) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Quote, cleanInline(quote.groupValues[1])); continue
            }

            val alert = Regex("^\\s*>\\s*\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]\\s*(.*)$", RegexOption.IGNORE_CASE).find(line)
            if (alert != null) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Alert, alert.groupValues[2], alert.groupValues[1].uppercase(Locale.ROOT)); continue
            }

            val htmlImages = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(line).toList()
            if (htmlImages.isNotEmpty()) {
                flushTable(); flushParagraph()
                htmlImages.forEach { match ->
                    val tag = match.value
                    val src = Regex("\\bsrc\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)
                    if (src != null) {
                        val alt = Regex("\\balt\\s*=\\s*[\\\"']([^\\\"']*)[\\\"']", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1).orEmpty()
                        blocks += MarkdownBlock(MarkdownBlockKind.Image, alt, src)
                    }
                }
                val remaining = line.replace(Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE), "").trim()
                if (remaining.isNotBlank()) paragraph += cleanInline(remaining)
                continue
            }

            if (line.contains('|')) {
                flushParagraph()
                table += line
                continue
            }

            val image = Regex("!\\[([^]]*)]\\(([^)]+)\\)").find(line)
            if (image != null && image.range.first == 0 && image.range.last == line.lastIndex) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Image, image.groupValues[1], image.groupValues[2]); continue
            }

            // Preserve inline Markdown for the renderer; only remove HTML tags.
            paragraph += line.replace(Regex("<[^>]+>"), "")
        }

        if (inFence) blocks += MarkdownBlock(MarkdownBlockKind.Code, fenceLines.joinToString("\n"), fenceLanguage)
        flushTable()
        flushParagraph()
        return blocks
    }

    /** Compatibility helper for callers that explicitly need plain text. */
    fun cleanInline(text: String): String = text
        .replace(Regex("""!\\[([^]]*)\\]\\(([^)]+)\\)""")) { it.groupValues[1] }
        .replace(Regex("""\\[([^]]+)\\]\\(([^)]+)\\)""")) { it.groupValues[1] }
        .replace(Regex("""<https?://[^>]+>""")) { it.value.removePrefix("<").removeSuffix(">") }
        .replace(Regex("""`([^`]+)`""")) { it.groupValues[1] }
        .replace(Regex("""\\*\\*([^*]+)\\*\\*""")) { it.groupValues[1] }
        .replace(Regex("""__([^_]+)__""")) { it.groupValues[1] }
        .replace(Regex("""~~([^~]+)~~""")) { it.groupValues[1] }
        .replace(Regex("""(?<!\\*)\\*([^*]+)\\*(?!\\*)""")) { it.groupValues[1] }
        .replace(Regex("""(?<!_)_([^_]+)_(?!_)""")) { it.groupValues[1] }
        .replace(Regex("""<[^>]+>"""), "")
}

enum class MarkdownBlockKind {
    Paragraph,
    Heading,
    UnorderedList,
    OrderedList,
    Task,
    Quote,
    Alert,
    Divider,
    Code,
    Table,
    Image
}

data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val text: String,
    val metadata: Any? = null
)
