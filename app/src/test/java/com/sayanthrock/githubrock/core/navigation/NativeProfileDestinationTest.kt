package com.sayanthrock.githubrock.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeProfileDestinationTest {
    @Test
    fun profileCounterLinksStayInsideTheApp() {
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

    @Test
    fun canonicalProfileLinksOpenTheCorrectNativeSection() {
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Repositories),
            nativeProfileDestination("https://github.com/SayanthRock")
        )
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Followers),
            nativeProfileDestination("https://github.com/SayanthRock/followers")
        )
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Following),
            nativeProfileDestination("https://github.com/SayanthRock/following")
        )
    }

    @Test
    fun followerAndFollowingDestinationsUseNativeRoutes() {
        assertEquals(
            "native-profile/SayanthRock/followers",
            NativeProfileDestination("SayanthRock", NativeProfileSection.Followers).route
        )
        assertEquals(
            "native-profile/SayanthRock/following",
            NativeProfileDestination("SayanthRock", NativeProfileSection.Following).route
        )
    }

    @Test
    fun profileCounterLinksAcceptQueryParametersWithoutChangingSection() {
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Followers),
            nativeProfileDestination("https://github.com/SayanthRock?tab=followers&sort=asc")
        )
        assertEquals(
            NativeProfileDestination("SayanthRock", NativeProfileSection.Following),
            nativeProfileDestination("https://github.com/SayanthRock?sort=asc&tab=following")
        )
    }

    @Test
    fun unrelatedGitHubLinksRemainExplicitlyExternal() {
        assertNull(nativeProfileDestination("https://github.com/settings/security"))
        assertNull(nativeProfileDestination("https://github.com/SayanthRock/issues"))
        assertNull(nativeProfileDestination("https://github.com/SayanthRock/repositories"))
        assertNull(nativeProfileDestination("https://example.com/SayanthRock?tab=followers"))
        assertNull(nativeProfileDestination("https://github.com/SayanthRock?tab=issues"))
    }
}
