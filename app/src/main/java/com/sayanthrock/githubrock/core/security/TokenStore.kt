package com.sayanthrock.githubrock.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sayanthrock.githubrock.core.model.AccessTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "github_rock_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Synchronized
    fun save(response: AccessTokenResponse) {
        val accessToken = requireNotNull(response.accessToken) { "Missing access token" }
        val now = System.currentTimeMillis()
        val accessExpiry = response.expiresIn
            ?.let { now + (it * 1_000L) - 60_000L }
            ?: Long.MAX_VALUE
        val refreshExpiry = response.refreshTokenExpiresIn
            ?.let { now + (it * 1_000L) - 60_000L }
            ?: Long.MAX_VALUE

        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putLong(KEY_ACCESS_EXPIRY, accessExpiry)
            .putLong(KEY_REFRESH_EXPIRY, refreshExpiry)
            .apply()
    }

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun hasSession(): Boolean = !accessToken().isNullOrBlank()

    fun accessTokenExpired(now: Long = System.currentTimeMillis()): Boolean =
        preferences.getLong(KEY_ACCESS_EXPIRY, Long.MAX_VALUE) <= now

    fun refreshTokenExpired(now: Long = System.currentTimeMillis()): Boolean =
        preferences.getLong(KEY_REFRESH_EXPIRY, Long.MAX_VALUE) <= now

    @Synchronized
    fun clear() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_EXPIRY = "access_expiry"
        const val KEY_REFRESH_EXPIRY = "refresh_expiry"
    }
}
