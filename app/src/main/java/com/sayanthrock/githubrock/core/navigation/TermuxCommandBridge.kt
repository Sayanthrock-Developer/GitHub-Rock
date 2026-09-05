package com.sayanthrock.githubrock.core.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.sayanthrock.githubrock.core.util.TermuxCommand
import java.util.UUID

/**
 * The single boundary for GitHub Rock -> Termux command execution.
 * Commands are always explicit, validated, and user-confirmed by the UI before this API is called.
 */
object TermuxCommandBridge {
    const val PACKAGE_NAME = "com.termux"
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val RESULT_ACTION = "com.sayanthrock.githubrock.TERMUX_COMMAND_RESULT"

    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val COMMAND_PATH = "/data/data/com.termux/files/usr/bin/bash"
    private const val DEFAULT_WORKDIR = "/data/data/com.termux/files/home"
    private const val MAX_COMMAND_LENGTH = 8_192
    private const val SESSION_ACTION_SWITCH_TO_NEW_SESSION = 0

    enum class State { Unavailable, Available, Ready, Executing, Success, Failed, Cancelled }

    data class Status(
        val state: State,
        val installed: Boolean,
        val permissionGranted: Boolean,
        val externalAppsEnabled: Boolean = false
    )

    data class Execution(
        val id: String = UUID.randomUUID().toString(),
        val command: String,
        val workingDirectory: String
    )

    fun status(context: Context): Status {
        val installed = isInstalled(context)
        if (!installed) return Status(State.Unavailable, false, false)
        val permission = hasRunCommandPermission(context)
        return Status(if (permission) State.Ready else State.Available, true, permission)
    }

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) != null

    fun hasRunCommandPermission(context: Context): Boolean =
        context.checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun open(context: Context): Result<Unit> = runCatching {
        val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)) {
            "Termux isn't installed"
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun validate(command: String, workingDirectory: String): Result<Execution> = runCatching {
        require(command.isNotBlank()) { "The command is empty" }
        require(command.length <= MAX_COMMAND_LENGTH) { "The command is too long" }
        require(workingDirectory.isNotBlank()) { "Select a Termux project directory" }
        require(!workingDirectory.contains('\n') && !workingDirectory.contains('\r')) {
            "The working directory contains an invalid line break"
        }
        Execution(command = command.trim(), workingDirectory = workingDirectory.trim())
    }

    fun runCommand(context: Context, execution: Execution): Result<Unit> = runCatching {
        check(isInstalled(context)) { "Termux isn't installed" }
        check(hasRunCommandPermission(context)) {
            "Grant GitHub Rock permission to run commands in Termux"
        }
        val workdir = execution.workingDirectory.ifBlank { DEFAULT_WORKDIR }
        val resultIntent = Intent(context, TermuxCommandResultReceiver::class.java).apply {
            action = RESULT_ACTION
            putExtra(TermuxCommandResultReceiver.EXTRA_EXECUTION_ID, execution.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            execution.id.hashCode(),
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(PACKAGE_NAME, RUN_COMMAND_SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", COMMAND_PATH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", execution.command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", workdir)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", SESSION_ACTION_SWITCH_TO_NEW_SESSION)
            putExtra("com.termux.RUN_COMMAND_RESULT_PENDING_INTENT", pendingIntent)
        }
        checkNotNull(context.startService(intent)) { "Termux did not accept the command" }
    }

    fun parseResult(extras: Bundle?): CommandResult? {
        if (extras == null) return null
        val id = extras.getString(TermuxCommandResultReceiver.EXTRA_EXECUTION_ID) ?: return null
        val exit = extras.getInt("com.termux.RUN_COMMAND_RESULT_EXIT_STATUS", Int.MIN_VALUE)
        if (exit == Int.MIN_VALUE) return null
        return CommandResult(
            id = id,
            exitCode = exit,
            stdout = extras.getString("com.termux.RUN_COMMAND_RESULT_STDOUT").orEmpty(),
            stderr = extras.getString("com.termux.RUN_COMMAND_RESULT_STDERR").orEmpty()
        )
    }

    fun userFacingError(error: Throwable): String = when (error) {
        is SecurityException -> "Android or Termux blocked the command. Grant Run commands in Termux permission and enable allow-external-apps=true in Termux."
        is IllegalStateException, is IllegalArgumentException -> error.message ?: "Unable to send the command to Termux"
        else -> "Unable to connect to Termux. Open Termux once, verify permission and external-app settings, then retry."
    }

    data class CommandResult(val id: String, val exitCode: Int, val stdout: String, val stderr: String) {
        val success: Boolean get() = exitCode == 0
    }
}

/** Receives the documented RUN_COMMAND result callback from Termux. */
class TermuxCommandResultReceiver : android.content.BroadcastReceiver() {
    companion object {
        const val EXTRA_EXECUTION_ID = "execution_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val result = TermuxCommandBridge.parseResult(intent.extras) ?: return
        context.sendBroadcast(
            Intent(TermuxCommandBridge.RESULT_ACTION).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_EXECUTION_ID, result.id)
                putExtra("exit_code", result.exitCode)
                putExtra("stdout", result.stdout)
                putExtra("stderr", result.stderr)
            }
        )
    }
}
