package com.fitnessrpg.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable

/** Short, purpose-driven motion tokens shared by every screen. */
object MotionTokens {
    const val Press = 90
    const val Quick = 150
    const val Standard = 240
    const val Emphasis = 300
}

@Composable
fun motionDuration(duration: Int): Int = if (ValueAnimator.areAnimatorsEnabled()) duration else 0
