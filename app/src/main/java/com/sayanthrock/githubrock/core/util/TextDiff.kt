package com.sayanthrock.githubrock.core.util

enum class DiffLineKind { Context, Added, Removed }

data class DiffLine(
    val kind: DiffLineKind,
    val text: String
)

object TextDiff {
    private const val MAX_EXACT_LINES = 400

    fun unified(before: String, after: String): List<DiffLine> {
        if (before == after) return emptyList()
        val oldLines = before.lines()
        val newLines = after.lines()
        return if (oldLines.size <= MAX_EXACT_LINES && newLines.size <= MAX_EXACT_LINES) {
            exact(oldLines, newLines)
        } else {
            bounded(oldLines, newLines)
        }
    }

    private fun exact(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val width = newLines.size + 1
        val lcs = Array(oldLines.size + 1) { IntArray(width) }
        for (oldIndex in oldLines.indices.reversed()) {
            for (newIndex in newLines.indices.reversed()) {
                lcs[oldIndex][newIndex] = if (oldLines[oldIndex] == newLines[newIndex]) {
                    lcs[oldIndex + 1][newIndex + 1] + 1
                } else {
                    maxOf(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1])
                }
            }
        }
        val result = mutableListOf<DiffLine>()
        var oldIndex = 0
        var newIndex = 0
        while (oldIndex < oldLines.size && newIndex < newLines.size) {
            when {
                oldLines[oldIndex] == newLines[newIndex] -> {
                    result += DiffLine(DiffLineKind.Context, oldLines[oldIndex])
                    oldIndex++
                    newIndex++
                }
                lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1] -> {
                    result += DiffLine(DiffLineKind.Removed, oldLines[oldIndex++])
                }
                else -> result += DiffLine(DiffLineKind.Added, newLines[newIndex++])
            }
        }
        while (oldIndex < oldLines.size) result += DiffLine(DiffLineKind.Removed, oldLines[oldIndex++])
        while (newIndex < newLines.size) result += DiffLine(DiffLineKind.Added, newLines[newIndex++])
        return result
    }

    private fun bounded(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        var prefix = 0
        while (prefix < oldLines.size && prefix < newLines.size && oldLines[prefix] == newLines[prefix]) prefix++
        var suffix = 0
        while (
            suffix < oldLines.size - prefix &&
            suffix < newLines.size - prefix &&
            oldLines[oldLines.lastIndex - suffix] == newLines[newLines.lastIndex - suffix]
        ) suffix++
        val result = mutableListOf<DiffLine>()
        oldLines.take(prefix).forEach { result += DiffLine(DiffLineKind.Context, it) }
        if (oldLines.size - prefix - suffix > 0) {
            result += DiffLine(DiffLineKind.Removed, "… ${oldLines.size - prefix - suffix} lines removed …")
        }
        if (newLines.size - prefix - suffix > 0) {
            result += DiffLine(DiffLineKind.Added, "… ${newLines.size - prefix - suffix} lines added …")
        }
        oldLines.takeLast(suffix).forEach { result += DiffLine(DiffLineKind.Context, it) }
        return result
    }
}
