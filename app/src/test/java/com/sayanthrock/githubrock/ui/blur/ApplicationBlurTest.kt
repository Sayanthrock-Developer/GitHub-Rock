package com.sayanthrock.githubrock.ui.blur

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationBlurTest {
    @Test
    fun profileSanitizedClampsAllUserRanges() {
        val profile = ApplicationBlurProfile(
            intensity = -1,
            radius = 100,
            backgroundDim = 101,
            glassOpacity = -4,
            saturation = 300,
            brightness = -10,
            cornerRadius = 99,
            borderOpacity = 120,
            tintOpacity = -5
        ).sanitized()

        assertEquals(0, profile.intensity)
        assertEquals(64, profile.radius)
        assertEquals(100, profile.backgroundDim)
        assertEquals(0, profile.glassOpacity)
        assertEquals(200, profile.saturation)
        assertEquals(0, profile.brightness)
        assertEquals(64, profile.cornerRadius)
        assertEquals(100, profile.borderOpacity)
        assertEquals(0, profile.tintOpacity)
    }

    @Test
    fun offModeDisablesEffectiveBlur() {
        val settings = ApplicationBlurPreset.LiquidGlass.toSettings().copy(mode = ApplicationBlurMode.Off)
        assertEquals(0, settings.effectiveRadius())
    }

    @Test
    fun presetsProduceExpectedModesAndProfiles() {
        val none = ApplicationBlurPreset.None.toSettings()
        assertEquals(ApplicationBlurMode.Off, none.mode)
        assertFalse(none.profile.enabled)

        val liquid = ApplicationBlurPreset.LiquidGlass.toSettings()
        assertEquals(ApplicationBlurPreset.LiquidGlass, liquid.preset)
        assertTrue(liquid.profile.intensity > 0)
        assertTrue(liquid.profile.radius > 0)
    }

    @Test
    fun componentProfileOverridesGlobalProfile() {
        val global = ApplicationBlurProfile(radius = 10)
        val dialogs = ApplicationBlurProfile(radius = 40)
        val settings = ApplicationBlurSettings(
            profile = global,
            components = mapOf(ApplicationBlurComponent.Dialogs to dialogs)
        )

        assertEquals(10, settings.profileFor(ApplicationBlurComponent.Cards).radius)
        assertEquals(40, settings.profileFor(ApplicationBlurComponent.Dialogs).radius)
    }
}
