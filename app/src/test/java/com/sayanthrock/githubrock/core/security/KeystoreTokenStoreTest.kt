package com.sayanthrock.githubrock.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class KeystoreTokenStoreTest {
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
        override fun edit(): SharedPreferences.Editor = FakeEditor(prefs)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    class FakeEditor(private val prefs: ConcurrentHashMap<String, Any?>) : SharedPreferences.Editor {
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
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) prefs.clear()
            changes.forEach { (key, value) -> if (value == null) prefs.remove(key) else prefs[key] = value }
        }
    }

    private fun tokens(access: String = "access") = StoredTokens(
        accessToken = access,
        refreshToken = "refresh-$access",
        accessExpiresAtEpochSeconds = 123L,
        refreshExpiresAtEpochSeconds = 456L
    )

    @Test
    fun `client compatibility is strict`() {
        assertTrue(isStoredClientIdCompatible("client", " client "))
        assertFalse(isStoredClientIdCompatible(null, "client"))
        assertFalse(isStoredClientIdCompatible("other", "client"))
    }

    @Test
    fun `store saves and reads active account`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens(), login = "alice", name = "Alice", avatarUrl = "https://avatar/alice")

        assertEquals(tokens(), store.read())
        assertEquals("alice", store.activeAccountId())
        assertEquals("Alice", store.accounts().single().name)
        assertEquals("https://avatar/alice", store.accounts().single().avatarUrl)
    }

    @Test
    fun `multiple accounts can be added and switched`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens("one"), login = "alice")
        store.addAccount(tokens("two"), login = "bob")

        assertEquals(2, store.accounts().size)
        assertEquals("bob", store.activeAccountId())
        assertEquals(tokens("two"), store.read())

        assertTrue(store.switchAccount("alice"))
        assertEquals("alice", store.activeAccountId())
        assertEquals(tokens("one"), store.read())
    }

    @Test
    fun `save updates only the active account`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens("one"), login = "alice")
        store.addAccount(tokens("two"), login = "bob")
        store.switchAccount("alice")
        store.save(tokens("updated"))

        assertEquals(tokens("updated"), store.read())
        store.switchAccount("bob")
        assertEquals(tokens("two"), store.read())
    }

    @Test
    fun `removing active account selects another account`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens("one"), login = "alice")
        store.addAccount(tokens("two"), login = "bob")
        store.switchAccount("bob")

        assertTrue(store.removeAccount("bob"))
        assertEquals("alice", store.activeAccountId())
        assertEquals(tokens("one"), store.read())
    }

    @Test
    fun `organization context belongs to the active session`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens(), login = "alice")
        store.setActiveOrganization("rock-org")
        assertEquals("rock-org", store.activeOrganization())
        store.switchAccount("alice")
        assertNull(store.activeOrganization())
    }

    @Test
    fun `clear removes all accounts`() {
        val prefs = FakeSharedPreferences()
        val store = KeystoreTokenStore(prefs, "configured_client")
        store.addAccount(tokens(), login = "alice")
        store.addAccount(tokens("two"), login = "bob")
        store.clear()

        assertTrue(store.accounts().isEmpty())
        assertNull(store.read())
        assertFalse(prefs.contains("oauth_client_id"))
    }
}
