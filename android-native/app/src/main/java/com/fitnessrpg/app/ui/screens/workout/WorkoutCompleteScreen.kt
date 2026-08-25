package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.data.workout.WorkoutResultHolder
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun WorkoutCompleteScreen(onDone: () -> Unit) {
    val result = WorkoutResultHolder.last

    ScreenScaffold {
        AppText("GATE CLEARED", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)

        if (result == null) {
            AppText("Workout saved.", variant = TextVariant.DISPLAY)
            AppButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            return@ScreenScaffold
        }

        val gate = result.gate
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(gate.gateClearRank, size = RankBadgeSize.LG)
            Column {
                AppText("Gate Clear Rank ${gate.gateClearRank.name}", variant = TextVariant.TITLE)
                AppText("Gate score ${gate.gateScore.roundToInt()}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatChip("XP EARNED", "+${gate.xpEarned}", modifier = Modifier.weight(1f))
            StatChip("SETS", "${result.aggregates.completedSets}", modifier = Modifier.weight(1f))
            StatChip("VOLUME", "${result.aggregates.totalVolumeKg.roundToInt()} kg", modifier = Modifier.weight(1f))
        }

        AppCard {
            AppText("Personal records", variant = TextVariant.HEADING)
            if (result.prs.isEmpty()) {
                AppText("No new records this session — keep pushing.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    result.prs.forEach { pr ->
                        AppText(
                            "${pr.recordType.wire.replace('_', ' ')}: ${pr.newValue.roundToInt()}",
                            variant = TextVariant.LABEL,
                            tone = TextTone.SUCCESS,
                        )
                    }
                }
            }
        }

        AppButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
