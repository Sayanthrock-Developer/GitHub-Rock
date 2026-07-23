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

data class BackendConnectionSnapshot(
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
        val health = api.health()
        val config = api.config()
        require(config.apiVersion == SUPPORTED_API_VERSION) {
            "Backend API ${config.apiVersion} is not compatible with this app."
        }
        return BackendConnectionSnapshot(
            endpoint = endpoint,
            health = health,
            config = config,
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

    suspend fun startDeviceFlow(): DeviceCodeResponse {
        val api = apiForConfiguredEndpoint()
        validateBackendForApp(api.config(), "oauthDeviceProxy")
        return api.startDeviceFlow()
    }

    suspend fun pollDeviceFlow(deviceCode: String): DeviceTokenResponse =
        apiForConfiguredEndpoint()
            .pollDeviceFlow(BackendDevicePollRequest(deviceCode))
            .toDeviceTokenResponse()

    suspend fun refreshToken(refreshToken: String): DeviceTokenResponse {
        val api = apiForConfiguredEndpoint()
        validateBackendForApp(api.config(), "oauthRefreshProxy")
        return api.refreshToken(BackendTokenRefreshRequest(refreshToken))
            .toDeviceTokenResponse()
    }

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

    private companion object {
        const val SUPPORTED_API_VERSION = "v1"
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

internal fun validateBackendForApp(
    config: BackendPublicConfigResponse,
    requiredFeature: String,
    currentVersion: String = BuildConfig.VERSION_NAME,
) {
    require(config.apiVersion == "v1") {
        "Backend API ${config.apiVersion} is not compatible with this app."
    }
    require(!config.maintenanceMode) {
        "GitHub Rock Backend is temporarily in maintenance mode."
    }
    require(isVersionAtLeast(currentVersion, config.minSupportedAppVersion)) {
        "GitHub Rock ${config.minSupportedAppVersion} or newer is required by the backend."
    }
    require(config.features[requiredFeature] == true) {
        "Backend feature $requiredFeature is not available."
    }
}

internal fun isVersionAtLeast(current: String, minimum: String): Boolean {
    val currentParts = versionParts(current)
    val minimumParts = versionParts(minimum)
    val width = maxOf(currentParts.size, minimumParts.size)
    return (0 until width).firstNotNullOfOrNull { index ->
        val currentPart = currentParts.getOrElse(index) { 0 }
        val minimumPart = minimumParts.getOrElse(index) { 0 }
        when {
            currentPart > minimumPart -> true
            currentPart < minimumPart -> false
            else -> null
        }
    } ?: true
}

private fun versionParts(value: String): List<Int> = value
    .trim()
    .removePrefix("v")
    .substringBefore('-')
    .split('.')
    .map { it.toIntOrNull() ?: 0 }

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
