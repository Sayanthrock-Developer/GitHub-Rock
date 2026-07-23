package com.sayanthrock.githubrock.data.backend

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sayanthrock.githubrock.BuildConfig
import com.sayanthrock.githubrock.core.model.DeviceCodeResponse
import com.sayanthrock.githubrock.core.model.DeviceTokenResponse
import com.sayanthrock.githubrock.core.network.BackendDevicePollRequest
import com.sayanthrock.githubrock.core.network.BackendDeviceTokenResponse
import com.sayanthrock.githubrock.core.network.BackendHealthResponse
import com.sayanthrock.githubrock.core.network.BackendPublicConfigResponse
import com.sayanthrock.githubrock.core.network.BackendTokenRefreshRequest
import com.sayanthrock.githubrock.core.network.GitHubRockBackendApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal data class BackendConnectionSnapshot(
    val endpoint: String,
    val health: BackendHealthResponse,
    val config: BackendPublicConfigResponse,
)

@Singleton
class BackendEndpointStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun endpoint(): String? = normalizedBackendBaseUrl(
        preferences.getString(KEY_ENDPOINT, null) ?: BuildConfig.BACKEND_BASE_URL,
    )

    fun save(rawEndpoint: String): String {
        val endpoint = requireNotNull(normalizedBackendBaseUrl(rawEndpoint)) {
            "Enter the HTTPS URL of the deployed GitHub Rock Backend."
        }
        preferences.edit().putString(KEY_ENDPOINT, endpoint).apply()
        return endpoint
    }

    fun clear() {
        preferences.edit().remove(KEY_ENDPOINT).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "github_rock_backend"
        const val KEY_ENDPOINT = "base_url"
    }
}

@Singleton
class BackendGateway @Inject constructor(
    private val json: Json,
    @Named("authClient") private val client: OkHttpClient,
    private val endpointStore: BackendEndpointStore,
) {
    @Volatile private var cachedEndpoint: String? = null
    @Volatile private var cachedApi: GitHubRockBackendApi? = null

    val isConfigured: Boolean get() = endpointStore.endpoint() != null
    val configuredEndpoint: String? get() = endpointStore.endpoint()

    suspend fun check(rawEndpoint: String? = null): BackendConnectionSnapshot {
        val endpoint = rawEndpoint?.let(::requireBackendBaseUrl)
            ?: requireNotNull(endpointStore.endpoint()) { "GitHub Rock Backend is not connected." }
        val api = api(endpoint)
        return BackendConnectionSnapshot(
            endpoint = endpoint,
            health = api.health(),
            config = api.config(),
        )
    }

    suspend fun saveAndCheck(rawEndpoint: String): BackendConnectionSnapshot {
        val endpoint = requireBackendBaseUrl(rawEndpoint)
        val snapshot = check(endpoint)
        endpointStore.save(endpoint)
        return snapshot
    }

    fun disconnect() {
        endpointStore.clear()
        synchronized(this) {
            cachedEndpoint = null
            cachedApi = null
        }
    }

    suspend fun startDeviceFlow(): DeviceCodeResponse =
        apiForConfiguredEndpoint().startDeviceFlow()

    suspend fun pollDeviceFlow(deviceCode: String): DeviceTokenResponse =
        apiForConfiguredEndpoint()
            .pollDeviceFlow(BackendDevicePollRequest(deviceCode))
            .toDeviceTokenResponse()

    suspend fun refreshToken(refreshToken: String): DeviceTokenResponse =
        apiForConfiguredEndpoint()
            .refreshToken(BackendTokenRefreshRequest(refreshToken))
            .toDeviceTokenResponse()

    private fun apiForConfiguredEndpoint(): GitHubRockBackendApi {
        val endpoint = requireNotNull(endpointStore.endpoint()) {
            "GitHub Rock Backend is not connected."
        }
        return api(endpoint)
    }

    private fun api(endpoint: String): GitHubRockBackendApi {
        cachedApi?.takeIf { cachedEndpoint == endpoint }?.let { return it }
        return synchronized(this) {
            cachedApi?.takeIf { cachedEndpoint == endpoint } ?: Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GitHubRockBackendApi::class.java)
                .also {
                    cachedEndpoint = endpoint
                    cachedApi = it
                }
        }
    }
}

internal fun normalizedBackendBaseUrl(raw: String?): String? {
    val candidate = raw?.trim()?.trimEnd('/').orEmpty()
    if (candidate.isBlank()) return null
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null) return null
    return "$candidate/"
}

internal fun requireBackendBaseUrl(raw: String): String =
    requireNotNull(normalizedBackendBaseUrl(raw)) {
        "Use a valid HTTPS backend URL without query parameters or credentials."
    }

internal fun BackendDeviceTokenResponse.toDeviceTokenResponse(): DeviceTokenResponse = when (state) {
    "authorized" -> DeviceTokenResponse(
        accessToken = accessToken,
        tokenType = tokenType,
        scope = scope,
        expiresIn = expiresIn,
        refreshToken = refreshToken,
        refreshTokenExpiresIn = refreshTokenExpiresIn,
    )
    "pending" -> DeviceTokenResponse(
        error = "authorization_pending",
        errorDescription = message,
    )
    "slow_down" -> DeviceTokenResponse(
        error = "slow_down",
        errorDescription = message,
    )
    "expired" -> DeviceTokenResponse(
        error = "expired_token",
        errorDescription = message,
    )
    "denied" -> DeviceTokenResponse(
        error = "access_denied",
        errorDescription = message,
    )
    else -> DeviceTokenResponse(
        error = "backend_error",
        errorDescription = message ?: "GitHub Rock Backend could not complete authentication.",
    )
}
