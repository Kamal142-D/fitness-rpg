package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing

enum class CardTone { FLAT, RAISED }

/**
 * Container primitive. Depth comes from a self-colored hairline edge and a tonal
 * surface shift — not a drawn outline or a fat drop shadow.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.RAISED,
    padding: Dp = Spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = if (tone == CardTone.RAISED) Palette.Surface1 else Palette.Background
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, Palette.Hairline), shape)
            .padding(padding),
        content = content,
    )
}
