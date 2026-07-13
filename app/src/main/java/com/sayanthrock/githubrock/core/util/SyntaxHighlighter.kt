package com.sayanthrock.githubrock.core.util

/**
 * Small, dependency-free syntax tokenizer for the repository editor preview.
 * It intentionally produces spans rather than rendering UI so unsupported
 * languages can fall back to readable plain text without executing anything.
 */
enum class SyntaxTokenKind {
    Keyword,
    String,
    Comment,
    Number,
    Type,
    Tag,
    Attribute,
    Property,
    Markdown
}

data class SyntaxSpan(
    val start: Int,
    val end: Int,
    val kind: SyntaxTokenKind
)

object SyntaxHighlighter {
    private val kotlinJavaExtensions = setOf("kt", "kts", "java", "gradle")
    private val xmlExtensions = setOf("xml", "html", "htm")
    private val jsonYamlExtensions = setOf("json", "yaml", "yml")
    private val markdownExtensions = setOf("md", "markdown")

    private val kotlinJavaKeywords = Regex(
        "\\b(?:as|break|class|const|continue|data|do|else|enum|" +
            "false|for|fun|if|import|in|interface|is|lateinit|" +
            "null|object|open|operator|override|package|private|" +
            "protected|public|return|sealed|super|suspend|this|" +
            "throw|true|try|typealias|typeof|val|var|when|while)\\b"
    )
    private val number = Regex("\\b(?:0[xX][0-9a-fA-F]+|0[bB][01]+|\\d+(?:\\.\\d+)?[fFdDlL]?)\\b")
    private val lineOrBlockComment = Regex("//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/")
    private val quotedString = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
    private val typeName = Regex("\\b[A-Z][A-Za-z0-9_]*\\b")

    private val xmlComment = Regex("<!--[\\s\\S]*?-->")
    private val xmlTag = Regex("</?[A-Za-z_][^>]*>|<\\?[A-Za-z_][^>]*\\?>")
    private val xmlAttribute = Regex("\\b[A-Za-z_:][A-Za-z0-9_.:-]*(?=\\s*=)")

    private val jsonProperty = Regex("\"(?:\\\\.|[^\"\\\\])*\"(?=\\s*:)")
    private val yamlProperty = Regex("(?m)^[ \\t-]*[A-Za-z_][A-Za-z0-9_.-]*(?=\\s*:)")
    private val hashComment = Regex("(?m)(?<!\\S)#.*$")

    private val markdownHeading = Regex("(?m)^#{1,6}\\s+.*$")
    private val markdownCode = Regex("`[^`\\r\\n]+`|```[\\s\\S]*?```")

    fun highlight(fileName: String, source: String): List<SyntaxSpan> {
        if (source.isEmpty()) return emptyList()
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            extension in kotlinJavaExtensions -> highlightKotlinJava(source)
            extension in xmlExtensions -> highlightXml(source)
            extension in jsonYamlExtensions -> highlightJsonYaml(source, extension == "json")
            extension in markdownExtensions -> highlightMarkdown(source)
            else -> emptyList()
        }
    }

    private fun highlightKotlinJava(source: String): List<SyntaxSpan> = tokenize(
        source,
        listOf(
            quotedString to SyntaxTokenKind.String,
            lineOrBlockComment to SyntaxTokenKind.Comment,
            kotlinJavaKeywords to SyntaxTokenKind.Keyword,
            number to SyntaxTokenKind.Number,
            typeName to SyntaxTokenKind.Type
        )
    )

    private fun highlightXml(source: String): List<SyntaxSpan> {
        val spans = tokenize(source, listOf(xmlComment to SyntaxTokenKind.Comment, xmlTag to SyntaxTokenKind.Tag))
        val occupied = spans.filter { it.kind == SyntaxTokenKind.Comment }
        val attributes = xmlAttribute.findAll(source).map { it.range.toSpan(SyntaxTokenKind.Attribute) }
            .filterNot { candidate -> occupied.any { it.overlaps(candidate) } }
        return (spans + attributes).sortedBy { it.start }
    }

    private fun highlightJsonYaml(source: String, json: Boolean): List<SyntaxSpan> {
        val property = if (json) jsonProperty else yamlProperty
        return tokenize(
            source,
            listOf(
                hashComment to SyntaxTokenKind.Comment,
                lineOrBlockComment to SyntaxTokenKind.Comment,
                property to SyntaxTokenKind.Property,
                quotedString to SyntaxTokenKind.String,
                number to SyntaxTokenKind.Number,
                Regex("\\b(?:true|false|null)\\b") to SyntaxTokenKind.Keyword
            )
        ).filterNot { span ->
            span.kind == SyntaxTokenKind.String && property.findAll(source).any { it.range.first == span.start }
        }
    }

    private fun highlightMarkdown(source: String): List<SyntaxSpan> = tokenize(
        source,
        listOf(markdownCode to SyntaxTokenKind.String, markdownHeading to SyntaxTokenKind.Markdown)
    )

    private fun tokenize(source: String, patterns: List<Pair<Regex, SyntaxTokenKind>>): List<SyntaxSpan> {
        val candidates = patterns.flatMap { (regex, kind) ->
            regex.findAll(source).map { it.range.toSpan(kind) }.toList()
        }.sortedWith(compareBy<SyntaxSpan> { it.start }.thenByDescending { it.end - it.start })
        val accepted = mutableListOf<SyntaxSpan>()
        candidates.forEach { candidate ->
            if (candidate.start < candidate.end && accepted.none { it.overlaps(candidate) }) accepted += candidate
        }
        return accepted.sortedBy { it.start }
    }

    private fun IntRange.toSpan(kind: SyntaxTokenKind): SyntaxSpan = SyntaxSpan(first, last + 1, kind)
    private fun SyntaxSpan.overlaps(other: SyntaxSpan): Boolean = start < other.end && other.start < end
}
