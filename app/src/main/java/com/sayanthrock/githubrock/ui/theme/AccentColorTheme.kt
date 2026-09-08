package com.sayanthrock.githubrock.ui.theme

import androidx.compose.ui.graphics.Color
import com.sayanthrock.githubrock.data.settings.AccentColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class AccentTonalPalette(val lightPrimary:Color,val lightContainer:Color,val lightOnPrimary:Color,val lightOnContainer:Color,val darkPrimary:Color,val darkContainer:Color,val darkOnPrimary:Color,val darkOnContainer:Color)
private data class Hsl(val h:Float,val s:Float,val l:Float)
fun AccentColor.seedColor():Color=when(this){AccentColor.DefaultGitHubRock->Color(0xFFE60023);AccentColor.Red->Color(0xFFE53935);AccentColor.Orange->Color(0xFFFF7A00);AccentColor.Yellow->Color(0xFFE6A700);AccentColor.Green->Color(0xFF2E9B50);AccentColor.Teal->Color(0xFF00897B);AccentColor.Cyan->Color(0xFF00A6B2);AccentColor.Blue->Color(0xFF0969DA);AccentColor.Indigo->Color(0xFF4F46C7);AccentColor.Purple->Color(0xFF8250DF);AccentColor.Pink->Color(0xFFC72C7A)}
fun parseAccentHex(value:String):Color?{val c=value.trim().removePrefix("#");if(c.length!=6&&c.length!=8)return null;if(!c.all{it in "0123456789abcdefABCDEF"})return null;val n=c.toLongOrNull(16)?:return null;return if(c.length==8)Color(((n shr 16)and 255)/255f,((n shr 8)and 255)/255f,(n and 255)/255f,((n shr 24)and 255)/255f)else Color(((n shr 16)and 255)/255f,((n shr 8)and 255)/255f,(n and 255)/255f)}
fun normalizeAccentHex(value:String):String?{val c=value.trim().removePrefix("#");if(c.length!=6&&c.length!=8)return null;if(!c.all{it in "0123456789abcdefABCDEF"})return null;return "#${c.uppercase()}"}
fun accentPalette(seed:Color):AccentTonalPalette{val h=rgbToHsl(seed);val lp=hslColor(h.h,max(h.s,.58f),.42f);val lc=hslColor(h.h,max(h.s*.72f,.32f),.92f);val dp=hslColor(h.h,max(h.s*.82f,.52f),.78f);val dc=hslColor(h.h,max(h.s*.78f,.38f),.24f);return AccentTonalPalette(lp,lc,readableOn(lp),readableOn(lc),dp,dc,readableOn(dp),readableOn(dc))}
fun contrastRatio(foreground:Color,background:Color):Float{val a=relativeLuminance(foreground)+.05f;val b=relativeLuminance(background)+.05f;return max(a,b)/min(a,b)}
fun readableOn(background:Color)=if(contrastRatio(Color.White,background)>=contrastRatio(Color.Black,background))Color.White else Color.Black
private fun relativeLuminance(c:Color):Float{fun linear(v:Float)=if(v<=.04045f)v/12.92f else ((v+.055f)/1.055f).toDouble().pow(2.4).toFloat();return .2126f*linear(c.red)+.7152f*linear(c.green)+.0722f*linear(c.blue)}
private fun rgbToHsl(c:Color):Hsl{val r=c.red;val g=c.green;val b=c.blue;val mx=max(r,max(g,b));val mn=min(r,min(g,b));val d=mx-mn;val l=(mx+mn)/2;if(d==0f)return Hsl(0f,0f,l);val s=d/(1-kotlin.math.abs(2*l-1));val h=when(mx){r->((g-b)/d).let{if(it<0)it+6 else it};g->(b-r)/d+2;else->(r-g)/d+4}/6f;return Hsl(h,s,l)}
private fun hslColor(hue:Float,saturation:Float,lightness:Float):Color{val h=(hue%1f+1)%1f;val s=saturation.coerceIn(0f,1f);val l=lightness.coerceIn(0f,1f);val c=(1-kotlin.math.abs(2*l-1))*s;val x=c*(1-kotlin.math.abs((h*6)%2-1));val m=l-c/2;val q=when((h*6).toInt()){0->Triple(c,x,0f);1->Triple(x,c,0f);2->Triple(0f,c,x);3->Triple(0f,x,c);4->Triple(x,0f,c);else->Triple(c,0f,x)};return Color(q.first+m,q.second+m,q.third+m)}
