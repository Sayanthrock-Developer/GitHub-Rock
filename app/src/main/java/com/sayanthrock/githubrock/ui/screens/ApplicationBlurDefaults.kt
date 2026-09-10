package com.sayanthrock.githubrock.ui.screens

import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurShadow

/** Local compatibility bridge for AppearanceContent's default parameter. */
fun ApplicationBlurPreset.toSettings(): ApplicationBlurSettings = when (this) {
    ApplicationBlurPreset.None -> ApplicationBlurSettings(
        mode = ApplicationBlurMode.Off,
        preset = this,
        profile = ApplicationBlurProfile(enabled = false, intensity = 0, radius = 0, glassOpacity = 100),
    )
    ApplicationBlurPreset.Clean -> ApplicationBlurSettings(
        preset = this,
        profile = ApplicationBlurProfile(intensity = 25, radius = 10, backgroundDim = 6, glassOpacity = 82),
    )
    ApplicationBlurPreset.LiquidGlass -> ApplicationBlurSettings(
        preset = this,
        profile = ApplicationBlurProfile(intensity = 60, radius = 24, backgroundDim = 10, glassOpacity = 68, saturation = 115, brightness = 105),
    )
    ApplicationBlurPreset.Frosted -> ApplicationBlurSettings(
        preset = this,
        profile = ApplicationBlurProfile(intensity = 75, radius = 32, backgroundDim = 16, glassOpacity = 76, saturation = 92),
    )
    ApplicationBlurPreset.DeepGlass -> ApplicationBlurSettings(
        preset = this,
        profile = ApplicationBlurProfile(intensity = 90, radius = 48, backgroundDim = 30, glassOpacity = 56, saturation = 105, brightness = 92, shadow = ApplicationBlurShadow.Strong),
    )
    ApplicationBlurPreset.Custom -> ApplicationBlurSettings(preset = this)
}
