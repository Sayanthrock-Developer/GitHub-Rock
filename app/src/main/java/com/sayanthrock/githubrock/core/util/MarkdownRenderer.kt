package com.sayanthrock.githubrock.core.util

enum class MarkdownBlockKind { Heading, Paragraph, Bullet, Quote, Code, Divider, Image }

data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val text: String,
    val level: Int = 0,
    val url: String? = null
)

/** GitHub-flavoured Markdown parser used by the native README/release renderer. */
object MarkdownRenderer {
    private val headingPattern = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val bulletPattern = Regex("^\\s*[-*+]\\s+(.+)$")
    private val orderedPattern = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val quotePattern = Regex("^>\\s?(.*)$")
    private val dividerPattern = Regex("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$")
    private val imagePattern = Regex("""^\s*!\[(.*?)\]\((\S+?)(?:\s+\".*?\")?\)\s*$""")
    private val htmlImagePattern = Regex("""^\s*(?:<a\s+[^>]*>\s*)?<img\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*(?:alt=[\"']([^\"']*)[\"'][^>]*)?>\s*(?:</a>\s*)?$""")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)+\\|?\\s*$")
    private val tableSplitter = Regex("\\s*\\|\\s*")

    fun render(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.replace("\\r\\n", "\\n").replace('\\r', '\\n').lines()
        val paragraph = StringBuilder()
        val code = StringBuilder()
        var inCode = false
        var inTable = false
        var tableText = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, cleanInline(paragraph.toString().trim()))
                paragraph.clear()
            }
        }

        fun flushTable() {
            if (inTable && tableText.isNotBlank()) {
                blocks += MarkdownBlock(MarkdownBlockKind.Code, tableText.toString().trimEnd())
            }
            inTable = false
            tableText.clear()
        }

        fun tableCells(line: String): String = line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split(tableSplitter)
            .joinToString("  |  ") { cleanInline(it.trim()) }

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

            if (!inTable && line.contains('|') && index + 1 < lines.size && tableSeparator.matches(lines[index + 1])) {
                flushParagraph()
                inTable = true
                tableText = StringBuilder(tableCells(line)).appendLine()
                return@forEachIndexed
            }
            if (inTable && line.contains('|')) {
                tableText.appendLine(tableCells(line))
                return@forEachIndexed
            }
            if (inTable) flushTable()

            val heading = headingPattern.matchEntire(line)
            val bullet = bulletPattern.matchEntire(line)
            val ordered = orderedPattern.matchEntire(line)
            val quote = quotePattern.matchEntire(line)
            val image = imagePattern.matchEntire(line)
            val htmlImage = htmlImagePattern.matchEntire(line)

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
                    blocks += MarkdownBlock(MarkdownBlockKind.Bullet, cleanInline("${ordered.groupValues[1]}. ${ordered.groupValues[2]}"))
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

    fun cleanInline(text: String): String = text
        .replace(Regex("""!\[([^]]*)\]\(([^)]+)\)"""), "$1")
        .replace(Regex("""\[([^]]+)\]\(([^)]+)\)"""), "$1 ($2)")
        .replace(Regex("""<https?://[^>]+>""")) { it.value.removePrefix("<").removeSuffix(">") }
        .replace(Regex("""`([^`]+)`"""), "$1")
        .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
        .replace(Regex("""__([^_]+)__"""), "$1")
        .replace(Regex("""(?<!\*)\*([^*]+)\*(?!\*)"""), "$1")
        .replace(Regex("""(?<!_)_([^_]+)_(?!_)"""), "$1")
        .replace(Regex("""<[^>]+>"""), "")
}
