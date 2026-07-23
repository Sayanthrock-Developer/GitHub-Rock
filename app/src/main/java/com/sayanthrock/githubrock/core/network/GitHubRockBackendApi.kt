package com.sayanthrock.githubrock.core.network

import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class BackendHealthResponse(
    val status: String,
    val version: String,
    val postgres: String,
    val redis: String,
    val meilisearch: String,
    val timestamp: String,
)

@Serializable
data class BackendPublicConfigResponse(
    val apiVersion: String = "v1",
    val minSupportedAppVersion: String,
    val latestAppVersion: String,
    val maintenanceMode: Boolean,
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class BackendDevicePollRequest(
    @SerialName("device_code") val deviceCode: String,
)

@Serializable
data class BackendTokenRefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class BackendDeviceTokenResponse(
    val state: String,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    val message: String? = null,
    val interval: Int? = null,
)

interface GitHubRockBackendApi {
    @GET("v1/health")
    suspend fun health(): BackendHealthResponse

    @GET("v1/config")
    suspend fun config(): BackendPublicConfigResponse

    @POST("v1/auth/device/start")
    suspend fun startDeviceFlow(): DeviceCodeResponse

    @POST("v1/auth/device/poll")
    suspend fun pollDeviceFlow(
        @Body request: BackendDevicePollRequest,
    ): BackendDeviceTokenResponse

    @POST("v1/auth/device/refresh")
    suspend fun refreshToken(
        @Body request: BackendTokenRefreshRequest,
    ): BackendDeviceTokenResponse
}
