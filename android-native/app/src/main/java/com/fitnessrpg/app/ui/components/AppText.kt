package com.fitnessrpg.app.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.fitnessrpg.app.ui.theme.AppFontFamily
import com.fitnessrpg.app.ui.theme.AppTypography
import com.fitnessrpg.app.ui.theme.Palette

enum class TextVariant { HERO, DISPLAY, TITLE, HEADING, BODY, LABEL, CAPTION }

enum class TextTone { PRIMARY, SECONDARY, TERTIARY, ACCENT, SUCCESS, DANGER, INHERIT }

private fun styleFor(variant: TextVariant): TextStyle = when (variant) {
    TextVariant.HERO -> AppTypography.displayLarge
    TextVariant.DISPLAY -> AppTypography.headlineLarge
    TextVariant.TITLE -> AppTypography.titleLarge
    TextVariant.HEADING -> AppTypography.titleMedium
    TextVariant.BODY -> AppTypography.bodyLarge
    TextVariant.LABEL -> AppTypography.labelLarge
    TextVariant.CAPTION -> AppTypography.labelSmall
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
    style = style.copy(fontFamily = if (mono) FontFamily.Monospace else AppFontFamily)
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
