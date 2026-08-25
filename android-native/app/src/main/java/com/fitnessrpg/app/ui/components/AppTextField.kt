package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing

/**
 * Labeled text input. The border is a self-colored hairline that shifts to the
 * primary accent on focus and to danger on error — tonal, no glow.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    error: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    secure: Boolean = false,
    secureToggle: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var hidden by remember { mutableStateOf(secure || secureToggle) }

    val borderColor = when {
        error != null -> Palette.Danger
        focused -> Palette.Primary
        else -> Palette.HairlineStrong
    }
    val shape = RoundedCornerShape(Radius.md)
    val visual: VisualTransformation =
        if ((secure || secureToggle) && hidden) PasswordVisualTransformation() else VisualTransformation.None

    Column(modifier = modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.xs)) {
        if (label != null) {
            AppText(label.uppercase(), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, modifier = Modifier.padding(start = Spacing.xs))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(Palette.Surface2)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).padding(vertical = Spacing.md)) {
                if (value.isEmpty() && placeholder != null) {
                    AppText(placeholder, tone = TextTone.TERTIARY)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interaction,
                    textStyle = TextStyle(color = Palette.TextPrimary, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Palette.Primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    visualTransformation = visual,
                )
            }
            if (secureToggle) {
                Text(
                    text = if (hidden) "Show" else "Hide",
                    color = Palette.Primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .clickable { hidden = !hidden },
                )
            }
        }
        if (error != null) {
            AppText(error, variant = TextVariant.CAPTION, tone = TextTone.DANGER, modifier = Modifier.padding(start = Spacing.xs))
        }
    }
}
