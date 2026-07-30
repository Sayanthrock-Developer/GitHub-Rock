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
        val currentParagraph = StringBuilder()

        fun flushParagraph() {
            if (currentParagraph.isNotEmpty()) {
                blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, cleanInline(currentParagraph.toString().trimEnd()))
                currentParagraph.clear()
            }
        }

        markdown.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) {
                flushParagraph()
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
            if (line.isBlank()) {
                flushParagraph()
                return@forEach
            }
            val heading = headingPattern.matchEntire(line)
            when {
                heading != null -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        MarkdownBlockKind.Heading,
                        cleanInline(heading.groupValues[2]),
                        heading.groupValues[1].length
                    )
                }
                dividerPattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(MarkdownBlockKind.Divider, "")
                }
                bulletPattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        MarkdownBlockKind.Bullet,
                        cleanInline(bulletPattern.matchEntire(line)!!.groupValues[1])
                    )
                }
                orderedPattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        MarkdownBlockKind.Bullet,
                        cleanInline("${line.takeWhile { it.isDigit() }}. ${orderedPattern.matchEntire(line)!!.groupValues[1]}")
                    )
                }
                quotePattern.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock(
                        MarkdownBlockKind.Quote,
                        cleanInline(quotePattern.matchEntire(line)!!.groupValues[1])
                    )
                }
                else -> {
                    if (currentParagraph.isNotEmpty()) {
                        currentParagraph.append(" ")
                    }
                    currentParagraph.append(line.trimStart())
                }
            }
        }
        flushParagraph()
        if (inCode && code.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Code, code.toString().trimEnd())
        return blocks
    }

    private fun cleanInline(text: String): String =
        text.replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("<[^>]+>"), "")
}
