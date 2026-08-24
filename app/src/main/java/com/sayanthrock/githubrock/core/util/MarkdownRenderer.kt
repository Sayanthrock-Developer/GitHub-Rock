package com.sayanthrock.githubrock.core.util

enum class MarkdownBlockKind { Heading, Paragraph, Bullet, Ordered, Quote, Code, Divider, Image, Link, Table }

data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val text: String,
    val level: Int = 0,
    val url: String? = null,
    val rows: List<List<String>> = emptyList()
)

/**
 * Lightweight GitHub-flavoured Markdown parser used by the native README screen.
 * It deliberately keeps content instead of truncating it so long READMEs are rendered
 * completely. Unsupported HTML is kept as readable text rather than silently discarded.
 */
object MarkdownRenderer {
    private val headingPattern = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val bulletPattern = Regex("^\\s*[-*+]\\s+(.+)$")
    private val orderedPattern = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val quotePattern = Regex("^>\\s?(.*)$")
    private val dividerPattern = Regex("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$")
    private val imagePattern = Regex("^\\s*!\\[(.*?)\\]\\((\\S+?)(?:\\s+\\\".*?\\\")?\\)\\s*$")
    private val htmlImagePattern = Regex("^\\s*(?:<a\\s+[^>]*>\\s*)?<img\\s+[^>]*src=[\\\"']([^\\\"']+)[\\\"'][^>]*(?:alt=[\\\"']([^\\\"']*)[\\\"'][^>]*)?>\\s*(?:</a>\\s*)?$")
    private val linkOnlyPattern = Regex("^\\s*\\[([^]]+)]\\(([^)]+)\\)\\s*$")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)+\\|?\\s*$")
    private val tableCellSplitter = Regex("\\s*\\|\\s*")

    fun render(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.replace("\\r\\n", "\\n").replace('\\r', '\\n').lines()
        val paragraph = StringBuilder()
        val code = StringBuilder()
        var inCode = false
        var tableHeader: List<String>? = null
        val tableRows = mutableListOf<List<String>>()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, paragraph.toString().trim())
                paragraph.clear()
            }
        }

        fun flushTable() {
            val header = tableHeader
            if (header != null) {
                blocks += MarkdownBlock(
                    kind = MarkdownBlockKind.Table,
                    text = "",
                    rows = listOf(header) + tableRows.toList()
                )
            }
            tableHeader = null
            tableRows.clear()
        }

        fun cells(line: String): List<String> {
            val value = line.trim().removePrefix("|").removeSuffix("|")
            return value.split(tableCellSplitter).map { cleanInline(it.trim()) }
        }

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()

            if (inCode) {
                if (line.trimStart().startsWith("```")) {
                    blocks += MarkdownBlock(MarkdownBlockKind.Code, code.toString().trimEnd())
                    code.clear()
                    inCode = false
                } else {
                    code.appendLine(line)
                }
                return@forEachIndexed
            }

            if (line.trimStart().startsWith("```")) {
                flushParagraph()
                flushTable()
                inCode = true
                return@forEachIndexed
            }

            if (line.isBlank()) {
                flushParagraph()
                flushTable()
                return@forEachIndexed
            }

            // GitHub pipe tables: header, separator, then data rows.
            if (tableHeader == null && index + 1 < lines.size && line.contains('|') && tableSeparator.matches(lines[index + 1])) {
                flushParagraph()
                tableHeader = cells(line)
                return@forEachIndexed
            }
            if (tableHeader != null && line.contains('|') && !headingPattern.matches(line)) {
                tableRows += cells(line)
                return@forEachIndexed
            }
            if (tableHeader != null) flushTable()

            val heading = headingPattern.matchEntire(line)
            val bullet = bulletPattern.matchEntire(line)
            val ordered = orderedPattern.matchEntire(line)
            val quote = quotePattern.matchEntire(line)
            val image = imagePattern.matchEntire(line)
            val htmlImage = htmlImagePattern.matchEntire(line)
            val link = linkOnlyPattern.matchEntire(line)

            when {
                heading != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Heading, cleanInline(heading.groupValues[2]), heading.groupValues[1].length)
                }
                dividerPattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Divider, "")
                }
                bullet != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Bullet, cleanInline(bullet.groupValues[1]))
                }
                ordered != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Ordered, cleanInline(ordered.groupValues[2]), ordered.groupValues[1].toIntOrNull() ?: 1)
                }
                quote != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Quote, cleanInline(quote.groupValues[1]))
                }
                image != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Image, cleanInline(image.groupValues[1]), url = image.groupValues[2])
                }
                htmlImage != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Image, htmlImage.groupValues.getOrElse(2) { "Image" }.ifBlank { "Image" }, url = htmlImage.groupValues[1])
                }
                link != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Link, cleanInline(link.groupValues[1]), url = link.groupValues[2])
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trimStart())
                }
            }
        }

        if (inCode && code.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Code, code.toString().trimEnd())
        flushTable()
        flushParagraph()
        return blocks
    }

    /** Removes Markdown syntax while preserving the actual link destination. */
    fun cleanInline(text: String): String = text
        .replace(Regex("!\\[([^]]*)]\\(([^)]+)\\)"), "$1")
        .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1 ($2)")
        .replace(Regex("<https?://[^>]+>")) { it.value.removePrefix("<").removeSuffix(">") }
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("__([^_]+)__"), "$1")
        .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
        .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
        .replace(Regex("<[^>]+>"), "")
}
