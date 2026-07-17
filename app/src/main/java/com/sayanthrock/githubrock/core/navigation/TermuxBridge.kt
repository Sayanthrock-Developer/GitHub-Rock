package com.sayanthrock.githubrock.core.navigation

import android.content.Context
import android.content.Intent

/** Explicit, user-confirmed bridge to Termux's RUN_COMMAND service. */
object TermuxBridge {
    const val PACKAGE_NAME = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val COMMAND_PATH = "/data/data/com.termux/files/usr/bin/bash"
    private const val HOME_PATH = "/data/data/com.termux/files/home"
    private const val MAX_COMMAND_LENGTH = 8_192
    private const val SESSION_ACTION_SWITCH_TO_NEW_SESSION = 0

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) != null

    fun open(context: Context): Result<Unit> = runCatching {
        val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)) {
            "Termux is not installed"
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun runCommand(context: Context, command: String): Result<Unit> = runCatching {
        require(command.isNotBlank()) { "The command is empty" }
        require(command.length <= MAX_COMMAND_LENGTH) { "The command is too long" }
        check(isInstalled(context)) { "Termux is not installed" }

        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(PACKAGE_NAME, RUN_COMMAND_SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", COMMAND_PATH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME_PATH)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", SESSION_ACTION_SWITCH_TO_NEW_SESSION)
        }
        checkNotNull(context.startService(intent)) {
            "Termux did not accept the command"
        }
    }

    fun userFacingError(error: Throwable): String = when (error) {
        is SecurityException -> "Termux blocked external commands. In Termux, set allow-external-apps=true in ~/.termux/termux.properties, then restart Termux."
        is IllegalStateException, is IllegalArgumentException -> error.message ?: "Unable to send the command to Termux"
        else -> "Unable to connect to Termux. Open Termux once, verify its external-app setting, and retry."
    }
}