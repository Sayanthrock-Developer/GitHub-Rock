package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurBorder
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurShadow
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurTint
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun ApplicationBlurEditor(
    settings: ApplicationBlurSettings,
    onMode: (ApplicationBlurMode) -> Unit,
    onPreset: (ApplicationBlurPreset) -> Unit,
    onProfile: (ApplicationBlurProfile) -> Unit,
) {
    val p = settings.profile
    GlassCard {
        Column(Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Application Blur")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (p.enabled) "Enabled" else "Off")
                Switch(checked = p.enabled, onCheckedChange = { onProfile(p.copy(enabled = it)) })
            }
            Text("Mode")
            ChipRow(ApplicationBlurMode.entries, settings.mode, onMode)
            Text("Preset")
            ChipRow(ApplicationBlurPreset.entries, settings.preset, onPreset)
            BlurSlider("Intensity", p.intensity, 0..100) { onProfile(p.copy(intensity = it)) }
            BlurSlider("Radius", p.radius, 0..64) { onProfile(p.copy(radius = it)) }
            BlurSlider("Background dim", p.backgroundDim, 0..100) { onProfile(p.copy(backgroundDim = it)) }
            BlurSlider("Glass opacity", p.glassOpacity, 0..100) { onProfile(p.copy(glassOpacity = it)) }
            BlurSlider("Saturation", p.saturation, 0..200) { onProfile(p.copy(saturation = it)) }
            BlurSlider("Brightness", p.brightness, 0..200) { onProfile(p.copy(brightness = it)) }
            BlurSlider("Corner radius", p.cornerRadius, 0..64) { onProfile(p.copy(cornerRadius = it)) }
            Text("Border")
            ChipRow(ApplicationBlurBorder.entries, p.border, { onProfile(p.copy(border = it)) })
            BlurSlider("Border opacity", p.borderOpacity, 0..100) { onProfile(p.copy(borderOpacity = it)) }
            Text("Shadow")
            ChipRow(ApplicationBlurShadow.entries, p.shadow, { onProfile(p.copy(shadow = it)) })
            Text("Tint")
            ChipRow(ApplicationBlurTint.entries, p.tint, { onProfile(p.copy(tint = it)) })
            BlurSlider("Tint opacity", p.tintOpacity, 0..100) { onProfile(p.copy(tintOpacity = it)) }
            Spacer(Modifier.height(4.dp))
            Text("Changes are persisted through the existing ApplicationBlurPreferences store.")
        }
    }
}

@Composable
private fun <T> ChipRow(values: List<T>, selected: T, onSelected: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(value.toString().replace('_', ' ')) }) }
    }
}

@Composable
private fun BlurSlider(label: String, value: Int, range: IntRange, onValue: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $value")
        Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat())
    }
}
