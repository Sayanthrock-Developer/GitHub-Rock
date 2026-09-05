package com.sayanthrock.githubrock.core.navigation

import android.content.Context
import android.util.Base64

/** Small local store for optional Termux bridge preferences and recent commands. */
class TermuxBridgeSettings(context: Context) {
    private val preferences = context.getSharedPreferences("termux_command_bridge", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_ENABLED, value).apply()

    var workingDirectory: String
        get() = preferences.getString(KEY_WORKDIR, "") ?: ""
        set(value) = preferences.edit().putString(KEY_WORKDIR, value.trim()).apply()

    fun addHistory(command: String, directory: String, success: Boolean?) {
        val item = listOf(
            encode(command), encode(directory), success?.toString() ?: "running"
        ).joinToString("|")
        val history = history().toMutableList()
        history.removeAll { it.command == command && it.directory == directory && it.success == null }
        history.add(0, HistoryItem(command, directory, success))
        val encoded = history.take(MAX_HISTORY).joinToString("\n") {
            listOf(encode(it.command), encode(it.directory), it.success?.toString() ?: "running").joinToString("|")
        }
        preferences.edit().putString(KEY_HISTORY, encoded).apply()
    }

    fun updateLatest(command: String, directory: String, success: Boolean) {
        val history = history().toMutableList()
        val index = history.indexOfFirst { it.command == command && it.directory == directory && it.success == null }
        if (index >= 0) history[index] = history[index].copy(success = success)
        else history.add(0, HistoryItem(command, directory, success))
        preferences.edit().putString(KEY_HISTORY, history.take(MAX_HISTORY).joinToString("\n") {
            listOf(encode(it.command), encode(it.directory), it.success?.toString() ?: "running").joinToString("|")
        }).apply()
    }

    fun history(): List<HistoryItem> = preferences.getString(KEY_HISTORY, "").orEmpty()
        .lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 3) return@mapNotNull null
            val success = when (parts[2]) { "true" -> true; "false" -> false; else -> null }
            runCatching { HistoryItem(decode(parts[0]), decode(parts[1]), success) }.getOrNull()
        }.toList()

    data class HistoryItem(val command: String, val directory: String, val success: Boolean?)

    private fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun decode(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_WORKDIR = "workdir"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY = 30
    }
}
