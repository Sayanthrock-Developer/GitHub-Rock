package com.sayanthrock.githubrock.core.util

import kotlinx.coroutines.CancellationException

/**
 * Runs a suspending operation as a [Result] without converting coroutine cancellation into failure data.
 */
/**
 * Executes a suspending operation and captures its successful result or failure.
 *
 * Cancellation exceptions are rethrown to preserve coroutine cancellation semantics.
 *
 * @param block The suspending operation to execute.
 * @return A successful result containing the operation's value, or a failed result containing a non-cancellation exception.
 */
suspend fun <T> runCatchingPreservingCancellation(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
