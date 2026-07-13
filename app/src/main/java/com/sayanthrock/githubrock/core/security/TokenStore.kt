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

    /**
     * Persists the access and refresh tokens with their expiration times.
     *
     * @param response The OAuth response containing the token data and expiration intervals.
     * @throws IllegalArgumentException If the response does not contain an access token.
     */
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

    /**
 * Retrieves the stored access token.
 *
 * @return The access token, or `null` when none is stored.
 */
fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    /**
 * Retrieves the stored refresh token.
 *
 * @return The refresh token, or `null` when none is stored.
 */
fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    /**
 * Determines whether a stored access token represents an active session.
 *
 * @return `true` if the access token is present and contains non-whitespace characters, `false` otherwise.
 */
fun hasSession(): Boolean = !accessToken().isNullOrBlank()

    /**
         * Determines whether the stored access token has expired.
         *
         * @param now The timestamp used for the expiration comparison, in milliseconds.
         * @return `true` if the access token expiry timestamp is less than or equal to `now`, `false` otherwise.
         */
        fun accessTokenExpired(now: Long = System.currentTimeMillis()): Boolean =
        preferences.getLong(KEY_ACCESS_EXPIRY, Long.MAX_VALUE) <= now

    /**
         * Determines whether the stored refresh token has expired.
         *
         * @param now The timestamp against which the refresh token expiry is checked.
         * @return `true` if the stored expiry time is less than or equal to `now`, `false` otherwise.
         */
        fun refreshTokenExpired(now: Long = System.currentTimeMillis()): Boolean =
        preferences.getLong(KEY_REFRESH_EXPIRY, Long.MAX_VALUE) <= now

    /**
     * Removes all stored token data.
     */
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
