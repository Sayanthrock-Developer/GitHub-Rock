package com.sayanthrock.githubrock.data.auth

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.DeviceTokenResponse
import com.sayanthrock.githubrock.core.security.StoredAccount
import com.sayanthrock.githubrock.core.security.StoredTokens
import com.sayanthrock.githubrock.core.security.TokenStore
import com.sayanthrock.githubrock.core.network.GitHubAuthApi
import com.sayanthrock.githubrock.data.backend.BackendGateway
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceFlowException(message: String) : IllegalStateException(message)

internal const val GITHUB_OAUTH_SCOPES = "repo workflow read:user user:email read:org notifications user:follow"

private enum class DeviceFlowTransport { Backend, DirectGitHub }

@Singleton
class DeviceFlowAuthRepository @Inject constructor(
    private val api: GitHubAuthApi,
    private val tokenStore: TokenStore,
    private val backendGateway: BackendGateway,
) {
    val isConfigured: Boolean get() = backendGateway.isConfigured || BuildConfig.GITHUB_CLIENT_ID.isNotBlank()
    val hasSession: Boolean get() = tokenStore.read() != null
    val accounts: List<StoredAccount> get() = tokenStore.accounts()
    val activeAccountId: String? get() = tokenStore.activeAccountId()
    val activeOrganization: String? get() = tokenStore.activeOrganization()

    private val pollMutex = Mutex()
    private var lastTokenRequestAtMillis = 0L
    private var requiredIntervalSeconds = MINIMUM_POLL_INTERVAL_SECONDS
    @Volatile private var activeTransport = DeviceFlowTransport.DirectGitHub

    suspend fun begin(): DeviceCodeResponse {
        check(isConfigured) { "Connect GitHub Rock Backend or add GITHUB_CLIENT_ID before using GitHub login." }
        val device = if (backendGateway.isConfigured) {
            try {
                backendGateway.startDeviceFlow().also { activeTransport = DeviceFlowTransport.Backend }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (backendFailure: Exception) {
                if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) {
                    throw DeviceFlowException("GitHub Rock Backend could not start login: ${backendFailure.message ?: "connection failed"}")
                }
                requestDirectDeviceCode().also { activeTransport = DeviceFlowTransport.DirectGitHub }
            }
        } else {
            requestDirectDeviceCode().also { activeTransport = DeviceFlowTransport.DirectGitHub }
        }
        return device.also {
            pollMutex.withLock {
                requiredIntervalSeconds = it.interval.coerceAtLeast(MINIMUM_POLL_INTERVAL_SECONDS)
                lastTokenRequestAtMillis = elapsedRealtimeMillis()
            }
        }
    }

    suspend fun startWebAuthorization(state: String): String {
        require(backendGateway.isConfigured) { "Connect GitHub Rock Backend to use browser-based GitHub sign-in." }
        return backendGateway.startWebOAuth(state)
    }

    suspend fun exchangeWebAuthorizationCode(code: String, save: Boolean = true): StoredTokens {
        val response = backendGateway.exchangeWebOAuth(code)
        val token = response.accessToken ?: throw DeviceFlowException(response.errorDescription ?: "GitHub browser authorization failed.")
        return response.toStoredTokens(token).also { tokens -> if (save) tokenStore.save(tokens) }
    }

    suspend fun poll(device: DeviceCodeResponse, save: Boolean = true, onStatus: (String?) -> Unit = {}): StoredTokens {
        val deadline = Instant.now().epochSecond + device.expiresIn
        while (Instant.now().epochSecond < deadline) {
            val response = requestTokenAtAllowedInterval(device)
            response.accessToken?.let { token ->
                return response.toStoredTokens(token).also { tokens -> if (save) tokenStore.save(tokens) }
            }
            when (response.error) {
                "authorization_pending" -> onStatus("Waiting for approval on GitHub…")
                "slow_down" -> onStatus("GitHub requested slower polling. Still waiting…")
                "expired_token" -> throw DeviceFlowException("The device code expired. Start login again.")
                "access_denied" -> throw DeviceFlowException("GitHub login was denied.")
                "device_flow_disabled" -> throw DeviceFlowException("Device Flow is disabled for this GitHub OAuth App. Enable it in the OAuth App settings.")
                "incorrect_client_credentials" -> throw DeviceFlowException("This build or backend has an invalid GitHub OAuth client configuration.")
                else -> throw DeviceFlowException(response.errorDescription ?: "GitHub authentication failed.")
            }
        }
        throw DeviceFlowException("The device code expired. Start login again.")
    }

    fun addAccount(tokens: StoredTokens, login: String? = null, name: String? = null, avatarUrl: String? = null): String =
        tokenStore.addAccount(tokens, login, name, avatarUrl)

    fun updateActiveAccount(login: String, name: String?, avatarUrl: String?) = tokenStore.updateActiveAccount(login, name, avatarUrl)
    fun switchAccount(accountId: String): Boolean = tokenStore.switchAccount(accountId)
    fun removeAccount(accountId: String): Boolean = tokenStore.removeAccount(accountId)
    fun setOrganization(login: String?) = tokenStore.setActiveOrganization(login)

    suspend fun refreshIfNeeded(): Boolean {
        val stored = tokenStore.read() ?: return false
        val now = Instant.now().epochSecond
        val expiresAt = stored.accessExpiresAtEpochSeconds
        if (expiresAt == null || expiresAt > now + SESSION_EXPIRY_SKEW_SECONDS) return true
        if (!isRefreshTokenUsable(stored.refreshToken, stored.refreshExpiresAtEpochSeconds, now)) return false
        val response = refreshThroughBackendOrGitHub(requireNotNull(stored.refreshToken))
        val token = response.accessToken ?: return false
        tokenStore.save(response.toStoredTokens(token))
        return true
    }

    fun logout() = tokenStore.clear()

    private suspend fun requestDirectDeviceCode(): DeviceCodeResponse {
        check(BuildConfig.GITHUB_CLIENT_ID.isNotBlank()) { "Add GITHUB_CLIENT_ID to local.properties before using direct GitHub login." }
        return api.requestDeviceCode(clientId = BuildConfig.GITHUB_CLIENT_ID, scope = GITHUB_OAUTH_SCOPES)
    }

    private suspend fun refreshThroughBackendOrGitHub(refreshToken: String): DeviceTokenResponse {
        if (backendGateway.isConfigured) {
            try {
                val backendResponse = backendGateway.refreshToken(refreshToken)
                if (!backendResponse.accessToken.isNullOrBlank()) return backendResponse
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Direct GitHub remains the availability fallback.
            }
        }
        if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) return DeviceTokenResponse(error = "refresh_unavailable")
        return api.refreshToken(clientId = BuildConfig.GITHUB_CLIENT_ID, refreshToken = refreshToken)
    }

    private suspend fun requestTokenAtAllowedInterval(device: DeviceCodeResponse): DeviceTokenResponse = pollMutex.withLock {
        val now = elapsedRealtimeMillis()
        val remainingDelay = remainingPollDelayMillis(lastTokenRequestAtMillis, now, requiredIntervalSeconds)
        if (remainingDelay > 0L) delay(remainingDelay)
        lastTokenRequestAtMillis = elapsedRealtimeMillis()
        val response = when (activeTransport) {
            DeviceFlowTransport.Backend -> backendGateway.pollDeviceFlow(device.deviceCode)
            DeviceFlowTransport.DirectGitHub -> api.requestToken(BuildConfig.GITHUB_CLIENT_ID, device.deviceCode)
        }
        requiredIntervalSeconds = nextPollIntervalSeconds(requiredIntervalSeconds, response.error, SLOW_DOWN_INCREMENT_SECONDS)
        response
    }

    private fun DeviceTokenResponse.toStoredTokens(token: String): StoredTokens {
        val now = Instant.now().epochSecond
        return StoredTokens(token, refreshToken, expiresIn?.let { now + it }, refreshTokenExpiresIn?.let { now + it })
    }

    private companion object {
        const val MINIMUM_POLL_INTERVAL_SECONDS = 5
        const val SLOW_DOWN_INCREMENT_SECONDS = 5
        const val SESSION_EXPIRY_SKEW_SECONDS = 60L
    }
}

internal fun remainingPollDelayMillis(lastRequestAtMillis: Long, nowMillis: Long, intervalSeconds: Int): Long {
    val intervalMillis = intervalSeconds.coerceAtLeast(0) * 1_000L
    if (lastRequestAtMillis <= 0L) return intervalMillis
    return (intervalMillis - (nowMillis - lastRequestAtMillis)).coerceAtLeast(0L)
}

private fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L

internal fun nextPollIntervalSeconds(currentIntervalSeconds: Int, error: String?, slowDownIncrementSeconds: Int = 5): Int =
    if (error == "slow_down") currentIntervalSeconds + slowDownIncrementSeconds else currentIntervalSeconds

internal fun isRefreshTokenUsable(refreshToken: String?, refreshExpiresAtEpochSeconds: Long?, nowEpochSeconds: Long): Boolean =
    !refreshToken.isNullOrBlank() && (refreshExpiresAtEpochSeconds == null || refreshExpiresAtEpochSeconds > nowEpochSeconds + 60L)
