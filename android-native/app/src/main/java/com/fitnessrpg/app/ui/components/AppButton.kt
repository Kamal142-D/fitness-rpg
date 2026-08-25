package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing

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
    val container = when (variant) {
        ButtonVariant.PRIMARY -> Palette.Primary
        ButtonVariant.SECONDARY -> Palette.Surface2
        ButtonVariant.GHOST -> Color.Transparent
    }
    val content = if (variant == ButtonVariant.PRIMARY) Color(0xFF07111F) else Palette.TextPrimary
    val border = when (variant) {
        ButtonVariant.SECONDARY -> BorderStroke(1.dp, Palette.HairlineStrong)
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled && !loading,
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
            CircularProgressIndicator(modifier = Modifier.heightIn(min = 18.dp), color = content, strokeWidth = 2.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) {
                    AppText(label, variant = TextVariant.LABEL, color = content)
                }
            }
        }
    }
}
