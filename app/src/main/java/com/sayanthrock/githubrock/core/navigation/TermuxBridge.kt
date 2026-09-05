package com.sayanthrock.githubrock.core.navigation

import android.content.Context
import com.sayanthrock.githubrock.core.util.TermuxCommand

/**
 * Backwards-compatible facade for the single TermuxCommandBridge implementation.
 * New UI should depend on TermuxCommandBridge directly.
 */
object TermuxBridge {
    const val PACKAGE_NAME = TermuxCommandBridge.PACKAGE_NAME
    const val RUN_COMMAND_PERMISSION = TermuxCommandBridge.RUN_COMMAND_PERMISSION

    fun isInstalled(context: Context): Boolean = TermuxCommandBridge.isInstalled(context)

    fun hasRunCommandPermission(context: Context): Boolean = TermuxCommandBridge.hasRunCommandPermission(context)

    fun open(context: Context): Result<Unit> = TermuxCommandBridge.open(context)

    fun runCommand(context: Context, command: TermuxCommand): Result<Unit> =
        TermuxCommandBridge.validate(command.value, "${'$'}HOME").fold(
            onSuccess = { TermuxCommandBridge.runCommand(context, it) },
            onFailure = { Result.failure(it) }
        )

    fun userFacingError(error: Throwable): String = TermuxCommandBridge.userFacingError(error)
}
