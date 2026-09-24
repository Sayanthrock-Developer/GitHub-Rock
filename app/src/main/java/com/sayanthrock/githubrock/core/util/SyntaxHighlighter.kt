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
    private val cLikeExtensions = setOf("js", "jsx", "ts", "tsx", "rs", "swift", "cs", "rb", "sh", "bash", "zsh")
    private val pythonExtensions = setOf("py", "pyw")

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
            extension in pythonExtensions -> highlightPython(source)
            extension in cLikeExtensions -> highlightCStyle(source, extension)
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

        fun isOccupied(candidate: SyntaxSpan): Boolean {
            var low = 0
            var high = occupied.size - 1
            while (low <= high) {
                val mid = (low + high) / 2
                val span = occupied[mid]
                if (span.overlaps(candidate)) return true
                if (candidate.start < span.start) high = mid - 1
                else low = mid + 1
            }
            return false
        }

        val attributes = xmlAttribute.findAll(source).map { it.range.toSpan(SyntaxTokenKind.Attribute) }
            .filterNot { candidate -> isOccupied(candidate) }
        return (spans + attributes).sortedBy { it.start }
    }

    private fun highlightJsonYaml(source: String, json: Boolean): List<SyntaxSpan> {
        val property = if (json) jsonProperty else yamlProperty
        val propertyStarts = property.findAll(source).map { it.range.first }.toSet()

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
            span.kind == SyntaxTokenKind.String && propertyStarts.contains(span.start)
        }
    }

    private fun highlightPython(source: String): List<SyntaxSpan> = tokenize(
        source,
        listOf(
            quotedString to SyntaxTokenKind.String,
            Regex("(?m)#.*$") to SyntaxTokenKind.Comment,
            Regex("\\b(?:and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|match|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b") to SyntaxTokenKind.Keyword,
            number to SyntaxTokenKind.Number,
            typeName to SyntaxTokenKind.Type
        )
    )

    private fun highlightCStyle(source: String, extension: String): List<SyntaxSpan> {
        val keywords = when (extension) {
            "rs" -> Regex("\\b(?:as|break|const|continue|crate|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|trait|true|type|unsafe|use|where|while)\\b")
            "swift" -> Regex("\\b(?:actor|associatedtype|class|defer|enum|extension|false|func|guard|if|import|in|init|let|nil|protocol|return|self|struct|switch|true|typealias|var|while)\\b")
            "cs" -> Regex("\\b(?:abstract|async|await|bool|break|case|catch|class|const|continue|else|enum|false|for|foreach|if|in|interface|internal|namespace|new|null|override|private|protected|public|readonly|return|sealed|static|string|struct|switch|this|throw|true|try|using|var|void|while)\\b")
            "rb" -> Regex("\\b(?:begin|class|def|do|else|elsif|end|false|for|if|in|module|nil|require|rescue|return|self|true|unless|until|when|while|yield)\\b")
            else -> Regex("\\b(?:async|await|break|case|catch|class|const|continue|else|export|false|for|from|function|if|import|in|interface|let|new|null|return|switch|throw|true|try|type|typeof|var|while)\\b")
        }
        val comment = if (extension in setOf("sh", "bash", "zsh", "rb")) Regex("#[^\\r\\n]*") else lineOrBlockComment
        return tokenize(source, listOf(quotedString to SyntaxTokenKind.String, comment to SyntaxTokenKind.Comment, keywords to SyntaxTokenKind.Keyword, number to SyntaxTokenKind.Number, typeName to SyntaxTokenKind.Type))
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
            if (candidate.start < candidate.end && (accepted.isEmpty() || candidate.start >= accepted.last().end)) accepted += candidate
        }
        return accepted.sortedBy { it.start }
    }

    private fun IntRange.toSpan(kind: SyntaxTokenKind): SyntaxSpan = SyntaxSpan(first, last + 1, kind)
    private fun SyntaxSpan.overlaps(other: SyntaxSpan): Boolean = start < other.end && other.start < end
}
