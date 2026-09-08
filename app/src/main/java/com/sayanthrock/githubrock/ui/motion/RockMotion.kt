package com.sayanthrock.githubrock.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/** Central motion tokens for GitHub Rock. Keep transitions fast and interruptible. */
object RockMotion {
    const val Fast = 100
    const val Quick = 140
    const val Standard = 180
    const val Smooth = 240

    const val ScreenOpen = 160
    const val ScreenClose = 120
    const val DialogOpen = 140
    const val DialogClose = 100
    const val SheetOpen = 180
    const val SheetClose = 140
    const val Menu = 120
    const val Navigation = 150
    const val Press = 90

    fun <T> open(durationMillis: Int = Standard): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)

    fun <T> close(durationMillis: Int = Quick): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutLinearInEasing)

    fun <T> settle(durationMillis: Int = Smooth): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing)

    fun <T> fastOrZero(reduceMotion: Boolean, durationMillis: Int = Fast): AnimationSpec<T> =
        if (reduceMotion) tween(durationMillis = 0) else tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)

    fun duration(reduceMotion: Boolean, durationMillis: Int): Int =
        if (reduceMotion) 0 else durationMillis
}
