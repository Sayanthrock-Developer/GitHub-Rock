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

    /**
     * Polls GitHub until the device authorization succeeds or expires.
     *
     * @param device The device authorization details used for polling.
     * @param onStatus Called with user-facing status updates while authorization is pending.
     * @return The stored tokens returned after successful authorization.
     * @throws DeviceFlowException If authorization fails or the device code expires.
     */
    suspend fun poll(device: DeviceCodeResponse, onStatus: (String) -> Unit = {}): StoredTokens {
        val deadline = Instant.now().epochSecond + device.expiresIn
        while (Instant.now().epochSecond < deadline) {
            val response = requestTokenAtAllowedInterval(device)
            response.accessToken?.let { token ->
                return response.toStoredTokens(token).also(tokenStore::save)
            }
            when (response.error) {
                "authorization_pending" -> onStatus("Waiting for approval on GitHub…")
                "slow_down" -> onStatus("GitHub requested slower polling. Still waiting…")
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

    /**
     * Refreshes the stored access token when it is expired or nearing expiration.
     *
     * @return `true` if the session is usable or the access token is refreshed successfully, `false` otherwise.
     */
    suspend fun refreshIfNeeded(): Boolean {
        val stored = tokenStore.read() ?: return false
        val now = Instant.now().epochSecond
        val expiresAt = stored.accessExpiresAtEpochSeconds ?: return true
        if (expiresAt > now + SESSION_EXPIRY_SKEW_SECONDS) return true
        if (!isRefreshTokenUsable(stored.refreshToken, stored.refreshExpiresAtEpochSeconds, now)) return false

        val response = api.refreshToken(
            clientId = BuildConfig.GITHUB_CLIENT_ID,
            refreshToken = requireNotNull(stored.refreshToken)
        )
        val token = response.accessToken ?: return false
        tokenStore.save(response.toStoredTokens(token))
        return true
    }

    fun logout() = tokenStore.clear()

    /**
         * Requests a device token after waiting for the currently required polling interval.
         *
         * @param device The device code used to request the token.
         * @return The device token response from GitHub.
         */
        private suspend fun requestTokenAtAllowedInterval(device: DeviceCodeResponse): DeviceTokenResponse =
        pollMutex.withLock {
            val now = elapsedRealtimeMillis()
            val remainingDelay = remainingPollDelayMillis(
                lastRequestAtMillis = lastTokenRequestAtMillis,
                nowMillis = now,
                intervalSeconds = requiredIntervalSeconds
            )
            if (remainingDelay > 0L) delay(remainingDelay)
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
        const val SESSION_EXPIRY_SKEW_SECONDS = 60L
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

/**
 * Returns monotonic elapsed time in milliseconds.
 *
 * @return The elapsed monotonic time in milliseconds.
 */
private fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L

/**
 * Calculates the polling interval for the next token request.
 *
 * @param currentIntervalSeconds The current polling interval in seconds.
 * @param error The token request error, if any.
 * @param slowDownIncrementSeconds The number of seconds to add when the server requests slower polling.
 * @return The polling interval for the next request in seconds.
 */
internal fun nextPollIntervalSeconds(
    currentIntervalSeconds: Int,
    error: String?,
    slowDownIncrementSeconds: Int = 5
): Int = if (error == "slow_down") {
    currentIntervalSeconds + slowDownIncrementSeconds
} else {
    currentIntervalSeconds
}

/**
     * Determines whether a refresh token can be used at the specified time.
     *
     * @param refreshToken The refresh token to evaluate.
     * @param refreshExpiresAtEpochSeconds The token expiration time, or null when it does not expire.
     * @param nowEpochSeconds The current time in epoch seconds.
     * @return `true` if the token has content and remains valid beyond the 60-second safety window, `false` otherwise.
     */
    internal fun isRefreshTokenUsable(
    refreshToken: String?,
    refreshExpiresAtEpochSeconds: Long?,
    nowEpochSeconds: Long
): Boolean = !refreshToken.isNullOrBlank() &&
    (refreshExpiresAtEpochSeconds == null || refreshExpiresAtEpochSeconds > nowEpochSeconds + 60L)
