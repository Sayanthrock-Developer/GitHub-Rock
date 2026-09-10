package com.sayanthrock.githubrock.ui.blur

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun intensityControlsEffectiveRadius() {
        val base = ApplicationBlurSettings(
            mode = ApplicationBlurMode.Automatic,
            profile = ApplicationBlurProfile(intensity = 100, radius = 40)
        )
        val half = base.copy(profile = base.profile.copy(intensity = 50))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            assertEquals(40, base.effectiveRadius())
            assertEquals(20, half.effectiveRadius())
        } else {
            assertEquals(0, base.effectiveRadius())
            assertEquals(0, half.effectiveRadius())
        }
    }

    @Test
    fun disabledProfileDisablesEffectiveBlur() {
        val settings = ApplicationBlurSettings(
            profile = ApplicationBlurProfile(enabled = false, intensity = 100, radius = 40)
        )
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
