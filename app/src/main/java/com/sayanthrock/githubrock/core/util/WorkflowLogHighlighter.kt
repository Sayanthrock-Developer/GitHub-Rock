package com.sayanthrock.githubrock.core.util

enum class WorkflowLogTokenKind { Timestamp, Command, Success, Warning, Error, Section }

data class WorkflowLogSpan(
    val start: Int,
    val end: Int,
    val kind: WorkflowLogTokenKind
)

data class WorkflowLogSummary(
    val lines: Int,
    val warnings: Int,
    val errors: Int
)

/** Lightweight GitHub Actions log tokenizer used by both log presentation styles. */
object WorkflowLogHighlighter {
    private val timestamp = Regex("^\\uFEFF?\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z")
    private val error = Regex("(?i)(?:##\\[error]|\\berror\\b|\\bfail(?:ed|ure)?\\b|exception)")
    private val warning = Regex("(?i)(?:##\\[warning]|\\bwarning\\b|\\bwarn\\b)")
    private val success = Regex("(?i)(?:\\bsuccess(?:ful|fully)?\\b|\\bpassed\\b|BUILD SUCCESSFUL)")
    private val command = Regex("(?i)(?:##\\[group]Run|\\[command]|^\\s*> Task|^\\s*\\$ )")
    private val section = Regex("(?:##\\[(?:group|endgroup)]|^={3,}|^-{3,})")

    fun highlight(line: String): List<WorkflowLogSpan> {
        if (line.isEmpty()) return emptyList()
        val candidates = buildList {
            timestamp.find(line)?.let { add(it.toSpan(WorkflowLogTokenKind.Timestamp)) }
            section.findAll(line).forEach { add(it.toSpan(WorkflowLogTokenKind.Section)) }
            command.findAll(line).forEach { add(it.toSpan(WorkflowLogTokenKind.Command)) }
            success.findAll(line).forEach { add(it.toSpan(WorkflowLogTokenKind.Success)) }
            warning.findAll(line).forEach { add(it.toSpan(WorkflowLogTokenKind.Warning)) }
            error.findAll(line).forEach { add(it.toSpan(WorkflowLogTokenKind.Error)) }
        }.sortedWith(compareBy<WorkflowLogSpan> { it.start }.thenByDescending { priority(it.kind) })

        val accepted = mutableListOf<WorkflowLogSpan>()
        candidates.forEach { candidate ->
            if (accepted.none { it.start < candidate.end && candidate.start < it.end }) accepted += candidate
        }
        return accepted.sortedBy { it.start }
    }

    fun summary(log: String): WorkflowLogSummary {
        val lines = log.lineSequence().toList()
        return WorkflowLogSummary(
            lines = lines.size,
            warnings = lines.count { warning.containsMatchIn(it) },
            errors = lines.count { error.containsMatchIn(it) }
        )
    }

    private fun MatchResult.toSpan(kind: WorkflowLogTokenKind) =
        WorkflowLogSpan(range.first, range.last + 1, kind)

    private fun priority(kind: WorkflowLogTokenKind): Int = when (kind) {
        WorkflowLogTokenKind.Error -> 6
        WorkflowLogTokenKind.Warning -> 5
        WorkflowLogTokenKind.Success -> 4
        WorkflowLogTokenKind.Command -> 3
        WorkflowLogTokenKind.Section -> 2
        WorkflowLogTokenKind.Timestamp -> 1
    }
}
