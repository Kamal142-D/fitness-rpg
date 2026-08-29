package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.data.workout.WorkoutResultHolder
import com.fitnessrpg.app.domain.rankings.ExerciseRankingMode
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun WorkoutCompleteScreen(onDone: () -> Unit) {
    val result = WorkoutResultHolder.last

    ScreenScaffold {
        if (result == null) {
            ScreenHeader("Workout saved", subtitle = "Your progress has been recorded.")
            AppButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            return@ScreenScaffold
        }

        val gate = result.gate
        ScreenHeader(result.aggregates.name, subtitle = "Your performance report is ready.")

        var showRankUp by remember { mutableStateOf(false) }
        LaunchedEffect(gate.rankUps) { showRankUp = gate.rankUps > 0 }
        AnimatedVisibility(visible = showRankUp, enter = fadeIn() + scaleIn(initialScale = 0.88f)) {
            AppCard {
                StatusPill("System notice")
                AppText("EXERCISE RANK UP", variant = TextVariant.TITLE, tone = TextTone.ACCENT)
                gate.perExercise.filter { it.rankChanged }.forEach { exercise ->
                    AppText(
                        "${result.exerciseNames[exercise.exerciseId] ?: "Exercise"}  ${exercise.previousRank?.wire ?: "—"} → ${exercise.exerciseRank?.wire ?: "—"}",
                        variant = TextVariant.LABEL,
                        mono = true,
                    )
                }
            }
        }

        SectionHeader("Gate result")
        AppCard {
            AppText("GATE DIFFICULTY", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(gate.difficulty?.rank ?: com.fitnessrpg.app.domain.rank.Rank.E, size = RankBadgeSize.LG)
                Column {
                    AppText(gate.difficulty?.rank?.wire ?: "Not Assessed", variant = TextVariant.TITLE)
                    AppText("Based on today's load, reps, working sets and volume", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    if (gate.difficulty?.provisional == true) AppText("PROVISIONAL", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, mono = true)
                }
            }
        }

        AppCard {
            AppText("CLEAR GRADE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(gate.gateClearRank, size = RankBadgeSize.LG)
            Column {
                AppText("Gate Clear Grade ${gate.gateClearRank.wire}", variant = TextVariant.TITLE)
                AppText("Gate score ${gate.gateScore.roundToInt()}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
                if (gate.clearProvisional) AppText("PROVISIONAL · More baseline evidence needed", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
            }
            }
        }

        SectionHeader("Session summary")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatChip("XP EARNED", "+${gate.xpEarned}", modifier = Modifier.weight(1f))
            StatChip("SETS", "${result.aggregates.completedSets}", modifier = Modifier.weight(1f))
            StatChip("VOLUME", "${result.aggregates.totalVolumeKg.roundToInt()} kg", modifier = Modifier.weight(1f))
        }

        SectionHeader("Exercise results")
        AppCard {
            gate.perExercise.forEach { exercise ->
                val difficulty = gate.difficulty?.perExercise?.find { it.exerciseId == exercise.exerciseId }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        AppText(result.exerciseNames[exercise.exerciseId] ?: "Exercise", variant = TextVariant.LABEL)
                        AppText(
                            when (exercise.rankingMode) {
                                ExerciseRankingMode.GLOBAL -> "GLOBAL STRENGTH RANK · ${exercise.exerciseRank?.wire ?: "—"} · ${exercise.exerciseRp ?: 0} RP"
                                ExerciseRankingMode.PERSONAL -> "PERSONAL TIER · ${exercise.exerciseRank?.wire ?: "—"} · ${exercise.exerciseRp ?: 0} RP"
                                ExerciseRankingMode.UNRANKED -> "BASELINE · ${exercise.baselineSessions}/${exercise.requiredBaselineSessions} sessions"
                            },
                            variant = TextVariant.CAPTION,
                            tone = TextTone.SECONDARY,
                        )
                        difficulty?.let { AppText("Gate difficulty ${it.rank.wire}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        AppText(exercise.todayLabel, variant = TextVariant.CAPTION, tone = TextTone.SUCCESS)
                        if (exercise.rankingMode != ExerciseRankingMode.UNRANKED) AppText("+${exercise.rpDelta} RP", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, mono = true)
                        if (exercise.rankChanged) AppText("RANK UP", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, mono = true)
                    }
                }
            }
        }

        AppCard {
            AppText("RANK PROGRESSION", variant = TextVariant.HEADING)
            AppText("${gate.exercisesGainedRp} exercises gained RP", variant = TextVariant.LABEL)
            AppText("${gate.exercisesUnchanged} unchanged · ${gate.rankUps} rank ups", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
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
