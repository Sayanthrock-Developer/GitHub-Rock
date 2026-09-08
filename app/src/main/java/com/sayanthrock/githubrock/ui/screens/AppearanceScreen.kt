package com.sayanthrock.githubrock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayanthrock.githubrock.data.settings.*
import com.sayanthrock.githubrock.ui.components.*
import com.sayanthrock.githubrock.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(onBack:()->Unit,viewModel:AppearanceViewModel=hiltViewModel()){
 val state by viewModel.state.collectAsStateWithLifecycle()
 AppearanceContent(state,onBack,viewModel::setThemeMode,viewModel::setAccentColor,viewModel::setDynamicColor,viewModel::setTrueBlack,viewModel::setThemeStyle,viewModel::setDisplaySize,viewModel::setFontSize,viewModel::setFontWeight,viewModel::setFontFamily,viewModel::setLoadingStyle,viewModel::setAnimationStyle,viewModel::setCodeColorStyle,viewModel::setLogDisplayStyle,viewModel::setShowImages,viewModel::setNavigationBarStyle,viewModel::setCustomAccentHex,viewModel::resetAppearance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceContent(state:AppearancePreferences,onBack:()->Unit,onThemeMode:(ThemeMode)->Unit,onAccentColor:(AccentColor)->Unit,onDynamicColor:(Boolean)->Unit,onTrueBlack:(Boolean)->Unit,onThemeStyle:(ThemeStyle)->Unit={},onDisplaySize:(DisplaySize)->Unit={},onFontSize:(FontSize)->Unit={},onFontWeight:(FontWeightStyle)->Unit={},onFontFamily:(AppFontFamily)->Unit={},onLoadingStyle:(LoadingStyle)->Unit={},onAnimationStyle:(AnimationStyle)->Unit={},onCodeColorStyle:(CodeColorStyle)->Unit={},onLogDisplayStyle:(LogDisplayStyle)->Unit={},onShowImages:(Boolean)->Unit={},onNavigationBarStyle:(NavigationBarStyle)->Unit={},onCustomAccentHex:(String)->Unit={},onReset:()->Unit={}){
 var confirmReset by remember{mutableStateOf(false)}
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("Settings")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))}){padding->
  androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp,12.dp,16.dp,48.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   item{StandardScreenHeader("Customize your experience","Choose the visual system, scale, typography, loading style, animation, and code presentation.")}
   item{StandardSectionHeader("Theme")};item{ThemePreview(state)}
   item{ChoiceCard("Design style","Seven complete surface and shape systems",Icons.Default.Palette,ThemeStyle.entries.map{it to it.displayName},state.themeStyle,onThemeStyle)}
   item{AccentPicker(state,onAccentColor,onDynamicColor,onCustomAccentHex)}
   item{ThemeControls(state,onThemeMode,onDynamicColor,onTrueBlack,onShowImages)}
   item{StandardSectionHeader("Navigation")};item{NavigationBarStyleControl(state.navigationBarStyle,onNavigationBarStyle)}
   item{StandardSectionHeader("Display size")};item{ChoiceCard("Interface scale","Changes controls, cards, spacing, and navigation app-wide",Icons.Default.ViewCompact,listOf(DisplaySize.Large to "Large",DisplaySize.Standard to "Standard",DisplaySize.Small to "Small"),state.displaySize,onDisplaySize)}
   item{StandardSectionHeader("Fonts")};item{ChoiceCard("Font family","System sans, serif, or developer monospace",Icons.Default.TextFields,AppFontFamily.entries.map{it to it.displayName},state.fontFamily,onFontFamily)}
   item{ChoiceCard("Font size","Small, default, or large readable text",Icons.Default.FormatSize,listOf(FontSize.Small to "Small",FontSize.Default to "Default",FontSize.Large to "Large"),state.fontSize,onFontSize)}
   item{ChoiceCard("Font weight","Light, default, or stronger text",Icons.Default.FormatSize,listOf(FontWeightStyle.Light to "Light",FontWeightStyle.Default to "Default",FontWeightStyle.Bold to "Bold"),state.fontWeight,onFontWeight)};item{TypographyPreview()}
   item{StandardSectionHeader("Animation")};item{AnimationStyleControl(state.animationStyle,state.reduceMotion,onAnimationStyle)}
   item{StandardSectionHeader("Loading and code")};item{ChoiceCard("Loading animation","Applied to repository loading and repository operations",Icons.Default.PlayArrow,LoadingStyle.entries.map{it to it.name},state.loadingStyle,onLoadingStyle)}
   item{GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){AppLoadingIndicator(state.loadingStyle,state.reduceMotion);Text("Live loading preview",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}}}
   item{ChoiceCard("Code colors","Syntax colors stay isolated from the rest of the interface",Icons.Default.Code,CodeColorStyle.entries.map{it to it.displayName},state.codeColorStyle,onCodeColorStyle)}
   item{ChoiceCard("Log display style","Scrollable popup or a full-screen highlighted terminal",Icons.Default.Code,listOf(LogDisplayStyle.Dialog to "Popup dialog",LogDisplayStyle.Terminal to "On-screen terminal"),state.logDisplayStyle,onLogDisplayStyle)};item{CodeColorPreview()}
   item{OutlinedButton(onClick={confirmReset=true},Modifier.fillMaxWidth().height(52.dp)){Icon(Icons.Default.RestartAlt,null);Spacer(Modifier.width(8.dp));Text("Reset settings")}}
   item{Text("Reset restores visual defaults. Your GitHub connection, downloads, and saved repositories stay unchanged.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}
  }
 }
 if(confirmReset)AlertDialog(onDismissRequest={confirmReset=false},title={Text("Reset settings?")},text={Text("Theme, accent and dynamic colors, true black, remote images, navigation bar style, display size, fonts, loading, animation, code colors, and log presentation will return to defaults.")},confirmButton={Button(onClick={confirmReset=false;onReset()}){Text("Reset")}},dismissButton={TextButton(onClick={confirmReset=false}){Text("Cancel")}})
}

@Composable private fun AccentPicker(state:AppearancePreferences,onSelected:(AccentColor)->Unit,onDynamic:(Boolean)->Unit,onCustom:(String)->Unit){
 var dialog by remember{mutableStateOf(false)}
 GlassCard{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.ColorLens,null,tint=MaterialTheme.colorScheme.primary);Column{Text("Accent color",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(if(state.dynamicColor)"System Dynamic is active" else "Customize the app highlight",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}}
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
   FilterChip(selected=state.dynamicColor,onClick={onDynamic(true)},label={Text("System Dynamic")},leadingIcon={Icon(Icons.Default.AutoAwesome,null,Modifier.size(18.dp))})
   AccentColor.entries.forEach{accent->val selected=!state.dynamicColor&&state.accentColor==accent&&state.customAccentHex.isBlank();Surface(onClick={onSelected(accent)},selected=selected,enabled=!state.dynamicColor,modifier=Modifier.size(44.dp).semantics{contentDescription="Use ${accentLabel(accent)} accent"},shape=CircleShape,color=accent.seedColor(),border=BorderStroke(if(selected)3.dp else 1.dp,if(selected)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)){if(selected)Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Check,null,tint=readableOn(accent.seedColor()))}}}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={dialog=true}){Icon(Icons.Default.ColorLens,null);Spacer(Modifier.width(6.dp));Text("Custom Color")};if(state.customAccentHex.isNotBlank())Text(state.customAccentHex,Modifier.align(Alignment.CenterVertically),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
  if(state.recentAccentColors.isNotEmpty()){Text("Recent Colors",style=MaterialTheme.typography.labelLarge);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){state.recentAccentColors.forEach{hex->val color=parseAccentHex(hex)?:return@forEach;Surface(onClick={onCustom(hex)},modifier=Modifier.size(38.dp).semantics{contentDescription="Use recent color $hex"},shape=CircleShape,color=color,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){}}}}
  Text("Static accents override Android wallpaper colors. System Dynamic is used only when Color mode is System.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
 }}
 if(dialog)CustomAccentDialog(state.customAccentHex,onCustom){dialog=false}
}

@Composable private fun CustomAccentDialog(initial:String,onApply:(String)->Unit,onDismiss:()->Unit){
 var hex by remember(initial){mutableStateOf(initial.ifBlank{"#E60023"})};var color by remember(initial){mutableStateOf(parseAccentHex(initial)?:Color(0xFFE60023))};var hue by remember(color){mutableFloatStateOf(rgbHue(color))};var sat by remember(color){mutableFloatStateOf(rgbSat(color))};var light by remember(color){mutableFloatStateOf(rgbLight(color))}
 fun update(){color=hslColor(hue,sat,light);hex=color.toHex()}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Custom Accent Color")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Surface(Modifier.fillMaxWidth().height(72.dp),shape=MaterialTheme.shapes.large,color=color){Box(contentAlignment=Alignment.Center){Text(hex,color=readableOn(color),fontWeight=FontWeight.Bold)}}
  OutlinedTextField(value=hex,onValueChange={candidate->hex=candidate;parseAccentHex(candidate)?.let{color=it;hue=rgbHue(it);sat=rgbSat(it);light=rgbLight(it)}},singleLine=true,label={Text("HEX")},placeholder={Text("#RRGGBB or #AARRGGBB")},isError=hex.isNotBlank()&&parseAccentHex(hex)==null,modifier=Modifier.fillMaxWidth())
  Text("Hue",style=MaterialTheme.typography.labelMedium);Slider(value=hue,onValueChange={hue=it;update()},valueRange=0f..1f);Text("Saturation",style=MaterialTheme.typography.labelMedium);Slider(value=sat,onValueChange={sat=it;update()},valueRange=0f..1f);Text("Lightness",style=MaterialTheme.typography.labelMedium);Slider(value=light,onValueChange={light=it;update()},valueRange=0f..1f)
  Text("Contrast is checked automatically for text placed on the accent.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
 }},confirmButton={Button(enabled=parseAccentHex(hex)!=null,onClick={onApply(normalizeAccentHex(hex)!!);onDismiss()}){Text("Apply")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable private fun ThemePreview(state:AppearancePreferences){GlassCard{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Surface(Modifier.size(52.dp),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Palette,null,tint=MaterialTheme.colorScheme.primary)}};Column(Modifier.weight(1f)){Text(state.themeStyle.displayName,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${state.themeMode.name} mode · ${state.displaySize.name} display · ${state.fontSize.name} text",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.Check,"Selected theme",tint=MaterialTheme.colorScheme.primary)}}}
@Composable private fun <T> ChoiceCard(title:String,subtitle:String,icon:ImageVector,choices:List<Pair<T,String>>,selected:T,onSelected:(T)->Unit){GlassCard{Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}};Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){choices.forEach{(v,l)->FilterChip(selected==v,{onSelected(v)},label={Text(l)},leadingIcon=if(selected==v)({Icon(Icons.Default.Check,null,Modifier.size(18.dp))})else null)}}}}}
@Composable private fun ThemeControls(state:AppearancePreferences,onThemeMode:(ThemeMode)->Unit,onDynamic:(Boolean)->Unit,onTrueBlack:(Boolean)->Unit,onImages:(Boolean)->Unit){StandardSettingsGroup{Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.DarkMode,null,tint=MaterialTheme.colorScheme.primary);Column{Text("Color mode",style=MaterialTheme.typography.titleSmall);Text("Follow the system, stay light, or stay dark",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){ThemeMode.entries.forEach{m->FilterChip(state.themeMode==m,{onThemeMode(m)},label={Text(m.name)},modifier=Modifier.weight(1f))}}};StandardSettingsDivider();ToggleRow(Icons.Default.Image,"Show remote images","Avatars and repository artwork",state.showImages,onImages);StandardSettingsDivider();ToggleRow(Icons.Default.ColorLens,"System dynamic color","Use the Android wallpaper palette in System mode",state.dynamicColor,onDynamic);StandardSettingsDivider();ToggleRow(Icons.Default.DarkMode,"True black","Pure black in dark mode",state.trueBlack,onTrueBlack)}}
@Composable private fun NavigationBarStyleControl(selected:NavigationBarStyle,onSelected:(NavigationBarStyle)->Unit){GlassCard{Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.ViewCompact,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f)){Text("Navigation Bar Style",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("Choose how Home, Repositories, Builds, Downloads, and Profile are presented.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}};Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){NavigationBarStyle.entries.forEach{style->FilterChip(selected==style,{onSelected(style)},label={Text(style.displayName)},leadingIcon=if(selected==style)({Icon(Icons.Default.Check,null,Modifier.size(18.dp))})else null)}}}}}
@Composable private fun AnimationStyleControl(selected:AnimationStyle,reduceMotion:Boolean,onSelected:(AnimationStyle)->Unit){val styles=AnimationStyle.entries;var value by remember(selected){mutableFloatStateOf(styles.indexOf(selected).coerceAtLeast(0).toFloat())};val current=styles[value.toInt().coerceIn(0,styles.lastIndex)];GlassCard{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.PlayArrow,null,tint=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f)){Text("Animation style",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("Swipe the pill to change the motion system",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}};Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.7f),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(horizontal=14.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(current.displayName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Slider(value=value,onValueChange={value=it},onValueChangeFinished={onSelected(styles[value.toInt().coerceIn(0,styles.lastIndex)])},valueRange=0f..styles.lastIndex.toFloat(),steps=styles.size-2,enabled=!reduceMotion,modifier=Modifier.semantics{contentDescription="Animation style slider"});Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(styles.first().displayName,style=MaterialTheme.typography.labelSmall);Text(styles.last().displayName,style=MaterialTheme.typography.labelSmall)}}};Text(if(reduceMotion)"Reduced motion is enabled; animation effects are minimized." else "Liquid · Spring · Cinematic · Magnetic · Dynamic",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)}}}
@Composable private fun TypographyPreview(){GlassCard{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Interface preview",style=MaterialTheme.typography.headlineSmall);Text("Clean typography preview",style=MaterialTheme.typography.titleMedium);Text("Repositories, workflows, releases, and code remain readable at every selected size.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun CodeColorPreview(){val colors=LocalCodeColors.current;val code=buildAnnotatedString{withStyle(SpanStyle(color=colors.keyword,fontWeight=FontWeight.Bold)){append("fun ")};withStyle(SpanStyle(color=colors.type)){append("publishRelease")};append("() {\n  ");withStyle(SpanStyle(color=colors.keyword)){append("val ")};withStyle(SpanStyle(color=colors.property)){append("version")};append(" = ");withStyle(SpanStyle(color=colors.string)){append("\"1.0.0\"")};append("\n  ");withStyle(SpanStyle(color=colors.comment)){append("// Signed and verified")};append("\n  ");withStyle(SpanStyle(color=colors.type)){append("release")};append("(");withStyle(SpanStyle(color=colors.number)){append("100")};append(")\n}")};GlassCard{Text(code,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodyMedium)}}
@Composable private fun ToggleRow(icon:ImageVector,title:String,subtitle:String,checked:Boolean,onCheckedChange:(Boolean)->Unit){StandardSettingsRow(icon,title,subtitle){Switch(checked,onCheckedChange,Modifier.semantics{contentDescription="Toggle $title"})}}

private val AnimationStyle.displayName:String get()=when(this){AnimationStyle.Liquid->"Liquid";AnimationStyle.Spring->"Spring";AnimationStyle.Cinematic->"Cinematic";AnimationStyle.Magnetic->"Magnetic";AnimationStyle.Dynamic->"Dynamic"}
private val ThemeStyle.displayName:String get()=when(this){ThemeStyle.Clean->"Clean";ThemeStyle.LiquidGlass->"Liquid glass";ThemeStyle.Studio->"Studio";ThemeStyle.Midnight->"Midnight";ThemeStyle.Aurora->"Aurora";ThemeStyle.HighContrast->"High contrast";ThemeStyle.Obsidian->"Obsidian"}
private val AppFontFamily.displayName:String get()=when(this){AppFontFamily.SystemSans->"System sans";AppFontFamily.Serif->"Serif";AppFontFamily.Monospace->"Monospace"}
private val CodeColorStyle.displayName:String get()=when(this){CodeColorStyle.Classic->"Classic";CodeColorStyle.Ocean->"Ocean";CodeColorStyle.Sunset->"Sunset";CodeColorStyle.Monochrome->"Mono";CodeColorStyle.GitHub->"GitHub"}
private val NavigationBarStyle.displayName:String get()=when(this){NavigationBarStyle.FloatingCapsule->"Floating Capsule";NavigationBarStyle.Classic->"Classic";NavigationBarStyle.Minimal->"Minimal";NavigationBarStyle.Glass->"Glass";NavigationBarStyle.Compact->"Compact"}
private fun accentLabel(a:AccentColor)=when(a){AccentColor.DefaultGitHubRock->"Default GitHub Rock";AccentColor.Red->"Red";AccentColor.Orange->"Orange";AccentColor.Yellow->"Yellow";AccentColor.Green->"Green";AccentColor.Teal->"Teal";AccentColor.Cyan->"Cyan";AccentColor.Blue->"Blue";AccentColor.Indigo->"Indigo";AccentColor.Purple->"Purple";AccentColor.Pink->"Pink"}
private fun Color.toHex()="#%02X%02X%02X".format((red*255).roundToInt(),(green*255).roundToInt(),(blue*255).roundToInt())
private fun hslColor(h:Float,s:Float,l:Float):Color{val c=(1-kotlin.math.abs(2*l-1))*s;val x=c*(1-kotlin.math.abs((h*6)%2-1));val m=l-c/2;val q=when((h*6).toInt()){0->Triple(c,x,0f);1->Triple(x,c,0f);2->Triple(0f,c,x);3->Triple(0f,x,c);4->Triple(x,0f,c);else->Triple(c,0f,x)};return Color(q.first+m,q.second+m,q.third+m)}
private fun rgbHue(c:Color):Float{val max=maxOf(c.red,c.green,c.blue);val min=minOf(c.red,c.green,c.blue);val d=max-min;if(d==0f)return 0f;return (((when(max){c.red->(c.green-c.blue)/d;c.green->(c.blue-c.red)/d+2;else->(c.red-c.green)/d+4})/6f)%1f+1f)%1f}
private fun rgbSat(c:Color):Float{val max=maxOf(c.red,c.green,c.blue);val min=minOf(c.red,c.green,c.blue);val l=(max+min)/2;return if(max==min)0f else (max-min)/(1-kotlin.math.abs(2*l-1))}
private fun rgbLight(c:Color)=(maxOf(c.red,c.green,c.blue)+minOf(c.red,c.green,c.blue))/2
