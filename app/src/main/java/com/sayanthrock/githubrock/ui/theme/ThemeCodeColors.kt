package com.sayanthrock.githubrock.ui.theme

import androidx.compose.ui.graphics.Color
import com.sayanthrock.githubrock.data.settings.CodeColorStyle

internal fun rockCodeColors(s:CodeColorStyle,d:Boolean)=when(s){
 CodeColorStyle.Classic->CodeColors(if(d)Color(0xFF79B8FF)else Color(0xFF0550AE),if(d)Color(0xFF85E89D)else Color(0xFF116329),if(d)Color(0xFF8B949E)else Color(0xFF57606A),if(d)Color(0xFFFFAB70)else Color(0xFF953800),if(d)Color(0xFFBC8CFF)else Color(0xFF8250DF),if(d)Color(0xFFFF7B72)else Color(0xFFCF222E))
 CodeColorStyle.Ocean->CodeColors(if(d)Color(0xFF58A6FF)else Color(0xFF0550AE),if(d)Color(0xFF7EE787)else Color(0xFF116329),if(d)Color(0xFF8B949E)else Color(0xFF57606A),if(d)Color(0xFF79C0FF)else Color(0xFF0A4A7A),if(d)Color(0xFFD2A8FF)else Color(0xFF6639BA),if(d)Color(0xFF39C5CF)else Color(0xFF006D75))
 CodeColorStyle.Sunset->CodeColors(if(d)Color(0xFFFF7B72)else Color(0xFFA40E26),if(d)Color(0xFFF2CC60)else Color(0xFF6F5500),if(d)Color(0xFF9DA7B3)else Color(0xFF57606A),if(d)Color(0xFFFFA657)else Color(0xFF953800),if(d)Color(0xFFD2A8FF)else Color(0xFF6639BA),if(d)Color(0xFFFF8FB3)else Color(0xFF9E1B59))
 CodeColorStyle.Monochrome->CodeColors(if(d)Color.White else Color.Black,if(d)Color(0xFFD0D7DE)else Color(0xFF24292F),if(d)Color(0xFF8C959F)else Color(0xFF57606A),if(d)Color(0xFFE6EDF3)else Color(0xFF24292F),if(d)Color.White else Color.Black,if(d)Color(0xFFC9D1D9)else Color(0xFF24292F))
 CodeColorStyle.GitHub->CodeColors(if(d)Color(0xFFFF7B72)else Color(0xFFCF222E),if(d)Color(0xFFA5D6FF)else Color(0xFF0A3069),if(d)Color(0xFF8B949E)else Color(0xFF6E7781),if(d)Color(0xFF79C0FF)else Color(0xFF0550AE),if(d)Color(0xFFD2A8FF)else Color(0xFF8250DF),if(d)Color(0xFFFFA657)else Color(0xFF953800))
}
