package com.sayanthrock.githubrock.core.util

enum class MarkdownBlockKind { Heading, Paragraph, Bullet, Quote, Code, Divider }

data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val text: String,
    val level: Int = 0
)

object MarkdownRenderer {
    private val headingPattern = Regex("^(#{1,6})\\s+(.+)$")
    private val bulletPattern = Regex("^\\s*[-*+]\\s+(.+)$")
    private val orderedPattern = Regex("^\\s*\\d+[.)]\\s+(.+)$")
    private val quotePattern = Regex("^>\\s?(.*)$")
    private val dividerPattern = Regex("^\\s*([-*_]){3,}\\s*$")

    fun render(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val code = StringBuilder()
        var inCode = false
        markdown.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) {
                if (inCode) {
                    blocks += MarkdownBlock(MarkdownBlockKind.Code, code.toString().trimEnd())
                    code.clear()
                }
                inCode = !inCode
                return@forEach
            }
            if (inCode) {
                code.appendLine(line)
                return@forEach
            }
            if (line.isBlank()) return@forEach
            val heading = headingPattern.matchEntire(line)
            when {
                heading != null -> blocks += MarkdownBlock(
                    MarkdownBlockKind.Heading,
                    cleanInline(heading.groupValues[2]),
                    heading.groupValues[1].length
                )
                dividerPattern.matches(line) -> blocks += MarkdownBlock(MarkdownBlockKind.Divider, "")
                bulletPattern.matches(line) -> blocks += MarkdownBlock(
                    MarkdownBlockKind.Bullet,
                    cleanInline(bulletPattern.matchEntire(line)!!.groupValues[1])
                )
                orderedPattern.matches(line) -> blocks += MarkdownBlock(
                    MarkdownBlockKind.Bullet,
                    cleanInline("${line.takeWhile { it.isDigit() }}. ${orderedPattern.matchEntire(line)!!.groupValues[1]}")
                )
                quotePattern.matches(line) -> blocks += MarkdownBlock(
                    MarkdownBlockKind.Quote,
                    cleanInline(quotePattern.matchEntire(line)!!.groupValues[1])
                )
                else -> blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, cleanInline(line))
            }
        }
        if (inCode && code.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Code, code.toString().trimEnd())
        return blocks
    }

    private fun cleanInline(text: String): String =
        text.replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("<[^>]+>"), "")
}
