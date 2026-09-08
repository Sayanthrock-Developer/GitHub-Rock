package com.sayanthrock.githubrock

import com.sayanthrock.githubrock.data.settings.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test fun storedThemeValuesFallBackSafely(){assertEquals(ThemeMode.Dark,ThemeMode.fromStored("Dark"));assertEquals(ThemeMode.System,ThemeMode.fromStored("unknown"));assertEquals(ThemeMode.System,ThemeMode.fromStored(null))}
    @Test fun storedStyleValuesFallBackToClean(){assertEquals(ThemeStyle.LiquidGlass,ThemeStyle.fromStored("LiquidGlass"));assertEquals(ThemeStyle.Midnight,ThemeStyle.fromStored("Midnight"));assertEquals(ThemeStyle.Obsidian,ThemeStyle.fromStored("Obsidian"));assertEquals(ThemeStyle.Clean,ThemeStyle.fromStored("unknown"));assertEquals(ThemeStyle.Clean,ThemeStyle.fromStored(null))}
    @Test fun storedAccentValuesMigrateAndFallbackSafely(){assertEquals(AccentColor.Purple,AccentColor.fromStored("Violet"));assertEquals(AccentColor.Green,AccentColor.fromStored("Emerald"));assertEquals(AccentColor.Pink,AccentColor.fromStored("Rose"));assertEquals(AccentColor.Red,AccentColor.fromStored("Coral"));assertEquals(AccentColor.Yellow,AccentColor.fromStored("Amber"));assertEquals(AccentColor.DefaultGitHubRock,AccentColor.fromStored("unknown"));assertEquals(AccentColor.DefaultGitHubRock,AccentColor.fromStored(null))}
    @Test fun allAccentOptionsAreAvailable(){assertEquals(listOf("DefaultGitHubRock","Red","Orange","Yellow","Green","Teal","Cyan","Blue","Indigo","Purple","Pink"),AccentColor.entries.map{it.name})}
    @Test fun displayAndTypographyValuesUseStandardFallbacks(){assertEquals(DisplaySize.Standard,DisplaySize.fromStored("unknown"));assertEquals(FontSize.Default,FontSize.fromStored(null));assertEquals(FontWeightStyle.Default,FontWeightStyle.fromStored("unknown"));assertEquals(AppFontFamily.SystemSans,AppFontFamily.fromStored(null));assertEquals(LoadingStyle.Spinner,LoadingStyle.fromStored("unknown"));assertEquals(CodeColorStyle.Classic,CodeColorStyle.fromStored(null));assertEquals(LogDisplayStyle.Terminal,LogDisplayStyle.fromStored(null));assertEquals(LogDisplayStyle.Dialog,LogDisplayStyle.fromStored("Dialog"))}
    @Test fun nativeDefaultsRemainAvailable(){val p=AppearancePreferences();assertEquals(ThemeStyle.Clean,p.themeStyle);assertEquals(DisplaySize.Standard,p.displaySize);assertEquals(FontSize.Default,p.fontSize);assertEquals(FontWeightStyle.Default,p.fontWeight);assertEquals(AppFontFamily.SystemSans,p.fontFamily);assertEquals(LoadingStyle.Spinner,p.loadingStyle);assertEquals(CodeColorStyle.Classic,p.codeColorStyle);assertEquals(LogDisplayStyle.Terminal,p.logDisplayStyle);assertTrue(p.dynamicColor);assertTrue(p.showImages);assertTrue(p.workflowPreview);assertTrue(p.workflowStepDetails);assertTrue(p.statusColors);assertTrue(p.actionsControls);assertTrue(p.repositoryManager);assertTrue(p.fileTools);assertFalse(p.compactCards);assertFalse(p.reduceMotion)}
}
