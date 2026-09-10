package com.sayanthrock.githubrock.ui.blur

import android.annotation.TargetApi
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

enum class ApplicationBlurMode { Off, Automatic, Subtle, Medium, Strong; companion object { fun fromStored(value: String?): ApplicationBlurMode = entries.firstOrNull { it.name == value } ?: Automatic } }
enum class ApplicationBlurPreset { None, Clean, LiquidGlass, Frosted, DeepGlass, Custom; companion object { fun fromStored(value: String?): ApplicationBlurPreset = entries.firstOrNull { it.name == value } ?: Clean } }
enum class ApplicationBlurBorder { Off, Subtle, Strong; companion object { fun fromStored(value: String?): ApplicationBlurBorder = entries.firstOrNull { it.name == value } ?: Subtle } }
enum class ApplicationBlurShadow { Off, Soft, Strong; companion object { fun fromStored(value: String?): ApplicationBlurShadow = entries.firstOrNull { it.name == value } ?: Soft } }
enum class ApplicationBlurTint { System, Theme, Custom; companion object { fun fromStored(value: String?): ApplicationBlurTint = entries.firstOrNull { it.name == value } ?: Theme } }
enum class ApplicationBlurComponent { NavigationBar, Dialogs, BottomSheets, DropdownMenus, Popups, TopBars, FloatingButtons, Cards, RepositoryOverlays, ImagePreviews, Notifications, LoadingOverlays }

data class ApplicationBlurProfile(val enabled: Boolean = true, val intensity: Int = 45, val radius: Int = 18, val backgroundDim: Int = 12, val glassOpacity: Int = 72, val saturation: Int = 100, val brightness: Int = 100, val cornerRadius: Int = 24, val border: ApplicationBlurBorder = ApplicationBlurBorder.Subtle, val borderOpacity: Int = 24, val shadow: ApplicationBlurShadow = ApplicationBlurShadow.Soft, val tint: ApplicationBlurTint = ApplicationBlurTint.Theme, val tintOpacity: Int = 10) {
    fun sanitized(): ApplicationBlurProfile = copy(intensity = intensity.coerceIn(0, 100), radius = radius.coerceIn(0, 64), backgroundDim = backgroundDim.coerceIn(0, 100), glassOpacity = glassOpacity.coerceIn(0, 100), saturation = saturation.coerceIn(0, 200), brightness = brightness.coerceIn(0, 200), cornerRadius = cornerRadius.coerceIn(0, 64), borderOpacity = borderOpacity.coerceIn(0, 100), tintOpacity = tintOpacity.coerceIn(0, 100))
}

data class ApplicationBlurSettings(val mode: ApplicationBlurMode = ApplicationBlurMode.Automatic, val preset: ApplicationBlurPreset = ApplicationBlurPreset.Clean, val profile: ApplicationBlurProfile = ApplicationBlurProfile(), val customTintHex: String = "", val components: Map<ApplicationBlurComponent, ApplicationBlurProfile> = emptyMap()) { fun profileFor(component: ApplicationBlurComponent): ApplicationBlurProfile = components[component] ?: profile }

fun ApplicationBlurSettings.effectiveRadius(component: ApplicationBlurComponent? = null): Int {
    if (mode == ApplicationBlurMode.Off) return 0
    val selected = (component?.let(::profileFor) ?: profile).sanitized()
    if (!selected.enabled || selected.intensity == 0) return 0
    val modeMultiplier = when (mode) { ApplicationBlurMode.Off -> 0f; ApplicationBlurMode.Automatic -> 1f; ApplicationBlurMode.Subtle -> 0.55f; ApplicationBlurMode.Medium -> 0.8f; ApplicationBlurMode.Strong -> 1.25f }
    val effective = (selected.radius * modeMultiplier * (selected.intensity / 100f)).toInt()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) effective.coerceIn(0, 64) else 0
}

private fun parseCustomTint(hex: String): Color? = runCatching { val normalized = hex.trim().removePrefix("#"); val argb = when (normalized.length) { 6, 8 -> AndroidColor.parseColor("#$normalized"); else -> return@runCatching null }; Color(argb) }.getOrNull()

@Composable private fun tintColor(profile: ApplicationBlurProfile, customTintHex: String): Color? = when (profile.tint) { ApplicationBlurTint.System -> null; ApplicationBlurTint.Theme -> MaterialTheme.colorScheme.primary; ApplicationBlurTint.Custom -> parseCustomTint(customTintHex) }

/**
 * Reusable glass surface. The [background] slot is deliberately separate from [content]:
 * only the background layer is rendered through RenderEffect, so foreground controls stay crisp.
 *
 * Android's RenderEffect is a layer/content effect, not a backdrop-filter API. Callers that
 * need true backdrop blur must provide the visual background in [background]; this component
 * never pretends that blurring an empty layer is a backdrop blur.
 */
@TargetApi(Build.VERSION_CODES.S)
@Composable
fun ApplicationBlurSurface(settings: ApplicationBlurSettings, component: ApplicationBlurComponent, modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(settings.profileFor(component).sanitized().cornerRadius.dp), background: @Composable BoxScope.() -> Unit = {}, content: @Composable BoxScope.() -> Unit) {
    val profile = settings.profileFor(component).sanitized()
    val radius = settings.effectiveRadius(component)
    if (settings.mode == ApplicationBlurMode.Off || !profile.enabled || profile.intensity == 0) {
        Box(modifier = modifier) { content() }
        return
    }

    val dim = profile.backgroundDim / 100f
    val glassAlpha = profile.glassOpacity / 100f
    val borderAlpha = profile.borderOpacity / 100f
    val borderWidth = when (profile.border) { ApplicationBlurBorder.Off -> 0.dp; ApplicationBlurBorder.Subtle -> 0.5.dp; ApplicationBlurBorder.Strong -> 1.dp }
    val shadowElevation = when (profile.shadow) { ApplicationBlurShadow.Off -> 0.dp; ApplicationBlurShadow.Soft -> 10.dp; ApplicationBlurShadow.Strong -> 20.dp }
    val tint = tintColor(profile, settings.customTintHex)
    val tintAlpha = profile.tintOpacity / 100f
    val brightnessDelta = (profile.brightness - 100) / 100f
    val saturation = profile.saturation / 100f

    Box(modifier = modifier.shadow(shadowElevation, shape, clip = false)) {
        // This is the only layer receiving the expensive RenderEffect. Keeping foreground
        // content outside this layer prevents text/icons/buttons from becoming soft.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (radius > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            var effect: android.graphics.RenderEffect = android.graphics.RenderEffect.createBlurEffect(
                                radius.toFloat(), radius.toFloat(), android.graphics.Shader.TileMode.CLAMP
                            )
                            if (saturation != 1f || brightnessDelta != 0f) {
                                val matrix = ColorMatrix().apply {
                                    setSaturation(saturation)
                                    if (brightnessDelta != 0f) {
                                        val d = brightnessDelta * 255f
                                        postConcat(ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, d, 0f, 1f, 0f, 0f, d, 0f, 0f, 1f, 0f, d, 0f, 0f, 0f, 1f, 0f)))
                                    }
                                }
                                val filterEffect = android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(matrix))
                                effect = android.graphics.RenderEffect.createChainEffect(filterEffect, effect)
                            }
                            renderEffect = effect.asComposeRenderEffect()
                        }
                    } else Modifier
                )
        ) { background() }

        // Glass treatment is independent from the blurred layer so user-configured opacity,
        // tint and dimming remain visible on API 29/30 as a safe non-blur fallback.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(Color.Black.copy(alpha = dim * 0.35f))
                .background(Color.White.copy(alpha = glassAlpha * 0.12f))
                .then(if (tint != null && tintAlpha > 0f) Modifier.background(tint.copy(alpha = tintAlpha)) else Modifier)
                .border(borderWidth, Color.White.copy(alpha = borderAlpha * 0.25f), shape)
        )

        // Foreground remains sharp and interactive.
        Box(modifier = Modifier.matchParentSize()) { content() }
    }
}

fun ApplicationBlurPreset.toSettings(): ApplicationBlurSettings = when (this) {
    ApplicationBlurPreset.None -> ApplicationBlurSettings(mode = ApplicationBlurMode.Off, preset = this, profile = ApplicationBlurProfile(enabled = false, intensity = 0, radius = 0, glassOpacity = 100))
    ApplicationBlurPreset.Clean -> ApplicationBlurSettings(preset = this, profile = ApplicationBlurProfile(intensity = 25, radius = 10, backgroundDim = 6, glassOpacity = 82))
    ApplicationBlurPreset.LiquidGlass -> ApplicationBlurSettings(preset = this, profile = ApplicationBlurProfile(intensity = 60, radius = 24, backgroundDim = 10, glassOpacity = 68, saturation = 115, brightness = 105))
    ApplicationBlurPreset.Frosted -> ApplicationBlurSettings(preset = this, profile = ApplicationBlurProfile(intensity = 75, radius = 32, backgroundDim = 16, glassOpacity = 76, saturation = 92))
    ApplicationBlurPreset.DeepGlass -> ApplicationBlurSettings(preset = this, profile = ApplicationBlurProfile(intensity = 90, radius = 48, backgroundDim = 30, glassOpacity = 56, saturation = 105, brightness = 92, shadow = ApplicationBlurShadow.Strong))
    ApplicationBlurPreset.Custom -> ApplicationBlurSettings(preset = this)
}
