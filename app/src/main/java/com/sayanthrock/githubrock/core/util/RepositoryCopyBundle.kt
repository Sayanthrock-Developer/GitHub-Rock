package com.sayanthrock.githubrock.core.util

import java.util.Locale

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

        val sorted = files.sortedBy { it.path.lowercase(Locale.ROOT) }
        return buildString {
            sorted.forEachIndexed { index, file ->
                if (index > 0) append("\n\n")
                append("===== FILE: ")
                append(escapedPath(file.path))
                append(" =====\n")
                append(file.content)
                if (!file.content.endsWith('\n')) append('\n')
                check(length <= MAX_CHARACTERS) {
                    "The combined text is too large to copy safely"
                }
            }
        }
    }

    private fun escapedPath(path: String): String = buildString(path.length) {
        path.forEach { character ->
            when (character) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.isISOControl()) {
                    append(String.format(Locale.ROOT, "\\u%04X", character.code))
                } else {
                    append(character)
                }
            }
        }
    }
}
