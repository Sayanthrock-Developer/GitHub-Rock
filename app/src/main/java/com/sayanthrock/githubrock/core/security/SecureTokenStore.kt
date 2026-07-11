package com.sayanthrock.githubrock.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessExpiresAtEpochSeconds: Long?,
    val refreshExpiresAtEpochSeconds: Long?
)

interface TokenStore {
    fun read(): StoredTokens?
    fun save(tokens: StoredTokens)
    fun clear()
}

@Singleton
class KeystoreTokenStore @Inject constructor(
    @ApplicationContext context: Context
) : TokenStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "github_rock_secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun read(): StoredTokens? {
        val access = preferences.getString(KEY_ACCESS, null) ?: return null
        return StoredTokens(
            accessToken = access,
            refreshToken = preferences.getString(KEY_REFRESH, null),
            accessExpiresAtEpochSeconds = if (preferences.contains(KEY_ACCESS_EXPIRY)) preferences.getLong(KEY_ACCESS_EXPIRY, 0L) else null,
            refreshExpiresAtEpochSeconds = if (preferences.contains(KEY_REFRESH_EXPIRY)) preferences.getLong(KEY_REFRESH_EXPIRY, 0L) else null
        )
    }

    override fun save(tokens: StoredTokens) {
        preferences.edit().apply {
            putString(KEY_ACCESS, tokens.accessToken)
            putString(KEY_REFRESH, tokens.refreshToken)
            putLongOrRemove(KEY_ACCESS_EXPIRY, tokens.accessExpiresAtEpochSeconds)
            putLongOrRemove(KEY_REFRESH_EXPIRY, tokens.refreshExpiresAtEpochSeconds)
        }.apply()
    }

    override fun clear() = preferences.edit().clear().apply()

    private fun android.content.SharedPreferences.Editor.putLongOrRemove(key: String, value: Long?) {
        if (value == null) remove(key) else putLong(key, value)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCESS_EXPIRY = "access_expiry"
        const val KEY_REFRESH_EXPIRY = "refresh_expiry"
    }
}
