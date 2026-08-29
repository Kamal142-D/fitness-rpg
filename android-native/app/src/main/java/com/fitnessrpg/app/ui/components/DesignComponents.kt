package com.fitnessrpg.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.fitnessrpg.app.R
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.theme.MotionTokens
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.theme.motionDuration

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText(title, variant = TextVariant.DISPLAY)
            subtitle?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
        }
        if (action != null) Row(content = action)
    }
}

@Composable
fun SectionHeader(title: String, meta: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(title, variant = TextVariant.HEADING)
        meta?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
    }
}

@Composable
fun StatusPill(label: String, modifier: Modifier = Modifier, color: Color = Palette.Primary) {
    val shape = RoundedCornerShape(Radius.pill)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), shape)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    ) {
        AppText(label.uppercase(), variant = TextVariant.CAPTION, color = color, mono = true)
    }
}

@Composable
fun SystemMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp).background(Palette.PrimaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_awesome), contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun AppIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contained: Boolean = false,
) {
    val containerModifier = if (contained) {
        Modifier
            .clip(CircleShape)
            .background(Palette.Surface2)
            .border(BorderStroke(1.dp, Palette.HairlineStrong), CircleShape)
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).then(containerModifier),
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) Palette.TextPrimary else Palette.TextTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun RevealContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(motionDuration(MotionTokens.Standard))) +
            slideInVertically(tween(motionDuration(MotionTokens.Standard))) { it / 12 },
    ) { content() }
}
