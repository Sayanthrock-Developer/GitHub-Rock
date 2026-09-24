package com.sayanthrock.githubrock.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurityAdvisoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes published advisory metadata and severity`() {
        val advisory = json.decodeFromString<SecurityAdvisory>(
            """{"ghsa_id":"GHSA-1234","cve_id":"CVE-2026-1234","html_url":"https://github.com/advisories/GHSA-1234","summary":"Example vulnerability","description":"Details","severity":"high","published_at":"2026-09-24T10:00:00Z","updated_at":"2026-09-24T11:00:00Z"}"""
        )

        assertEquals("GHSA-1234", advisory.ghsaId)
        assertEquals("CVE-2026-1234", advisory.cveId)
        assertEquals("high", advisory.severity)
        assertEquals("Example vulnerability", advisory.summary)
        assertNull(advisory.withdrawnAt)
    }
}