package com.sayanthrock.githubrock.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthClientTokenMigrationTest {
    @Test fun acceptsTokensIssuedForTheConfiguredClient() {
        assertTrue(
            isStoredClientIdCompatible(
                storedClientId = "Ov23lim8WhLjeUMqvuMj",
                configuredClientId = " Ov23lim8WhLjeUMqvuMj "
            )
        )
    }

    @Test fun rejectsLegacyOrDifferentClientTokens() {
        assertFalse(isStoredClientIdCompatible(null, "Ov23lim8WhLjeUMqvuMj"))
        assertFalse(isStoredClientIdCompatible("", "Ov23lim8WhLjeUMqvuMj"))
        assertFalse(
            isStoredClientIdCompatible(
                storedClientId = "Iv23liBz9KwjI8S24igW",
                configuredClientId = "Ov23lim8WhLjeUMqvuMj"
            )
        )
    }
}
