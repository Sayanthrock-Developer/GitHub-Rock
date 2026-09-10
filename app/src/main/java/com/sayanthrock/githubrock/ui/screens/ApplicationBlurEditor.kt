package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurBorder
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurComponent
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurShadow
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurTint
import com.sayanthrock.githubrock.ui.components.GlassCard

@Composable
fun ApplicationBlurEditor(
    settings: ApplicationBlurSettings,
    onMode: (ApplicationBlurMode) -> Unit,
    onPreset: (ApplicationBlurPreset) -> Unit,
    onProfile: (ApplicationBlurProfile) -> Unit,
    onCustomTint: (String) -> Unit = {},
    onComponentProfile: (ApplicationBlurComponent, ApplicationBlurProfile?) -> Unit = { _, _ -> },
    onReset: () -> Unit = {},
) {
    var selectedComponent by remember { mutableStateOf<ApplicationBlurComponent?>(null) }
    val global = settings.profile.sanitized()
    val selectedProfile = selectedComponent?.let(settings::profileFor) ?: global

    GlassCard {
        Column(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Application Blur")
            Text(
                "Real app surfaces can use these settings while repository content, text, icons, Markdown, and code remain sharp.",
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (global.enabled) "Enabled" else "Off")
                Switch(checked = global.enabled, onCheckedChange = { onProfile(global.copy(enabled = it)) })
            }

            Text("Mode")
            ChipRow(ApplicationBlurMode.entries, settings.mode, onMode)
            Text("Preset")
            ChipRow(ApplicationBlurPreset.entries, settings.preset, onPreset)

            Text("Global profile")
            ProfileControls(selectedProfile, onProfile = { updated ->
                if (selectedComponent == null) onProfile(updated) else onComponentProfile(selectedComponent!!, updated)
            })

            Text("Tint color")
            TextField(
                value = settings.customTintHex,
                onValueChange = onCustomTint,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Custom tint HEX") },
                placeholder = { Text("#RRGGBB") },
            )
            Text("Tint supports System, Theme, or Custom. Invalid HEX is safely ignored by the renderer.")

            Text("Per-component overrides")
            Text("Choose a surface to customize it independently, or return to Global.")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedComponent == null,
                    onClick = { selectedComponent = null },
                    label = { Text("Global") },
                )
                ApplicationBlurComponent.entries.forEach { component ->
                    FilterChip(
                        selected = selectedComponent == component,
                        onClick = { selectedComponent = component },
                        label = { Text(component.displayName()) },
                    )
                }
            }

            selectedComponent?.let { component ->
                val hasOverride = settings.components.containsKey(component)
                Text("${component.displayName()} profile")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !hasOverride,
                        onClick = { onComponentProfile(component, null) },
                        label = { Text("Use global") },
                    )
                    FilterChip(
                        selected = hasOverride,
                        onClick = {
                            if (!hasOverride) onComponentProfile(component, settings.profileFor(component))
                        },
                        label = { Text("Custom") },
                    )
                }
                ProfileControls(selectedProfile, onProfile = { onComponentProfile(component, it) })
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset Application Blur")
            }
            Text("Settings are persisted through the existing ApplicationBlurPreferences store.")
        }
    }
}

@Composable
private fun ProfileControls(
    profile: ApplicationBlurProfile,
    onProfile: (ApplicationBlurProfile) -> Unit,
) {
    BlurSlider("Intensity", profile.intensity, 0..100) { onProfile(profile.copy(intensity = it)) }
    BlurSlider("Radius", profile.radius, 0..64) { onProfile(profile.copy(radius = it)) }
    BlurSlider("Background dim", profile.backgroundDim, 0..100) { onProfile(profile.copy(backgroundDim = it)) }
    BlurSlider("Glass opacity", profile.glassOpacity, 0..100) { onProfile(profile.copy(glassOpacity = it)) }
    BlurSlider("Saturation", profile.saturation, 0..200) { onProfile(profile.copy(saturation = it)) }
    BlurSlider("Brightness", profile.brightness, 0..200) { onProfile(profile.copy(brightness = it)) }
    BlurSlider("Corner radius", profile.cornerRadius, 0..64) { onProfile(profile.copy(cornerRadius = it)) }
    Text("Border")
    ChipRow(ApplicationBlurBorder.entries, profile.border) { onProfile(profile.copy(border = it)) }
    BlurSlider("Border opacity", profile.borderOpacity, 0..100) { onProfile(profile.copy(borderOpacity = it)) }
    Text("Shadow")
    ChipRow(ApplicationBlurShadow.entries, profile.shadow) { onProfile(profile.copy(shadow = it)) }
    Text("Tint")
    ChipRow(ApplicationBlurTint.entries, profile.tint) { onProfile(profile.copy(tint = it)) }
    BlurSlider("Tint opacity", profile.tintOpacity, 0..100) { onProfile(profile.copy(tintOpacity = it)) }
}

@Composable
private fun <T> ChipRow(
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(value.displayName()) },
            )
        }
    }
}

private fun Any.displayName(): String = when (this) {
    ApplicationBlurMode.Automatic -> "Automatic"
    ApplicationBlurMode.Subtle -> "Subtle"
    ApplicationBlurMode.Medium -> "Medium"
    ApplicationBlurMode.Strong -> "Strong"
    ApplicationBlurMode.Off -> "Off"
    ApplicationBlurPreset.LiquidGlass -> "Liquid Glass"
    ApplicationBlurPreset.DeepGlass -> "Deep Glass"
    ApplicationBlurPreset.None -> "None"
    ApplicationBlurPreset.Clean -> "Clean"
    ApplicationBlurPreset.Frosted -> "Frosted"
    ApplicationBlurPreset.Custom -> "Custom"
    ApplicationBlurBorder.Off -> "Off"
    ApplicationBlurBorder.Subtle -> "Subtle"
    ApplicationBlurBorder.Strong -> "Strong"
    ApplicationBlurShadow.Off -> "Off"
    ApplicationBlurShadow.Soft -> "Soft"
    ApplicationBlurShadow.Strong -> "Strong"
    ApplicationBlurTint.System -> "System"
    ApplicationBlurTint.Theme -> "Theme"
    ApplicationBlurTint.Custom -> "Custom"
    else -> toString().replace('_', ' ')
}

@Composable
private fun BlurSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $value")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}
