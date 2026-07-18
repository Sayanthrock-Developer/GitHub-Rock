package com.sayanthrock.githubrock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sayanthrock.githubrock.data.settings.AppFontFamily
import com.sayanthrock.githubrock.data.settings.FontWeightStyle

private fun AppFontFamily.composeFamily(): FontFamily = when (this) {
    AppFontFamily.SystemSans -> FontFamily.SansSerif
    AppFontFamily.Serif -> FontFamily.Serif
    AppFontFamily.Monospace -> FontFamily.Monospace
}

private data class WeightSet(
    val display: FontWeight,
    val title: FontWeight,
    val body: FontWeight,
    val label: FontWeight
)

private fun FontWeightStyle.weights(): WeightSet = when (this) {
    FontWeightStyle.Light -> WeightSet(
        display = FontWeight.SemiBold,
        title = FontWeight.Medium,
        body = FontWeight.Light,
        label = FontWeight.Normal
    )
    FontWeightStyle.Default -> WeightSet(
        display = FontWeight.Bold,
        title = FontWeight.SemiBold,
        body = FontWeight.Normal,
        label = FontWeight.Medium
    )
    FontWeightStyle.Bold -> WeightSet(
        display = FontWeight.ExtraBold,
        title = FontWeight.Bold,
        body = FontWeight.Medium,
        label = FontWeight.SemiBold
    )
}

fun rockTypography(
    familyStyle: AppFontFamily = AppFontFamily.SystemSans,
    weightStyle: FontWeightStyle = FontWeightStyle.Default
): Typography {
    val family = familyStyle.composeFamily()
    val weights = weightStyle.weights()
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontWeight = weights.display, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = weights.display, fontSize = 30.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = weights.display, fontSize = 26.sp, lineHeight = 34.sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = weights.title, fontSize = 22.sp, lineHeight = 29.sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = weights.title, fontSize = 20.sp, lineHeight = 27.sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = weights.title, fontSize = 16.sp, lineHeight = 23.sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = weights.title, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = weights.body, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = weights.body, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = weights.body, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = weights.label, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = weights.label, fontSize = 12.sp, lineHeight = 17.sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = weights.label, fontSize = 11.sp, lineHeight = 16.sp)
    )
}

val RockTypography = rockTypography()
