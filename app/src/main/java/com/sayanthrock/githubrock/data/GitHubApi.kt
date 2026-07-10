package com.sayanthrock.githubrock.data

import com.sayanthrock.githubrock.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class DeviceCodeRequest(val client_id: String, val scope: String)

@Serializable
data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int = 5
)

@Serializable
data class AccessTokenRequest(
    val client_id: String,
    val device_code: String,
    val grant_type: String = "urn:ietf:params:oauth:grant-type:device_code"
)

@Serializable
data class AccessTokenResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null,
    val interval: Int? = null
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String
)

interface GitHubOAuthApi {
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun requestDeviceCode(@Body request: DeviceCodeRequest): DeviceCodeResponse

    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun pollAccessToken(@Body request: AccessTokenRequest): AccessTokenResponse
}

interface GitHubRestApi {
    @GET("user")
    suspend fun currentUser(): GitHubUser
}

sealed interface DeviceFlowState {
    data object Idle : DeviceFlowState
    data class AwaitingUser(val code: DeviceCodeResponse) : DeviceFlowState
    data object Pending : DeviceFlowState
    data class SlowDown(val nextIntervalSeconds: Int) : DeviceFlowState
    data object Expired : DeviceFlowState
    data object Denied : DeviceFlowState
    data class Authorized(val token: String) : DeviceFlowState
    data class Failure(val message: String) : DeviceFlowState
}

fun AccessTokenResponse.toState(previousInterval: Int): DeviceFlowState = when {
    access_token != null -> DeviceFlowState.Authorized(access_token)
    error == "authorization_pending" -> DeviceFlowState.Pending
    error == "slow_down" -> DeviceFlowState.SlowDown(interval ?: previousInterval + 5)
    error == "expired_token" -> DeviceFlowState.Expired
    error == "access_denied" -> DeviceFlowState.Denied
    else -> DeviceFlowState.Failure(error_description ?: error ?: "Unknown authentication error")
}

object GitHubConfig {
    val clientId: String get() = BuildConfig.GITHUB_CLIENT_ID
    const val apiVersionHeader = "X-GitHub-Api-Version"
    val apiVersion: String get() = BuildConfig.GITHUB_API_VERSION
}
