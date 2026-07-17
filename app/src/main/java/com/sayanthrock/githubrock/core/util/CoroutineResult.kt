package com.sayanthrock.githubrock.core.util

import kotlinx.coroutines.CancellationException

/**
 * Runs a suspending operation as a [Result] without converting coroutine cancellation into failure data.
 */
suspend fun <T> runCatchingPreservingCancellation(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
