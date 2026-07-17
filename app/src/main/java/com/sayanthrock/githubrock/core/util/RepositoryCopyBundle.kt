package com.sayanthrock.githubrock.core.util

data class CopyableRepositoryFile(
    val path: String,
    val content: String
)

object RepositoryCopyBundle {
    const val MAX_FILES = 30
    const val MAX_CHARACTERS = 1_500_000

    fun build(files: List<CopyableRepositoryFile>): String {
        require(files.isNotEmpty()) { "No text or code files are available to copy" }
        require(files.size <= MAX_FILES) { "Copy up to $MAX_FILES visible text files at a time" }

        val sorted = files.sortedBy { it.path.lowercase() }
        val bundle = buildString {
            sorted.forEachIndexed { index, file ->
                if (index > 0) append("\n\n")
                append("===== FILE: ")
                append(file.path)
                append(" =====\n")
                append(file.content)
                if (!file.content.endsWith('\n')) append('\n')
                check(length <= MAX_CHARACTERS) {
                    "The combined text is too large to copy safely"
                }
            }
        }
        return bundle
    }
}
