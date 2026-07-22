package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeProfileDestinationTest {
    @Test fun profileCounterLinksStayInsideTheApp() {
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Repositories),
            nativeProfileDestination("https://github.com/SayanthRock?tab=repositories")
        )
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Followers),
            nativeProfileDestination("https://github.com/SayanthRock?tab=followers")
        )
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Following),
            nativeProfileDestination("https://github.com/SayanthRock?tab=following")
        )
    }

    @Test fun unrelatedGitHubLinksRemainExplicitlyExternal() {
        assertNull(nativeProfileDestination("https://github.com/settings/security"))
        assertNull(nativeProfileDestination("https://github.com/SayanthRock"))
        assertNull(nativeProfileDestination("https://example.com/SayanthRock?tab=followers"))
    }
}
