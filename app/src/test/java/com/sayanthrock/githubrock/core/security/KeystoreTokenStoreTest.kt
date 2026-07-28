package com.sayanthrock.githubrock.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class KeystoreTokenStoreTest {

    // A simple in-memory implementation of SharedPreferences for testing
    class FakeSharedPreferences : SharedPreferences {
        private val prefs = ConcurrentHashMap<String, Any?>()

        override fun getAll(): MutableMap<String, *> = prefs.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = prefs[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = prefs[key] as? MutableSet<String> ?: defValues

        override fun getInt(key: String, defValue: Int): Int = prefs[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = prefs[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = prefs[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = prefs[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = prefs.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(this, prefs)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    class FakeEditor(
        private val sharedPreferences: FakeSharedPreferences,
        private val prefs: ConcurrentHashMap<String, Any?>
    ) : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply { changes[key] = values }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { changes[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { changes[key] = null }
        override fun clear(): SharedPreferences.Editor = apply { clear = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) {
                prefs.clear()
            }
            for ((key, value) in changes) {
                if (value == null) {
                    prefs.remove(key)
                } else {
                    prefs[key] = value
                }
            }
        }
    }

    @Test
    fun `isStoredClientIdCompatible accepts tokens issued for the configured client`() {
        assertTrue(
            isStoredClientIdCompatible(
                storedClientId = "Ov23lim8WhLjeUMqvuMj",
                configuredClientId = " Ov23lim8WhLjeUMqvuMj "
            )
        )
    }

    @Test
    fun `isStoredClientIdCompatible rejects legacy or different client tokens`() {
        assertFalse(isStoredClientIdCompatible(null, "Ov23lim8WhLjeUMqvuMj"))
        assertFalse(isStoredClientIdCompatible("", "Ov23lim8WhLjeUMqvuMj"))
        assertFalse(
            isStoredClientIdCompatible(
                storedClientId = "Iv23liBz9KwjI8S24igW",
                configuredClientId = "Ov23lim8WhLjeUMqvuMj"
            )
        )
    }

    @Test
    fun `store saves and reads tokens successfully`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")

        val tokens = StoredTokens(
            accessToken = "access",
            refreshToken = "refresh",
            accessExpiresAtEpochSeconds = 123L,
            refreshExpiresAtEpochSeconds = 456L
        )

        store.save(tokens)

        val readTokens = store.read()
        assertEquals(tokens, readTokens)

        // Under the hood, clientId should be saved
        assertEquals("configured_client", prefs.getString("oauth_client_id", null))
    }

    @Test
    fun `store reads null if client ID doesn't match`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")

        val tokens = StoredTokens(
            accessToken = "access",
            refreshToken = "refresh",
            accessExpiresAtEpochSeconds = 123L,
            refreshExpiresAtEpochSeconds = 456L
        )
        store.save(tokens)

        // Change configured client ID
        val storeWithNewClient = KeystoreTokenStore(prefs, "new_client")

        assertNull(storeWithNewClient.read())

        // After failed read, the store should be cleared
        assertFalse(prefs.contains("access_token"))
    }

    @Test
    fun `store handles null optional fields correctly`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")

        val tokens = StoredTokens(
            accessToken = "access",
            refreshToken = null,
            accessExpiresAtEpochSeconds = null,
            refreshExpiresAtEpochSeconds = null
        )

        store.save(tokens)

        val readTokens = store.read()
        assertEquals(tokens, readTokens)

        // Ensure keys for nulls are not stored (putLongOrRemove)
        assertFalse(prefs.contains("access_expiry"))
        assertFalse(prefs.contains("refresh_expiry"))
    }

    @Test
    fun `store clear removes all tokens`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")

        val tokens = StoredTokens(
            accessToken = "access",
            refreshToken = "refresh",
            accessExpiresAtEpochSeconds = 123L,
            refreshExpiresAtEpochSeconds = 456L
        )
        store.save(tokens)

        store.clear()

        assertNull(store.read())
        assertFalse(prefs.contains("oauth_client_id"))
        assertFalse(prefs.contains("access_token"))
    }
}
