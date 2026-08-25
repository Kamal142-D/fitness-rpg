package com.fitnessrpg.app.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.ui.theme.Palette

enum class TextVariant { DISPLAY, TITLE, HEADING, BODY, LABEL, CAPTION }

enum class TextTone { PRIMARY, SECONDARY, TERTIARY, ACCENT, SUCCESS, DANGER, INHERIT }

private fun styleFor(variant: TextVariant): TextStyle = when (variant) {
    TextVariant.DISPLAY -> TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
    TextVariant.TITLE -> TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
    TextVariant.HEADING -> TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    TextVariant.BODY -> TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    TextVariant.LABEL -> TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    TextVariant.CAPTION -> TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
}

private fun toneColor(tone: TextTone): Color = when (tone) {
    TextTone.PRIMARY -> Palette.TextPrimary
    TextTone.SECONDARY -> Palette.TextSecondary
    TextTone.TERTIARY -> Palette.TextTertiary
    TextTone.ACCENT -> Palette.Primary
    TextTone.SUCCESS -> Palette.Success
    TextTone.DANGER -> Palette.Danger
    TextTone.INHERIT -> Color.Unspecified
}

/**
 * Themed text primitive. All app text routes through this so sizes, weights, and
 * colors come from the design tokens rather than inline styles.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    variant: TextVariant = TextVariant.BODY,
    tone: TextTone = TextTone.PRIMARY,
    mono: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
) {
    var style = styleFor(variant)
    if (mono) style = style.copy(fontFamily = FontFamily.Monospace)
    val resolved = when {
        color != Color.Unspecified -> color
        tone == TextTone.INHERIT -> LocalContentColor.current
        else -> toneColor(tone)
    }
    Text(
        text = text,
        modifier = modifier,
        color = resolved,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
