package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.MotionTokens
import com.fitnessrpg.app.ui.theme.motionDuration

/** Determinate progress track — a filled bar over a faint track, no glow. */
@Composable
fun AppProgressBar(value: Float, modifier: Modifier = Modifier) {
    val target = value.coerceIn(0f, 1f)
    val pct by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(motionDuration(MotionTokens.Standard)),
        label = "progress",
    )
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(shape)
            .background(Palette.Surface3),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .fillMaxHeight()
                .clip(shape)
                .background(Palette.Primary),
        )
    }
}
