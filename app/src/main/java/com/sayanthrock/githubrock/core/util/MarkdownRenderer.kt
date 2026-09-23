package com.sayanthrock.githubrock.core.util

import java.util.Locale

object MarkdownRenderer {
    private val imageRegex = Regex("!\\[([^]]*)\\]\\(([^)]+)\\)")
    private val htmlImageRegex = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val htmlAttrRegex = Regex("\\b([A-Za-z_:][A-Za-z0-9_.:-]*)\\s*=\\s*[\\\"']([^\\\"']*)[\\\"']", RegexOption.IGNORE_CASE)

    fun render(markdown: String): List<MarkdownBlock> {
        val source = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val lines = source.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val table = mutableListOf<String>()
        var inFence = false
        var fenceLanguage: String? = null
        val fenceLines = mutableListOf<String>()
        var index = 0
        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                val text = paragraph.joinToString(" ").trim()
                if (text.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, text)
                paragraph.clear()
            }
        }
        fun flushTable() {
            if (table.isEmpty()) return
            val rows = table.map { it.trim().removePrefix("|").removeSuffix("|").split('|').map(String::trim) }
            val separator = rows.getOrNull(1)
            val isSeparator = separator != null && separator.isNotEmpty() && separator.all { Regex(":?-{3,}:?").matches(it) }
            if (rows.size >= 2 && isSeparator) blocks += MarkdownBlock(MarkdownBlockKind.Table, "", MarkdownTable(rows.first(), rows.drop(2)))
            else paragraph += table
            table.clear()
        }
        fun isBlockHtmlWrapper(line: String) =
            line.trim().matches(Regex("</?(div|p|center|section|article|aside|header|footer|main|figure|figcaption)(\\s[^>]*)?/?>", RegexOption.IGNORE_CASE))
        fun isStandaloneMarkdownPunctuation(line: String) =
            line.trim().matches(Regex("^(#{1,6}|\\*{1,2}|_{1,2})$"))

        while (index < lines.size) {
            val line = lines[index].trimEnd()
            if (inFence) {
                if (line.trim().startsWith("```")) {
                    blocks += MarkdownBlock(MarkdownBlockKind.Code, fenceLines.joinToString("\n"), fenceLanguage)
                    fenceLines.clear(); inFence = false; fenceLanguage = null
                } else fenceLines += line
                index++; continue
            }
            val fence = Regex("^\\s*```(.*)$").find(line)
            if (fence != null) {
                flushTable(); flushParagraph()
                inFence = true; fenceLanguage = fence.groupValues[1].trim().ifBlank { null }
                index++; continue
            }
            if (line.isBlank()) { flushTable(); flushParagraph(); index++; continue }

            if (line.trim().startsWith("<details", true)) {
                flushTable(); flushParagraph()
                val detailLines = mutableListOf<String>()
                var end = index + 1
                while (end < lines.size && !lines[end].trim().equals("</details>", true)) { detailLines += lines[end]; end++ }
                val summary = detailLines.firstOrNull { it.trim().startsWith("<summary", true) }
                    ?.let { Regex("<summary\\b[^>]*>(.*?)</summary>", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
                    ?.let(::cleanInline)?.ifBlank { null } ?: "Details"
                val inner = detailLines.filterNot { it.trim().startsWith("<summary", true) }
                    .joinToString("\n").replace(Regex("</?summary\\b[^>]*>", RegexOption.IGNORE_CASE), "")
                blocks += MarkdownBlock(MarkdownBlockKind.Details, summary, DetailsMetadata(summary, render(inner)))
                index = if (end < lines.size) end + 1 else lines.size
                continue
            }

            if (line.trim().startsWith("<picture", true)) {
                flushTable(); flushParagraph()
                val pictureLines = mutableListOf<String>()
                var end = index + 1
                while (end < lines.size && !lines[end].trim().equals("</picture>", true)) { pictureLines += lines[end]; end++ }
                val html = pictureLines.joinToString("\n")
                fun attrs(tag: String) = htmlAttrRegex.findAll(tag).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                val sources = Regex("<source\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).map { attrs(it.value) }.toList()
                val imgAttrs = htmlImageRegex.find(html)?.value?.let(::attrs).orEmpty()
                val dark = sources.firstOrNull { it["media"].orEmpty().contains("prefers-color-scheme: dark", true) }?.get("srcset")?.let(::firstSrcsetUrl)
                val light = sources.firstOrNull { it["media"].orEmpty().contains("prefers-color-scheme: light", true) }?.get("srcset")
                val fallback = imgAttrs["src"]
                val alt = imgAttrs["alt"].orEmpty()
                if (!dark.isNullOrBlank() || !light.isNullOrBlank() || !fallback.isNullOrBlank())
                    blocks += MarkdownBlock(MarkdownBlockKind.Image, alt, ImageMetadata(fallback, light, dark, alt))
                index = if (end < lines.size) end + 1 else lines.size
                continue
            }

            if (isBlockHtmlWrapper(line)) { index++; continue }
            if (isStandaloneMarkdownPunctuation(line)) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Paragraph, line.trim()); index++; continue
            }
            Regex("^\\s*(#{1,6})\\s+(.+?)\\s*$").find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Heading, it.groupValues[2], it.groupValues[1].length); index++; continue
            }
            if (line.matches(Regex("^\\s*((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$"))) {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Divider, ""); index++; continue
            }
            Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+(.+)$").find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Task, it.groupValues[2], it.groupValues[1].equals("x", true)); index++; continue
            }
            Regex("^\\s*[-*+]\\s+(.+)$").find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Bullet, it.groupValues[1], ListMetadata(false, 1)); index++; continue
            }
            Regex("^\\s*(\\d+)[.)]\\s+(.+)$").find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Bullet, it.groupValues[2], ListMetadata(true, it.groupValues[1].toIntOrNull() ?: 1)); index++; continue
            }
            Regex("^\\s*>\\s*\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]\\s*(.*)$", RegexOption.IGNORE_CASE).find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Alert, it.groupValues[2], it.groupValues[1].uppercase(Locale.ROOT)); index++; continue
            }
            Regex("^\\s*>\\s?(.*)$").find(line)?.let {
                flushTable(); flushParagraph(); blocks += MarkdownBlock(MarkdownBlockKind.Quote, cleanInline(it.groupValues[1])); index++; continue
            }

            val htmlImages = htmlImageRegex.findAll(line).toList()
            if (htmlImages.size > 1) {
                flushTable(); flushParagraph()
                val images = htmlImages.mapNotNull { tag ->
                    val a = htmlAttrRegex.findAll(tag.value).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    a["src"]?.let { ImageMetadata(it, it, it, a["alt"].orEmpty()) }
                }
                if (images.isNotEmpty()) blocks += MarkdownBlock(MarkdownBlockKind.ImageRow, "", images)
                index++; continue
            }
            if (htmlImages.size == 1) {
                flushTable(); flushParagraph()
                val a = htmlAttrRegex.findAll(htmlImages.single().value).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                a["src"]?.let { blocks += MarkdownBlock(MarkdownBlockKind.Image, a["alt"].orEmpty(), ImageMetadata(it, it, it, a["alt"].orEmpty())) }
                val remaining = line.replace(htmlImages.single().value, "").trim()
                if (remaining.isNotBlank()) paragraph += cleanInline(remaining)
                index++; continue
            }

            val images = imageRegex.findAll(line).toList()
            if (images.size > 1) {
                flushTable(); flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockKind.ImageRow, "", images.map { ImageMetadata(it.groupValues[2], it.groupValues[2], it.groupValues[2], it.groupValues[1]) })
                index++; continue
            }
            val image = images.singleOrNull()
            if (image != null && image.range.first == 0 && image.range.last == line.lastIndex) {
                flushTable(); flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockKind.Image, image.groupValues[1], ImageMetadata(image.groupValues[2], image.groupValues[2], image.groupValues[2], image.groupValues[1]))
                index++; continue
            }
            if (line.contains('|')) { flushParagraph(); table += line; index++; continue }
            paragraph += line.replace(Regex("</?(?!script\\b|style\\b)[A-Za-z][^>]*>", RegexOption.IGNORE_CASE), "")
            index++
        }
        if (inFence) blocks += MarkdownBlock(MarkdownBlockKind.Code, fenceLines.joinToString("\n"), fenceLanguage)
        flushTable(); flushParagraph()
        return blocks
    }

    fun cleanInline(text: String): String = decodeHtmlEntities(
        text.replace(Regex("<sup\\b[^>]*>(.*?)</sup>", RegexOption.IGNORE_CASE)) { toSuperscript(it.groupValues[1]) }
            .replace(Regex("<sub\\b[^>]*>(.*?)</sub>", RegexOption.IGNORE_CASE)) { toSubscript(it.groupValues[1]) }
            .replace(imageRegex) { it.groupValues[1] }
            .replace(Regex("\\[([^]]+)\\]\\(([^)]+)\\)")) { it.groupValues[1] }
            .replace(Regex("<https?://[^>]+>")) { it.value.removePrefix("<").removeSuffix(">") }
            .replace(Regex("`([^`]+)`")) { it.groupValues[1] }
            .replace(Regex("\\*\\*([^*]+)\\*\\*")) { it.groupValues[1] }
            .replace(Regex("__([^_]+)__")) { it.groupValues[1] }
            .replace(Regex("~~([^~]+)~~")) { it.groupValues[1] }
            .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")) { it.groupValues[1] }
            .replace(Regex("(?<!_)_([^_]+)_(?!_)")) { it.groupValues[1] }
            .replace(Regex("<[^>]+>"), "")
    )
    private fun firstSrcsetUrl(value: String): String = value.substringBefore(",").trim().substringBefore(" ").trim()

    fun decodeHtmlEntities(text: String): String = text
        .replace("&amp;", "&", true).replace("&lt;", "<", true).replace("&gt;", ">", true)
        .replace("&quot;", "\"", true).replace("&#39;", "'", true).replace("&apos;", "'", true)
    private fun toSuperscript(value: String) = value.map {
        when (it) { '0' -> '⁰'; '1' -> '¹'; '2' -> '²'; '3' -> '³'; '4' -> '⁴'; '5' -> '⁵'; '6' -> '⁶'; '7' -> '⁷'; '8' -> '⁸'; '9' -> '⁹'; '+' -> '⁺'; '-' -> '⁻'; '=' -> '⁼'; '(' -> '⁽'; ')' -> '⁾'; 'n' -> 'ⁿ'; 'i' -> 'ⁱ'; else -> it }
    }.joinToString("")
    private fun toSubscript(value: String) = value.map {
        when (it) { '0' -> '₀'; '1' -> '₁'; '2' -> '₂'; '3' -> '₃'; '4' -> '₄'; '5' -> '₅'; '6' -> '₆'; '7' -> '₇'; '8' -> '₈'; '9' -> '₉'; '+' -> '₊'; '-' -> '₋'; '=' -> '₌'; '(' -> '₍'; ')' -> '₎'; 'a' -> 'ₐ'; 'e' -> 'ₑ'; 'h' -> 'ₕ'; 'i' -> 'ᵢ'; 'j' -> 'ⱼ'; 'k' -> 'ₖ'; 'l' -> 'ₗ'; 'm' -> 'ₘ'; 'n' -> 'ₙ'; 'o' -> 'ₒ'; 'p' -> 'ₚ'; 'r' -> 'ᵣ'; 's' -> 'ₛ'; 't' -> 'ₜ'; 'u' -> 'ᵤ'; 'v' -> 'ᵥ'; 'x' -> 'ₓ'; else -> it }
    }.joinToString("")
}

data class ImageMetadata(val fallbackUrl: String?, val lightUrl: String?, val darkUrl: String?, val alt: String = "")
data class DetailsMetadata(val summary: String, val blocks: List<MarkdownBlock>)

class MarkdownBlockKind private constructor(private val name: String) {
    override fun toString(): String = name
    companion object {
        val Paragraph = MarkdownBlockKind("Paragraph"); val Heading = MarkdownBlockKind("Heading")
        val Bullet = MarkdownBlockKind("Bullet"); val Task = MarkdownBlockKind("Task")
        val Quote = MarkdownBlockKind("Quote"); val Alert = MarkdownBlockKind("Alert")
        val Divider = MarkdownBlockKind("Divider"); val Code = MarkdownBlockKind("Code")
        val Table = MarkdownBlockKind("Table"); val Image = MarkdownBlockKind("Image")
        val ImageRow = MarkdownBlockKind("ImageRow"); val Details = MarkdownBlockKind("Details")
    }
}
data class ListMetadata(val ordered: Boolean, val level: Int)
data class MarkdownTable(val headers: List<String>, val rows: List<List<String>>)
data class MarkdownBlock(val kind: MarkdownBlockKind, val text: String, val metadata: Any? = null) {
    val level: Int get() = when (val value = metadata) { is Int -> value; is ListMetadata -> value.level; else -> 1 }
    val ordered: Boolean get() = (metadata as? ListMetadata)?.ordered ?: false
    val checked: Boolean get() = metadata as? Boolean ?: false
    val codeLanguage: String? get() = metadata as? String
    val url: String? get() = when (val value = metadata) { is String -> value; is ImageMetadata -> value.fallbackUrl; else -> null }
    val image: ImageMetadata? get() = metadata as? ImageMetadata
    val imageRow: List<ImageMetadata> get() = metadata as? List<ImageMetadata> ?: emptyList()
    val details: DetailsMetadata? get() = metadata as? DetailsMetadata
    val table: MarkdownTable? get() = metadata as? MarkdownTable
}
