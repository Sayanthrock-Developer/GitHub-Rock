package com.sayanthrock.githubrock.data.auth

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.DeviceTokenResponse
import com.sayanthrock.githubrock.core.network.GitHubAuthApi
import com.sayanthrock.githubrock.core.security.StoredTokens
import com.sayanthrock.githubrock.core.security.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

class DeviceFlowException(message: String) : IllegalStateException(message)

@Singleton
class DeviceFlowAuthRepository @Inject constructor(
    private val api: GitHubAuthApi,
    private val tokenStore: TokenStore
) {
    val isConfigured: Boolean get() = BuildConfig.GITHUB_CLIENT_ID.isNotBlank()
    val hasSession: Boolean get() = tokenStore.read() != null

    private val pollMutex = Mutex()
    private var lastTokenRequestAtMillis = 0L
    private var requiredIntervalSeconds = MINIMUM_POLL_INTERVAL_SECONDS

    suspend fun begin(): DeviceCodeResponse {
        check(isConfigured) { "Add GITHUB_CLIENT_ID to local.properties before using GitHub login." }
        return api.requestDeviceCode(
            clientId = BuildConfig.GITHUB_CLIENT_ID
        ).also { device ->
            pollMutex.withLock {
                requiredIntervalSeconds = device.interval.coerceAtLeast(MINIMUM_POLL_INTERVAL_SECONDS)
                lastTokenRequestAtMillis = elapsedRealtimeMillis()
            }
        }
    }

    suspend fun poll(device: DeviceCodeResponse, onStatus: (String) -> Unit = {}): StoredTokens {
        val deadline = Instant.now().epochSecond + device.expiresIn
        while (Instant.now().epochSecond < deadline) {
            val response = requestTokenAtAllowedInterval(device)
            response.accessToken?.let { token ->
                return response.toStoredTokens(token).also(tokenStore::save)
            }
            when (response.error) {
                "authorization_pending" -> onStatus("Waiting for approval on GitHub…")
                "slow_down" -> {
                    onStatus("GitHub requested slower polling. Still waiting…")
                }
                "expired_token" -> throw DeviceFlowException("The device code expired. Start login again.")
                "access_denied" -> throw DeviceFlowException("GitHub login was denied.")
                "device_flow_disabled" -> throw DeviceFlowException(
                    "Device Flow is disabled for this GitHub App. Enable it in the GitHub App settings."
                )
                "incorrect_client_credentials" -> throw DeviceFlowException(
                    "This build has an invalid GitHub App client ID."
                )
                else -> throw DeviceFlowException(response.errorDescription ?: "GitHub authentication failed.")
            }
        }
        throw DeviceFlowException("The device code expired. Start login again.")
    }

    suspend fun refreshIfNeeded(): Boolean {
        val stored = tokenStore.read() ?: return false
        val expiresAt = stored.accessExpiresAtEpochSeconds ?: return true
        if (expiresAt > Instant.now().epochSecond + 60) return true
        val refresh = stored.refreshToken ?: return false
        val response = api.refreshToken(BuildConfig.GITHUB_CLIENT_ID, refreshToken = refresh)
        val token = response.accessToken ?: return false
        tokenStore.save(response.toStoredTokens(token))
        return true
    }

    fun logout() = tokenStore.clear()

    private suspend fun requestTokenAtAllowedInterval(device: DeviceCodeResponse): DeviceTokenResponse =
        pollMutex.withLock {
            val now = elapsedRealtimeMillis()
            val remainingDelay = remainingPollDelayMillis(
                lastRequestAtMillis = lastTokenRequestAtMillis,
                nowMillis = now,
                intervalSeconds = requiredIntervalSeconds
            )
            if (remainingDelay > 0L) {
                delay(remainingDelay)
            }
            lastTokenRequestAtMillis = elapsedRealtimeMillis()
            api.requestToken(BuildConfig.GITHUB_CLIENT_ID, device.deviceCode).also { response ->
                requiredIntervalSeconds = nextPollIntervalSeconds(
                    currentIntervalSeconds = requiredIntervalSeconds,
                    error = response.error,
                    slowDownIncrementSeconds = SLOW_DOWN_INCREMENT_SECONDS
                )
            }
        }

    private fun DeviceTokenResponse.toStoredTokens(token: String): StoredTokens {
        val now = Instant.now().epochSecond
        return StoredTokens(
            accessToken = token,
            refreshToken = refreshToken,
            accessExpiresAtEpochSeconds = expiresIn?.let { now + it },
            refreshExpiresAtEpochSeconds = refreshTokenExpiresIn?.let { now + it }
        )
    }

    private companion object {
        const val MINIMUM_POLL_INTERVAL_SECONDS = 5
        const val SLOW_DOWN_INCREMENT_SECONDS = 5
    }
}

internal fun remainingPollDelayMillis(
    lastRequestAtMillis: Long,
    nowMillis: Long,
    intervalSeconds: Int
): Long {
    val intervalMillis = intervalSeconds.coerceAtLeast(0) * 1_000L
    if (lastRequestAtMillis <= 0L) return intervalMillis
    return (intervalMillis - (nowMillis - lastRequestAtMillis)).coerceAtLeast(0L)
}

private fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L


internal fun nextPollIntervalSeconds(
    currentIntervalSeconds: Int,
    error: String?,
    slowDownIncrementSeconds: Int = 5
): Int = if (error == "slow_down") {
    currentIntervalSeconds + slowDownIncrementSeconds
} else {
    currentIntervalSeconds
}
