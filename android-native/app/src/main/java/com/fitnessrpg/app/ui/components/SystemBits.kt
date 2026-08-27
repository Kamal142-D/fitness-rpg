package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.domain.progression.xpProgress
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.theme.rankColor
import kotlin.math.roundToInt

/** Level label + XP progress toward the next level. */
@Composable
fun XpBar(level: Int, currentXp: Int, modifier: Modifier = Modifier) {
    val p = xpProgress(currentXp, level)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText("Level $level", variant = TextVariant.LABEL)
            AppText("${p.current} / ${p.required} XP", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
        }
        AppProgressBar(p.fraction.toFloat())
    }
}

/** One attribute row: label, derived rank letter, value, and a tonal bar. */
@Composable
fun AttributeRow(label: String, value: Double, modifier: Modifier = Modifier) {
    val v = value.roundToInt()
    val rank = scoreToRank(value)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText(label, variant = TextVariant.LABEL, tone = TextTone.SECONDARY)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(rank.wire, color = rankColor(rank), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                AppText(v.toString(), variant = TextVariant.LABEL, mono = true)
            }
        }
        AppProgressBar((v / 100f).coerceIn(0f, 1f))
    }
}

/** Compact stat: a quiet tonal container with a caption label over a value. */
@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Palette.Surface1)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Palette.Hairline), shape)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppText(label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppText(value, variant = TextVariant.HEADING)
    }
}
