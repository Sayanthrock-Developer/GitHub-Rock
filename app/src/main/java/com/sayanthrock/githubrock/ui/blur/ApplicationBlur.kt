package com.sayanthrock.githubrock.ui.blur

import android.annotation.TargetApi
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

enum class ApplicationBlurMode {
    Off, Automatic, Subtle, Medium, Strong;
    companion object { fun fromStored(value: String?): ApplicationBlurMode = entries.firstOrNull { it.name == value } ?: Automatic }
}

enum class ApplicationBlurPreset {
    None, Clean, LiquidGlass, Frosted, DeepGlass, Custom;
    companion object { fun fromStored(value: String?): ApplicationBlurPreset = entries.firstOrNull { it.name == value } ?: Clean }
}

enum class ApplicationBlurBorder {
    Off, Subtle, Strong;
    companion object { fun fromStored(value: String?): ApplicationBlurBorder = entries.firstOrNull { it.name == value } ?: Subtle }
}

enum class ApplicationBlurShadow {
    Off, Soft, Strong;
    companion object { fun fromStored(value: String?): ApplicationBlurShadow = entries.firstOrNull { it.name == value } ?: Soft }
}

enum class ApplicationBlurTint {
    System, Theme, Custom;
    companion object { fun fromStored(value: String?): ApplicationBlurTint = entries.firstOrNull { it.name == value } ?: Theme }
}

enum class ApplicationBlurComponent {
    NavigationBar, Dialogs, BottomSheets, DropdownMenus, Popups, TopBars,
    FloatingButtons, Cards, RepositoryOverlays, ImagePreviews, Notifications, LoadingOverlays
}

data class ApplicationBlurProfile(
    val enabled: Boolean = true,
    val intensity: Int = 45,
    val radius: Int = 18,
    val backgroundDim: Int = 12,
    val glassOpacity: Int = 72,
    val saturation: Int = 100,
    val brightness: Int = 100,
    val cornerRadius: Int = 24,
    val border: ApplicationBlurBorder = ApplicationBlurBorder.Subtle,
    val borderOpacity: Int = 24,
    val shadow: ApplicationBlurShadow = ApplicationBlurShadow.Soft,
    val tint: ApplicationBlurTint = ApplicationBlurTint.Theme,
    val tintOpacity: Int = 10
) {
    fun sanitized(): ApplicationBlurProfile = copy(
        intensity = intensity.coerceIn(0, 100), radius = radius.coerceIn(0, 64),
        backgroundDim = backgroundDim.coerceIn(0, 100), glassOpacity = glassOpacity.coerceIn(0, 100),
        saturation = saturation.coerceIn(0, 200), brightness = brightness.coerceIn(0, 200),
        cornerRadius = cornerRadius.coerceIn(0, 64), borderOpacity = borderOpacity.coerceIn(0, 100),
        tintOpacity = tintOpacity.coerceIn(0, 100)
    )
}

data class ApplicationBlurSettings(
    val mode: ApplicationBlurMode = ApplicationBlurMode.Automatic,
    val preset: ApplicationBlurPreset = ApplicationBlurPreset.Clean,
    val profile: ApplicationBlurProfile = ApplicationBlurProfile(),
    val customTintHex: String = "",
    val components: Map<ApplicationBlurComponent, ApplicationBlurProfile> = emptyMap()
) {
    fun profileFor(component: ApplicationBlurComponent): ApplicationBlurProfile = components[component] ?: profile
}

fun ApplicationBlurSettings.effectiveRadius(component: ApplicationBlurComponent? = null): Int {
    if (mode == ApplicationBlurMode.Off) return 0
    val selected = (component?.let(::profileFor) ?: profile).sanitized()
    if (!selected.enabled) return 0
    val modeRadius = when (mode) {
        ApplicationBlurMode.Off -> 0
        ApplicationBlurMode.Automatic -> selected.radius
        ApplicationBlurMode.Subtle -> minOf(selected.radius, 12)
        ApplicationBlurMode.Medium -> minOf(selected.radius, 28)
        ApplicationBlurMode.Strong -> selected.radius
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) modeRadius else 0
}

/**
 * Glass surface with a separate background layer. The RenderEffect is applied
 * only to [background], so text, icons and controls in [content] remain sharp.
 * Android 10/11 safely fall back to translucency because RenderEffect requires 12+.
 */
@TargetApi(Build.VERSION_CODES.S)
@Composable
fun ApplicationBlurSurface(
    settings: ApplicationBlurSettings,
    component: ApplicationBlurComponent,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(settings.profileFor(component).sanitized().cornerRadius.dp),
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val profile = settings.profileFor(component).sanitized()
    val radius = settings.effectiveRadius(component)
    val dim = profile.backgroundDim / 100f
    val glassAlpha = profile.glassOpacity / 100f
    val borderAlpha = profile.borderOpacity / 100f
    val borderWidth = when (profile.border) {
        ApplicationBlurBorder.Off -> 0.dp
        ApplicationBlurBorder.Subtle -> 0.5.dp
        ApplicationBlurBorder.Strong -> 1.dp
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (radius > 0) Modifier.graphicsLayer {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            radius.toFloat(), radius.toFloat(), android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    } else Modifier
                )
        ) {
            background()
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(Color.Black.copy(alpha = dim * 0.35f))
                .background(Color.White.copy(alpha = glassAlpha * 0.12f))
                .border(borderWidth, Color.White.copy(alpha = borderAlpha * 0.25f), shape)
        )
        Box(modifier = Modifier.matchParentSize()) {
            content()
        }
    }
}

fun ApplicationBlurPreset.toSettings(): ApplicationBlurSettings = when (this) {
    ApplicationBlurPreset.None -> ApplicationBlurSettings(
        mode = ApplicationBlurMode.Off, preset = this,
        profile = ApplicationBlurProfile(enabled = false, intensity = 0, radius = 0, glassOpacity = 100)
    )
    ApplicationBlurPreset.Clean -> ApplicationBlurSettings(
        preset = this, profile = ApplicationBlurProfile(intensity = 25, radius = 10, backgroundDim = 6, glassOpacity = 82)
    )
    ApplicationBlurPreset.LiquidGlass -> ApplicationBlurSettings(
        preset = this, profile = ApplicationBlurProfile(intensity = 60, radius = 24, backgroundDim = 10, glassOpacity = 68, saturation = 115, brightness = 105)
    )
    ApplicationBlurPreset.Frosted -> ApplicationBlurSettings(
        preset = this, profile = ApplicationBlurProfile(intensity = 75, radius = 32, backgroundDim = 16, glassOpacity = 76, saturation = 92)
    )
    ApplicationBlurPreset.DeepGlass -> ApplicationBlurSettings(
        preset = this, profile = ApplicationBlurProfile(intensity = 90, radius = 48, backgroundDim = 30, glassOpacity = 56, saturation = 105, brightness = 92, shadow = ApplicationBlurShadow.Strong)
    )
    ApplicationBlurPreset.Custom -> ApplicationBlurSettings(preset = this)
}
