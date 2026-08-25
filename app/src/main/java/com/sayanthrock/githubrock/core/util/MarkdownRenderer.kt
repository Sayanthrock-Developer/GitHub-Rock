package com.sayanthrock.githubrock.core.util

class MarkdownBlockKind private constructor(val name: String) {
    override fun toString(): String = name

    companion object {
        val Heading = MarkdownBlockKind("Heading")
        val Paragraph = MarkdownBlockKind("Paragraph")
        val Bullet = MarkdownBlockKind("Bullet")
        val Quote = MarkdownBlockKind("Quote")
        val Code = MarkdownBlockKind("Code")
        val Divider = MarkdownBlockKind("Divider")
        val Image = MarkdownBlockKind("Image")
        val Table = MarkdownBlockKind("Table")
    }
}

data class MarkdownTable(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val text: String,
    val level: Int = 0,
    val url: String? = null,
    val table: MarkdownTable? = null,
    val ordered: Boolean = false
)

/** GitHub-flavoured Markdown parser used by the native README/release renderer. */
object MarkdownRenderer {
    private val headingPattern = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val bulletPattern = Regex("^\\s*[-*+]\\s+(.+)$")
    private val orderedPattern = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val quotePattern = Regex("^>\\s?(.*)$")
    private val dividerPattern = Regex("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$")
    private val imagePattern = Regex("""^\s*!\[(.*?)\]\((\S+?)(?:\s+\".*?\")?\)\s*$""")
    private val htmlImagePattern = Regex("""^\s*(?:<a\s+[^>]*>\s*)?<img\s+[^>]*?src=[\"']([^\"']+)[\"'][^>]*?(?:alt=[\"']([^\"']*)[\"'])?[^>]*?>\s*(?:</a>\s*)?$""")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)+\\|?\\s*$")

    fun render(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines()
        val buffer = StringBuilder()
        val tableLines = mutableListOf<String>()
        var inCode = false
        var inTable = false

        fun flushParagraph() {
            if (buffer.isNotBlank()) {
                blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, buffer.toString().trim())
                buffer.clear()
            }
        }

        fun splitTableRow(line: String): List<String> = line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .replace("\\|", "\u0000")
            .split('|')
            .map { it.trim().replace("\u0000", "|") }

        fun flushTable() {
            if (!inTable || tableLines.size < 2) {
                inTable = false
                tableLines.clear()
                return
            }
            val headers = splitTableRow(tableLines.first())
            val rows = tableLines.drop(2).map(::splitTableRow).map { row ->
                if (row.size < headers.size) row + List(headers.size - row.size) { "" } else row.take(headers.size)
            }
            blocks += MarkdownBlock(
                kind = MarkdownBlockKind.Table,
                text = "",
                table = MarkdownTable(headers = headers, rows = rows)
            )
            inTable = false
            tableLines.clear()
        }

        fun isTableRow(line: String): Boolean {
            if (!line.contains('|')) return false
            val headers = tableLines.firstOrNull()?.let(::splitTableRow) ?: return false
            return splitTableRow(line).size == headers.size
        }

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()

            if (inCode) {
                if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
                    blocks += MarkdownBlock(MarkdownBlockKind.Code, buffer.toString().trimEnd())
                    buffer.clear()
                    inCode = false
                } else {
                    buffer.appendLine(line)
                }
                return@forEachIndexed
            }

            if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
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

            if (!inTable && line.contains('|') && index + 1 < lines.size && tableSeparator.matches(lines[index + 1])) {
                flushParagraph()
                inTable = true
                tableLines += line
                return@forEachIndexed
            }

            if (inTable) {
                if (isTableRow(line)) {
                    tableLines += line
                    return@forEachIndexed
                }
                flushTable()
            }

            val heading = headingPattern.matchEntire(line)
            val bullet = bulletPattern.matchEntire(line)
            val ordered = orderedPattern.matchEntire(line)
            val quote = quotePattern.matchEntire(line)
            val image = imagePattern.matchEntire(line)
            val htmlImage = htmlImagePattern.matchEntire(line)

            when {
                heading != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Heading, heading.groupValues[2], heading.groupValues[1].length)
                }
                dividerPattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Divider, "")
                }
                bullet != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Bullet, bullet.groupValues[1])
                }
                ordered != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        kind = MarkdownBlockKind.Bullet,
                        text = ordered.groupValues[2],
                        ordered = true,
                        level = ordered.groupValues[1].toIntOrNull() ?: 1
                    )
                }
                quote != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Quote, quote.groupValues[1])
                }
                image != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Image, image.groupValues[1], url = image.groupValues[2])
                }
                htmlImage != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        MarkdownBlockKind.Image,
                        htmlImage.groupValues.getOrElse(2) { "Image" }.ifBlank { "Image" },
                        url = htmlImage.groupValues[1]
                    )
                }
                else -> {
                    if (buffer.isNotEmpty()) buffer.append(' ')
                    buffer.append(line.trimStart())
                }
            }
        }

        if (inCode && buffer.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Code, buffer.toString().trimEnd())
        flushTable()
        flushParagraph()
        return blocks
    }

    /** Compatibility helper for callers that explicitly need plain text. */
    fun cleanInline(text: String): String = text
        .replace(Regex("""!\[([^]]*)\]\(([^)]+)\)"""), "$1")
        .replace(Regex("""\[([^]]+)\]\(([^)]+)\)"""), "$1")
        .replace(Regex("""<https?://[^>]+>""")) { it.value.removePrefix("<").removeSuffix(">") }
        .replace(Regex("""`([^`]+)`"""), "$1")
        .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
        .replace(Regex("""__([^_]+)__"""), "$1")
        .replace(Regex("""~~([^~]+)~~"""), "$1")
        .replace(Regex("""(?<!\*)\*([^*]+)\*(?!\*)"""), "$1")
        .replace(Regex("""(?<!_)_([^_]+)_(?!_)"""), "$1")
        .replace(Regex("""<[^>]+>"""), "")
}
