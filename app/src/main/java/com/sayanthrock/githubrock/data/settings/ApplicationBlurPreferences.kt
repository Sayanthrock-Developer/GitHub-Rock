package com.sayanthrock.githubrock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurBorder
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurComponent
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurMode
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurPreset
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurProfile
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurSettings
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurShadow
import com.sayanthrock.githubrock.ui.blur.ApplicationBlurTint
import com.sayanthrock.githubrock.ui.blur.toSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.applicationBlurDataStore by preferencesDataStore(name = "github_rock_blur_preferences")

@Singleton
class ApplicationBlurPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<ApplicationBlurSettings> = context.applicationBlurDataStore.data.map { preferences ->
        val mode = ApplicationBlurMode.fromStored(preferences[MODE])
        val preset = ApplicationBlurPreset.fromStored(preferences[PRESET])
        ApplicationBlurSettings(
            mode = mode,
            preset = preset,
            profile = decode(preferences[GLOBAL_PROFILE]) ?: preset.toSettings().profile,
            customTintHex = preferences[CUSTOM_TINT].orEmpty(),
            components = ApplicationBlurComponent.entries.mapNotNull { component ->
                decode(preferences[componentKey(component)])?.let { component to it }
            }.toMap()
        )
    }

    suspend fun setSettings(settings: ApplicationBlurSettings) = context.applicationBlurDataStore.edit { preferences ->
        val clean = settings.copy(profile = settings.profile.sanitized())
        preferences[MODE] = clean.mode.name
        preferences[PRESET] = clean.preset.name
        preferences[GLOBAL_PROFILE] = encode(clean.profile)
        preferences[CUSTOM_TINT] = clean.customTintHex.take(32)
        ApplicationBlurComponent.entries.forEach { component ->
            val key = componentKey(component)
            clean.components[component]?.let { preferences[key] = encode(it) } ?: preferences.remove(key)
        }
    }

    suspend fun setProfile(profile: ApplicationBlurProfile) = update { it.copy(profile = profile.sanitized(), preset = ApplicationBlurPreset.Custom) }
    suspend fun setMode(mode: ApplicationBlurMode) = update { it.copy(mode = mode, preset = if (mode == ApplicationBlurMode.Off) ApplicationBlurPreset.None else it.preset) }
    suspend fun setPreset(preset: ApplicationBlurPreset) = setSettings(preset.toSettings())
    suspend fun setCustomTint(hex: String) = update { it.copy(customTintHex = hex.take(32)) }
    suspend fun setComponentProfile(component: ApplicationBlurComponent, profile: ApplicationBlurProfile?) = update {
        it.copy(components = it.components.toMutableMap().apply {
            if (profile == null) remove(component) else put(component, profile.sanitized())
        })
    }
    suspend fun reset() = setSettings(ApplicationBlurPreset.Clean.toSettings())

    private suspend fun update(transform: (ApplicationBlurSettings) -> ApplicationBlurSettings) {
        setSettings(transform(settings.first()))
    }

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val PRESET = stringPreferencesKey("preset")
        val GLOBAL_PROFILE = stringPreferencesKey("global_profile")
        val CUSTOM_TINT = stringPreferencesKey("custom_tint")
        const val SEP = "|"
        fun componentKey(component: ApplicationBlurComponent) = stringPreferencesKey("component_${component.name}")

        fun encode(profile: ApplicationBlurProfile): String = listOf(
            profile.enabled, profile.intensity, profile.radius, profile.backgroundDim, profile.glassOpacity,
            profile.saturation, profile.brightness, profile.cornerRadius, profile.border.name,
            profile.borderOpacity, profile.shadow.name, profile.tint.name, profile.tintOpacity
        ).joinToString(SEP)

        fun decode(value: String?): ApplicationBlurProfile? = runCatching {
            val p = value?.split(SEP) ?: return null
            if (p.size != 13) return null
            ApplicationBlurProfile(
                enabled = p[0].toBooleanStrict(), intensity = p[1].toInt(), radius = p[2].toInt(),
                backgroundDim = p[3].toInt(), glassOpacity = p[4].toInt(), saturation = p[5].toInt(),
                brightness = p[6].toInt(), cornerRadius = p[7].toInt(),
                border = ApplicationBlurBorder.fromStored(p[8]), borderOpacity = p[9].toInt(),
                shadow = ApplicationBlurShadow.fromStored(p[10]), tint = ApplicationBlurTint.fromStored(p[11]),
                tintOpacity = p[12].toInt()
            ).sanitized()
        }.getOrNull()
    }
}
