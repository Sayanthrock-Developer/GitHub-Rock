package com.sayanthrock.githubrock.core.auth

import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.DeviceFlowInterpreter
import com.sayanthrock.githubrock.core.model.DeviceFlowResult
import com.sayanthrock.githubrock.core.network.GitHubAuthApi
import com.sayanthrock.githubrock.core.security.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeviceAuthUpdate {
    data class Waiting(val nextPollSeconds: Long) : DeviceAuthUpdate
    data class SlowDown(val nextPollSeconds: Long) : DeviceAuthUpdate
    data object Success : DeviceAuthUpdate
    data class Failure(val message: String) : DeviceAuthUpdate
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: GitHubAuthApi,
    private val tokenStore: TokenStore
) {
    /**
 * Determines whether an authenticated session exists.
 *
 * @return `true` if a session exists, `false` otherwise.
 */
fun hasSession(): Boolean = tokenStore.hasSession()

    /**
         * Determines whether a valid GitHub client ID is configured.
         *
         * @return `true` if the client ID is non-blank and differs from the demo value, `false` otherwise.
         */
        fun isClientConfigured(): Boolean = BuildConfig.GITHUB_CLIENT_ID.isNotBlank() &&
        BuildConfig.GITHUB_CLIENT_ID != "DEMO_CLIENT_ID"

    /**
     * Requests a GitHub device code for authentication.
     *
     * @return The GitHub device-code response.
     */
    suspend fun requestDeviceCode(): DeviceCodeResponse {
        check(isClientConfigured()) {
            "Add the public GitHub App client ID to local.properties before signing in."
        }
        return api.requestDeviceCode(BuildConfig.GITHUB_CLIENT_ID)
    }

    /**
     * Polls GitHub until device authorization succeeds, fails, or expires.
     *
     * @param deviceCode The device authorization details used for polling.
     * @return Updates describing polling status, success, or failure.
     */
    fun pollForToken(deviceCode: DeviceCodeResponse): Flow<DeviceAuthUpdate> = flow {
        var intervalSeconds = deviceCode.interval.coerceAtLeast(5)
        val expiresAt = System.currentTimeMillis() + deviceCode.expiresIn * 1_000L

        while (System.currentTimeMillis() < expiresAt) {
            delay(intervalSeconds * 1_000L)
            val response = api.exchangeToken(
                clientId = BuildConfig.GITHUB_CLIENT_ID,
                grantType = DEVICE_GRANT_TYPE,
                deviceCode = deviceCode.deviceCode
            )

            when (DeviceFlowInterpreter.classify(response)) {
                DeviceFlowResult.SUCCESS -> {
                    tokenStore.save(response)
                    emit(DeviceAuthUpdate.Success)
                    return@flow
                }
                DeviceFlowResult.AUTHORIZATION_PENDING ->
                    emit(DeviceAuthUpdate.Waiting(intervalSeconds))
                DeviceFlowResult.SLOW_DOWN -> {
                    intervalSeconds += 5
                    emit(DeviceAuthUpdate.SlowDown(intervalSeconds))
                }
                DeviceFlowResult.EXPIRED_TOKEN -> {
                    emit(DeviceAuthUpdate.Failure("The verification code expired. Start again."))
                    return@flow
                }
                DeviceFlowResult.ACCESS_DENIED -> {
                    emit(DeviceAuthUpdate.Failure("GitHub sign-in was denied."))
                    return@flow
                }
                DeviceFlowResult.UNKNOWN_ERROR -> {
                    emit(DeviceAuthUpdate.Failure(response.errorDescription ?: "GitHub sign-in failed."))
                    return@flow
                }
            }
        }

        emit(DeviceAuthUpdate.Failure("The verification code expired. Start again."))
    }

    /**
     * Ensures that the stored session has a valid access token.
     *
     * @return `true` if a session is available or successfully refreshed, `false` otherwise.
     */
    suspend fun refreshIfNeeded(): Boolean {
        if (!tokenStore.hasSession()) return false
        if (!tokenStore.accessTokenExpired()) return true

        val refreshToken = tokenStore.refreshToken()
        if (refreshToken.isNullOrBlank() || tokenStore.refreshTokenExpired()) {
            tokenStore.clear()
            return false
        }

        val response = api.exchangeToken(
            clientId = BuildConfig.GITHUB_CLIENT_ID,
            grantType = REFRESH_GRANT_TYPE,
            refreshToken = refreshToken
        )

        return if (!response.accessToken.isNullOrBlank()) {
            tokenStore.save(response)
            true
        } else {
            tokenStore.clear()
            false
        }
    }

    /**
 * Clears the stored authentication session.
 */
fun logout() = tokenStore.clear()

    private companion object {
        const val DEVICE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
        const val REFRESH_GRANT_TYPE = "refresh_token"
    }
}
