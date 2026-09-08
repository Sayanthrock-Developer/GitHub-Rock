package com.sayanthrock.githubrock.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import com.sayanthrock.githubrock.data.settings.*

val LocalRemoteImagesEnabled=staticCompositionLocalOf{true}
val LocalLoadingStyle=staticCompositionLocalOf{LoadingStyle.Spinner}
val LocalReduceMotion=staticCompositionLocalOf{false}
val LocalCodeColorStyle=staticCompositionLocalOf{CodeColorStyle.Classic}
val LocalLogDisplayStyle=staticCompositionLocalOf{LogDisplayStyle.Terminal}
data class CodeColors(val keyword:Color,val string:Color,val comment:Color,val number:Color,val type:Color,val property:Color)
val LocalCodeColors=staticCompositionLocalOf{CodeColors(Color(0xFF79B8FF),Color(0xFF85E89D),Color(0xFF8B949E),Color(0xFFFFAB70),Color(0xFFBC8CFF),Color(0xFFFF7B72))}
private fun DisplaySize.scale()=when(this){DisplaySize.Small->.90f;DisplaySize.Standard->1f;DisplaySize.Large->1.12f}
private fun FontSize.scale()=when(this){FontSize.Small->.90f;FontSize.Default->1f;FontSize.Large->1.16f}

@Composable
fun GitHubRockTheme(darkTheme:Boolean=true,dynamicColor:Boolean=true,trueBlack:Boolean=true,accentColor:AccentColor=AccentColor.DefaultGitHubRock,customAccentHex:String="",themeStyle:ThemeStyle=ThemeStyle.Clean,displaySize:DisplaySize=DisplaySize.Standard,fontSize:FontSize=FontSize.Default,fontWeight:FontWeightStyle=FontWeightStyle.Default,fontFamily:AppFontFamily=AppFontFamily.SystemSans,loadingStyle:LoadingStyle=LoadingStyle.Spinner,codeColorStyle:CodeColorStyle=CodeColorStyle.Classic,logDisplayStyle:LogDisplayStyle=LogDisplayStyle.Terminal,reduceMotion:Boolean=false,showImages:Boolean=true,content:@Composable ()->Unit){
 val c=LocalContext.current;val d=LocalDensity.current;val sys=dynamicColor&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S;val a=accentPalette(parseAccentHex(customAccentHex)?:accentColor.seedColor());val scheme=when{sys&&darkTheme->dynamicDarkColorScheme(c);sys->dynamicLightColorScheme(c);darkTheme->rockDarkColors(a);else->rockLightColors(a)}.applyRockStyle(themeStyle,darkTheme).applyTrueBlack(darkTheme,trueBlack);val density=Density(d.density*displaySize.scale(),d.fontScale*fontSize.scale());CompositionLocalProvider(LocalRemoteImagesEnabled provides showImages,LocalLoadingStyle provides loadingStyle,LocalReduceMotion provides reduceMotion,LocalCodeColorStyle provides codeColorStyle,LocalLogDisplayStyle provides logDisplayStyle,LocalCodeColors provides rockCodeColors(codeColorStyle,darkTheme),LocalDensity provides density){MaterialTheme(colorScheme=scheme,typography=rockTypography(fontFamily,fontWeight),shapes=rockShapes(themeStyle),content=content)}
}
