package com.sayanthrock.githubrock.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sayanthrock.githubrock.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessExpiresAtEpochSeconds: Long?,
    val refreshExpiresAtEpochSeconds: Long?
)

data class StoredAccount(
    val id: String,
    val login: String?,
    val name: String?,
    val avatarUrl: String?,
    val tokens: StoredTokens
)

interface TokenStore {
    fun read(): StoredTokens?
    fun save(tokens: StoredTokens)
    fun clear()
    fun accounts(): List<StoredAccount> = emptyList()
    fun activeAccountId(): String? = accounts().firstOrNull()?.id
    fun addAccount(tokens: StoredTokens, login: String? = null, name: String? = null, avatarUrl: String? = null, activate: Boolean = true): String = error("Multi-account storage is not supported")
    fun updateActiveAccount(login: String, name: String?, avatarUrl: String?) {}
    fun switchAccount(accountId: String): Boolean = false
    fun removeAccount(accountId: String): Boolean = false
    fun setActiveOrganization(login: String?) {}
    fun activeOrganization(): String? = null
}

@Singleton
class KeystoreTokenStore internal constructor(
    private val preferences: SharedPreferences,
    private val configuredClientId: String
) : TokenStore {
    @Inject constructor(@ApplicationContext context: Context) : this(
        preferences = EncryptedSharedPreferences.create(
            context,
            "github_rock_secure_tokens",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ),
        configuredClientId = BuildConfig.GITHUB_CLIENT_ID.trim()
    )

    override fun read(): StoredTokens? = activeAccount()?.tokens

    override fun save(tokens: StoredTokens) {
        val active = activeAccount()
        if (active == null) addAccount(tokens) else writeAccount(active.copy(tokens = tokens))
    }

    override fun clear() { preferences.edit().clear().apply() }

    override fun accounts(): List<StoredAccount> {
        migrateLegacyIfNeeded()
        val count = preferences.getInt(KEY_COUNT, 0)
        return (0 until count).mapNotNull { index -> readAccount(idAt(index)) }
    }

    override fun activeAccountId(): String? {
        migrateLegacyIfNeeded()
        return preferences.getString(KEY_ACTIVE_ID, null)?.takeIf { id -> accounts().any { it.id == id } }
            ?: accounts().firstOrNull()?.id
    }

    override fun addAccount(
        tokens: StoredTokens,
        login: String?,
        name: String?,
        avatarUrl: String?,
        activate: Boolean
    ): String {
        migrateLegacyIfNeeded()
        val normalizedLogin = login?.trim()?.removePrefix("@").takeIf { !it.isNullOrBlank() }
        val id = normalizedLogin?.lowercase() ?: "account-${System.currentTimeMillis()}"
        val existingIndex = accounts().indexOfFirst { it.id == id }
        val account = StoredAccount(id, normalizedLogin, name, avatarUrl, tokens)
        if (existingIndex >= 0) {
            writeAccount(account)
        } else {
            val count = preferences.getInt(KEY_COUNT, 0)
            preferences.edit().putInt(KEY_COUNT, count + 1).putString(KEY_ID_PREFIX + count, id).apply()
            writeAccount(account)
        }
        if (activate) preferences.edit().putString(KEY_ACTIVE_ID, id).apply()
        return id
    }

    override fun updateActiveAccount(login: String, name: String?, avatarUrl: String?) {
        val active = activeAccount() ?: return
        val normalized = login.trim().removePrefix("@").ifBlank { active.login ?: active.id }
        writeAccount(active.copy(login = normalized, name = name, avatarUrl = avatarUrl))
    }

    override fun switchAccount(accountId: String): Boolean {
        val currentId = activeAccountId()
        if (accounts().none { it.id == accountId }) return false
        preferences.edit().apply {
            putString(KEY_ACTIVE_ID, accountId)
            // Organization context belongs to the current account session. Never carry it across a switch.
            currentId?.let { remove(organizationKey(it)) }
            remove(organizationKey(accountId))
            remove(KEY_ACTIVE_ORG_LEGACY)
        }.apply()
        return true
    }

    override fun removeAccount(accountId: String): Boolean {
        val current = accounts()
        if (current.none { it.id == accountId }) return false
        val remaining = current.filterNot { it.id == accountId }
        val accountKey = KEY_ACCOUNT_PREFIX + encoded(accountId)
        preferences.edit().apply {
            remove(accountKey + SUFFIX_LOGIN)
            remove(accountKey + SUFFIX_NAME)
            remove(accountKey + SUFFIX_AVATAR)
            remove(accountKey + SUFFIX_ACCESS)
            remove(accountKey + SUFFIX_REFRESH)
            remove(accountKey + SUFFIX_ACCESS_EXPIRY)
            remove(accountKey + SUFFIX_REFRESH_EXPIRY)
            remove(accountKey + SUFFIX_ORGANIZATION)
            putInt(KEY_COUNT, remaining.size)
            for (i in current.indices) remove(KEY_ID_PREFIX + i)
            remaining.forEachIndexed { newIndex, account -> putString(KEY_ID_PREFIX + newIndex, account.id) }
            val activeId = preferences.getString(KEY_ACTIVE_ID, null)
            if (activeId == accountId) {
                if (remaining.isEmpty()) remove(KEY_ACTIVE_ID) else putString(KEY_ACTIVE_ID, remaining.first().id)
            }
        }.apply()
        return true
    }

    override fun setActiveOrganization(login: String?) {
        val activeId = activeAccountId() ?: return
        val value = login?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
        preferences.edit().apply {
            if (value == null) remove(organizationKey(activeId)) else putString(organizationKey(activeId), value)
            remove(KEY_ACTIVE_ORG_LEGACY)
        }.apply()
    }

    override fun activeOrganization(): String? {
        val activeId = activeAccountId() ?: return null
        val key = organizationKey(activeId)
        preferences.getString(key, null)?.let { return it }
        val legacy = preferences.getString(KEY_ACTIVE_ORG_LEGACY, null)?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
        if (legacy != null) {
            preferences.edit().putString(key, legacy).remove(KEY_ACTIVE_ORG_LEGACY).apply()
        }
        return legacy
    }

    private fun activeAccount(): StoredAccount? = accounts().firstOrNull { it.id == activeAccountId() }

    private fun writeAccount(account: StoredAccount) {
        val key = KEY_ACCOUNT_PREFIX + encoded(account.id)
        preferences.edit().apply {
            putString(key + SUFFIX_LOGIN, account.login)
            putString(key + SUFFIX_NAME, account.name)
            putString(key + SUFFIX_AVATAR, account.avatarUrl)
            putString(key + SUFFIX_ACCESS, account.tokens.accessToken)
            putString(key + SUFFIX_REFRESH, account.tokens.refreshToken)
            putLongOrRemove(key + SUFFIX_ACCESS_EXPIRY, account.tokens.accessExpiresAtEpochSeconds)
            putLongOrRemove(key + SUFFIX_REFRESH_EXPIRY, account.tokens.refreshExpiresAtEpochSeconds)
            putString(KEY_CLIENT_ID, configuredClientId)
        }.apply()
    }

    private fun readAccount(id: String?): StoredAccount? {
        if (id.isNullOrBlank()) return null
        val key = KEY_ACCOUNT_PREFIX + encoded(id)
        val access = preferences.getString(key + SUFFIX_ACCESS, null) ?: return null
        val storedClientId = preferences.getString(KEY_CLIENT_ID, null)
        if (!isStoredClientIdCompatible(storedClientId, configuredClientId)) return null
        return StoredAccount(
            id = id,
            login = preferences.getString(key + SUFFIX_LOGIN, null),
            name = preferences.getString(key + SUFFIX_NAME, null),
            avatarUrl = preferences.getString(key + SUFFIX_AVATAR, null),
            tokens = StoredTokens(
                accessToken = access,
                refreshToken = preferences.getString(key + SUFFIX_REFRESH, null),
                accessExpiresAtEpochSeconds = preferences.longOrNull(key + SUFFIX_ACCESS_EXPIRY),
                refreshExpiresAtEpochSeconds = preferences.longOrNull(key + SUFFIX_REFRESH_EXPIRY)
            )
        )
    }

    private fun idAt(index: Int): String? = preferences.getString(KEY_ID_PREFIX + index, null)

    private fun migrateLegacyIfNeeded() {
        if (preferences.getInt(KEY_COUNT, 0) > 0) return
        val legacyAccess = preferences.getString(LEGACY_ACCESS, null) ?: return
        val storedClientId = preferences.getString(LEGACY_CLIENT_ID, null)
        if (!isStoredClientIdCompatible(storedClientId, configuredClientId)) {
            preferences.edit().clear().apply()
            return
        }
        val tokens = StoredTokens(
            accessToken = legacyAccess,
            refreshToken = preferences.getString(LEGACY_REFRESH, null),
            accessExpiresAtEpochSeconds = preferences.longOrNull(LEGACY_ACCESS_EXPIRY),
            refreshExpiresAtEpochSeconds = preferences.longOrNull(LEGACY_REFRESH_EXPIRY)
        )
        val account = "legacy-account"
        preferences.edit()
            .putInt(KEY_COUNT, 1)
            .putString(KEY_ID_PREFIX + 0, account)
            .putString(KEY_ACTIVE_ID, account)
            .remove(LEGACY_ACCESS)
            .remove(LEGACY_REFRESH)
            .remove(LEGACY_ACCESS_EXPIRY)
            .remove(LEGACY_REFRESH_EXPIRY)
            .remove(LEGACY_CLIENT_ID)
            .apply()
        writeAccount(StoredAccount(account, null, null, null, tokens))
    }

    private fun organizationKey(accountId: String): String = KEY_ACCOUNT_PREFIX + encoded(accountId) + SUFFIX_ORGANIZATION
    private fun encoded(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun SharedPreferences.longOrNull(key: String): Long? = if (contains(key)) getLong(key, 0L) else null
    private fun SharedPreferences.Editor.putLongOrRemove(key: String, value: Long?) { if (value == null) remove(key) else putLong(key, value) }

    private companion object {
        const val KEY_CLIENT_ID = "oauth_client_id"
        const val KEY_COUNT = "account_count"
        const val KEY_ACTIVE_ID = "active_account_id"
        const val KEY_ACTIVE_ORG_LEGACY = "active_organization"
        const val KEY_ID_PREFIX = "account_id_"
        const val KEY_ACCOUNT_PREFIX = "account_"
        const val SUFFIX_LOGIN = "_login"
        const val SUFFIX_NAME = "_name"
        const val SUFFIX_AVATAR = "_avatar"
        const val SUFFIX_ACCESS = "_access"
        const val SUFFIX_REFRESH = "_refresh"
        const val SUFFIX_ACCESS_EXPIRY = "_access_expiry"
        const val SUFFIX_REFRESH_EXPIRY = "_refresh_expiry"
        const val SUFFIX_ORGANIZATION = "_organization"
        const val LEGACY_ACCESS = "access_token"
        const val LEGACY_REFRESH = "refresh_token"
        const val LEGACY_ACCESS_EXPIRY = "access_expiry"
        const val LEGACY_REFRESH_EXPIRY = "refresh_expiry"
        const val LEGACY_CLIENT_ID = "oauth_client_id"
    }
}

internal fun isStoredClientIdCompatible(storedClientId: String?, configuredClientId: String): Boolean {
    val configured = configuredClientId.trim()
    return configured.isNotBlank() && storedClientId == configured
}
