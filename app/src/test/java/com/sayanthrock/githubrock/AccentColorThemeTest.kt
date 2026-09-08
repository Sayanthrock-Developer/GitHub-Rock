package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class AccentColorThemeTest {
    @Test fun parsesRgbAndArgbHex(){
        assertEquals(1f,parseAccentHex("#FF0000")!!.red,0.001f)
        assertEquals(1f,parseAccentHex("#80FF0000")!!.alpha,0.001f)
        assertEquals(.501f,parseAccentHex("#80FF0000")!!.alpha,0.002f)
    }
    @Test fun rejectsInvalidHex(){assertNull(parseAccentHex("#12345"));assertNull(parseAccentHex("#GGGGGG"));assertNull(parseAccentHex("red"))}
    @Test fun normalizesHex(){assertEquals("#ABCDEF",normalizeAccentHex(" abcdef "));assertEquals("#80ABCDEF",normalizeAccentHex("#80abcdef"));assertNull(normalizeAccentHex("#1234"))}
    @Test fun everyPresetProducesAReadablePalette(){AccentColor.values().forEach{val p=accentPalette(it.seedColor());assertTrue(contrastRatio(p.lightOnPrimary,p.lightPrimary)>=4.5f);assertTrue(contrastRatio(p.darkOnPrimary,p.darkPrimary)>=4.5f)}}
    @Test fun tonalPaletteContainsDistinctLightAndDarkTones(){val p=accentPalette(AccentColor.Blue.seedColor());assertNotEquals(p.lightPrimary,p.darkPrimary);assertNotEquals(p.lightContainer,p.darkContainer)}
}
