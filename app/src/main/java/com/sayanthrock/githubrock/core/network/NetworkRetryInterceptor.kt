package com.sayanthrock.githubrock.core.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Retries only idempotent requests so transient connectivity and GitHub 5xx/429
 * responses do not immediately surface as a broken network state.
 */
class NetworkRetryInterceptor(
    private val maxRetries: Int = 2,
    private val retryDelayMillis: Long = 500L
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isRetryableMethod()) return chain.proceed(request)

        var attempt = 0
        var lastFailure: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (!response.shouldRetry(attempt)) return response

                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                response.close()
                sleepBeforeRetry(attempt, retryAfter)
            } catch (error: IOException) {
                lastFailure = error
                if (attempt == maxRetries) throw error
                sleepBeforeRetry(attempt, null)
            }
            attempt++
        }

        throw lastFailure ?: IOException("Network request failed after retries")
    }

    private fun sleepBeforeRetry(attempt: Int, retryAfterSeconds: Long?) {
        val serverDelay = retryAfterSeconds?.coerceIn(0, 30)?.times(1_000L)
        val exponentialDelay = retryDelayMillis * (1L shl attempt.coerceAtMost(4))
        val delay = serverDelay ?: exponentialDelay
        try {
            TimeUnit.MILLISECONDS.sleep(delay)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Retry interrupted", interrupted)
        }
    }

    private fun Request.isRetryableMethod(): Boolean =
        method == "GET" || method == "HEAD" || method == "OPTIONS"

    private fun Response.shouldRetry(attempt: Int): Boolean =
        attempt < maxRetries && (code == 408 || code == 429 || code in 500..599)
}
