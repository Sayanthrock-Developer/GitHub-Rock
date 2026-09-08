package com.sayanthrock.githubrock.ui.theme

import androidx.compose.ui.graphics.Color
import com.sayanthrock.githubrock.data.settings.AccentColor
import kotlin.math.max
import kotlin.math.min

/** Resolved colors used by the Material 3 theme and the Appearance preview. */
data class AccentTonalPalette(
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnPrimary: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnPrimary: Color,
    val darkOnContainer: Color
)

private data class Hsl(val h: Float, val s: Float, val l: Float)

fun AccentColor.seedColor(): Color = when (this) {
    AccentColor.DefaultGitHubRock -> Color(0xFFE60023)
    AccentColor.Red -> Color(0xFFE53935)
    AccentColor.Orange -> Color(0xFFFF7A00)
    AccentColor.Yellow -> Color(0xFFE6A700)
    AccentColor.Green -> Color(0xFF2E9B50)
    AccentColor.Teal -> Color(0xFF00897B)
    AccentColor.Cyan -> Color(0xFF00A6B2)
    AccentColor.Blue -> Color(0xFF0969DA)
    AccentColor.Indigo -> Color(0xFF4F46C7)
    AccentColor.Purple -> Color(0xFF8250DF)
    AccentColor.Pink -> Color(0xFFC72C7A)
}

fun parseAccentHex(value: String): Color? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    if (!clean.all { it in "0123456789abcdefABCDEF" }) return null
    val number = clean.toLongOrNull(16) ?: return null
    return if (clean.length == 8) {
        Color(
            red = ((number shr 16) and 0xFF) / 255f,
            green = ((number shr 8) and 0xFF) / 255f,
            blue = (number and 0xFF) / 255f,
            alpha = ((number shr 24) and 0xFF) / 255f
        )
    } else {
        Color(
            red = ((number shr 16) and 0xFF) / 255f,
            green = ((number shr 8) and 0xFF) / 255f,
            blue = (number and 0xFF) / 255f
        )
    }
}

fun normalizeAccentHex(value: String): String? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    if (!clean.all { it in "0123456789abcdefABCDEF" }) return null
    return "#${clean.uppercase()}"
}

fun accentPalette(seed: Color): AccentTonalPalette {
    val hsl = rgbToHsl(seed)
    val lightPrimary = hslColor(hsl.h, max(hsl.s, 0.58f), 0.42f)
    val lightContainer = hslColor(hsl.h, max(hsl.s * 0.72f, 0.32f), 0.92f)
    val darkPrimary = hslColor(hsl.h, max(hsl.s * 0.82f, 0.52f), 0.78f)
    val darkContainer = hslColor(hsl.h, max(hsl.s * 0.78f, 0.38f), 0.24f)

    return AccentTonalPalette(
        lightPrimary = lightPrimary,
        lightContainer = lightContainer,
        lightOnPrimary = readableOn(lightPrimary),
        lightOnContainer = readableOn(lightContainer),
        darkPrimary = darkPrimary,
        darkContainer = darkContainer,
        darkOnPrimary = readableOn(darkPrimary),
        darkOnContainer = readableOn(darkContainer)
    )
}

fun contrastRatio(foreground: Color, background: Color): Float {
    val a = foreground.luminance() + 0.05f
    val b = background.luminance() + 0.05f
    return max(a, b) / min(a, b)
}

fun readableOn(background: Color): Color =
    if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) Color.White else Color.Black

private fun rgbToHsl(color: Color): Hsl {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxValue = max(r, max(g, b))
    val minValue = min(r, min(g, b))
    val delta = maxValue - minValue
    val lightness = (maxValue + minValue) / 2f
    if (delta == 0f) return Hsl(0f, 0f, lightness)

    val saturation = delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    val hue = when (maxValue) {
        r -> ((g - b) / delta).let { if (it < 0f) it + 6f else it }
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } / 6f
    return Hsl(hue, saturation, lightness)
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = (hue % 1f + 1f) % 1f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val chroma = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = chroma * (1f - kotlin.math.abs((h * 6f) % 2f - 1f))
    val m = l - chroma / 2f
    val (r, g, b) = when ((h * 6f).toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
