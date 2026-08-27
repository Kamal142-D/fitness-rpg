package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.theme.MotionTokens
import com.fitnessrpg.app.ui.theme.motionDuration

enum class ButtonVariant { PRIMARY, SECONDARY, GHOST }

/**
 * Button primitive. Solid or tonal surface with a legible label — no gradient
 * pill, no glow, no scale "boop". Minimum 44dp touch target.
 */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.985f else 1f,
        animationSpec = tween(motionDuration(MotionTokens.Press)),
        label = "buttonPress",
    )
    val haptics = LocalHapticFeedback.current
    val container = when (variant) {
        ButtonVariant.PRIMARY -> Palette.Primary
        ButtonVariant.SECONDARY -> Palette.Surface3
        ButtonVariant.GHOST -> Color.Transparent
    }
    val content = if (variant == ButtonVariant.PRIMARY) Color(0xFF07111F) else Palette.TextPrimary
    val border = when (variant) {
        ButtonVariant.SECONDARY -> BorderStroke(1.dp, Palette.HairlineStrong)
        else -> null
    }

    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = RoundedCornerShape(Radius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.45f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
        border = border,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.heightIn(min = 20.dp), color = content, strokeWidth = 2.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
                    AppText(label, variant = TextVariant.LABEL, color = content)
                }
            }
        }
    }
}
