package com.sayanthrock.githubrock.data.auth

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.DeviceTokenResponse
import com.sayanthrock.githubrock.core.network.GitHubAuthApi
import com.sayanthrock.githubrock.core.security.StoredTokens
import com.sayanthrock.githubrock.core.security.TokenStore
import kotlinx.coroutines.delay
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

    suspend fun begin(): DeviceCodeResponse {
        check(isConfigured) { "Add GITHUB_CLIENT_ID to local.properties before using GitHub login." }
        return api.requestDeviceCode(
            clientId = BuildConfig.GITHUB_CLIENT_ID,
            scope = "repo read:user user:email workflow write:packages"
        )
    }

    suspend fun poll(device: DeviceCodeResponse, onStatus: (String) -> Unit = {}): StoredTokens {
        var intervalSeconds = device.interval.coerceAtLeast(5)
        val deadline = Instant.now().epochSecond + device.expiresIn
        while (Instant.now().epochSecond < deadline) {
            delay(intervalSeconds * 1_000L)
            val response = api.requestToken(BuildConfig.GITHUB_CLIENT_ID, device.deviceCode)
            response.accessToken?.let { token ->
                return response.toStoredTokens(token).also(tokenStore::save)
            }
            when (response.error) {
                "authorization_pending" -> onStatus("Waiting for approval on GitHub…")
                "slow_down" -> {
                    intervalSeconds += 5
                    onStatus("GitHub requested slower polling. Still waiting…")
                }
                "expired_token" -> throw DeviceFlowException("The device code expired. Start login again.")
                "access_denied" -> throw DeviceFlowException("GitHub login was denied.")
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

    private fun DeviceTokenResponse.toStoredTokens(token: String): StoredTokens {
        val now = Instant.now().epochSecond
        return StoredTokens(
            accessToken = token,
            refreshToken = refreshToken,
            accessExpiresAtEpochSeconds = expiresIn?.let { now + it },
            refreshExpiresAtEpochSeconds = refreshTokenExpiresIn?.let { now + it }
        )
    }
}
