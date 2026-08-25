package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.domain.gates.SuggestedGate
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing

/** "Today's Gate" call-to-action card on the System dashboard. */
@Composable
fun GateCard(gate: SuggestedGate, onEnter: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("TODAY'S GATE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(gate.name, variant = TextVariant.HEADING)
            }
            RankBadge(gate.difficulty, size = RankBadgeSize.MD)
        }

        if (gate.muscleGroups.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                gate.muscleGroups.forEach { Chip(it) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppText("${gate.durationMinutes} min", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            AppText(gate.intensity, variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
        }

        AppButton("Enter Gate", onClick = onEnter, modifier = Modifier.fillMaxWidth().padding(top = Spacing.md))
    }
}

@Composable
private fun Chip(text: String) {
    val shape = RoundedCornerShape(Radius.pill)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(shape)
            .background(Palette.Surface2)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Palette.Hairline), shape)
            .padding(horizontal = Spacing.md, vertical = 4.dp),
    ) {
        AppText(text, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
    }
}
