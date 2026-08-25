package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing

data class ChoiceOption<T>(val label: String, val value: T, val description: String? = null)

/**
 * Single-select list of tappable rows. The selected row is marked with a tonal
 * accent border + faint tint and an explicit indicator dot — never color alone.
 */
@Composable
fun <T> ChoiceGroup(
    options: List<ChoiceOption<T>>,
    value: T?,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { opt ->
            val selected = opt.value == value
            val shape = RoundedCornerShape(Radius.md)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(shape)
                    .background(if (selected) Palette.Primary.copy(alpha = 0.10f) else Palette.Surface1)
                    .border(
                        BorderStroke(1.dp, if (selected) Palette.Primary.copy(alpha = 0.7f) else Palette.Hairline),
                        shape,
                    )
                    .clickable { onChange(opt.value) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(2.dp, if (selected) Palette.Primary else Palette.HairlineStrong), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Palette.Primary))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    AppText(opt.label, variant = TextVariant.LABEL)
                    if (opt.description != null) {
                        AppText(opt.description, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    }
                }
            }
        }
    }
}
