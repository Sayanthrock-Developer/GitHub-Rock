package com.sayanthrock.githubrock.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Shared motion language for GitHub Rock.
 *
 * Motion should communicate hierarchy, continuity, and state. Keep animations
 * short, interruptible, and consistent across screens. Prefer these tokens
 * over one-off durations or springs in feature code.
 */
object RockMotion {
    const val Fast = 100
    const val Quick = 140
    const val Standard = 180
    const val Smooth = 240
    const val PageTransition = 280

    const val ScreenOpen = Standard
    const val ScreenClose = Quick
    const val DialogOpen = Quick
    const val DialogClose = Fast
    const val SheetOpen = Standard
    const val SheetClose = Quick
    const val Menu = Quick
    const val Navigation = Quick
    const val Press = Fast

    /** Small spatial movement used when a component enters the hierarchy. */
    const val FlowEnterOffset = 18

    /** Slight compression gives press feedback without a bouncy scale. */
    const val PressScale = 0.985f

    fun <T> open(durationMillis: Int = Standard): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)

    fun <T> close(durationMillis: Int = Quick): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutLinearInEasing)

    fun <T> settle(durationMillis: Int = Smooth): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing)

    fun <T> page(reduceMotion: Boolean): AnimationSpec<T> =
        tween(
            durationMillis = duration(reduceMotion, PageTransition),
            easing = FastOutSlowInEasing
        )

    fun <T> fastOrZero(reduceMotion: Boolean, durationMillis: Int = Fast): AnimationSpec<T> =
        if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        }

    fun duration(reduceMotion: Boolean, durationMillis: Int): Int =
        if (reduceMotion) 0 else durationMillis
}
