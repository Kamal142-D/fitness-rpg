package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.produceState
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.PlayerData
import com.fitnessrpg.app.domain.analytics.computeExerciseRanks
import com.fitnessrpg.app.domain.analytics.monthlyComparison
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.data.repo.RankAssessmentSnapshot
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.HunterRankPanel
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AttributeRow
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.XpBar
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(userId: String, onAssessment: () -> Unit) {
    val player = rememberCached("player:$userId") {
        coroutineScope {
            val prog = async { ServiceLocator.progressionRepository.getProgression(userId) }
            val data = async { ServiceLocator.analyticsRepository.getPlayerData(userId) }
            val assessment = async { ServiceLocator.assessmentRepository.getRankAssessment(userId) }
            Triple(prog.await(), data.await(), assessment.await())
        }
    }

    ScreenScaffold {
        Column {
            AppText("PLAYER", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Player", variant = TextVariant.DISPLAY)
        }

        val t = player.data
        when {
            t == null && player.error != null -> AppCard { AppText(friendlyDataError(player.error, "Couldn't load your Player data."), tone = TextTone.DANGER) }
            t == null -> AppText("Loading Player…", tone = TextTone.SECONDARY)
            else -> {
                val (prog, data, assessment) = t
                if (prog != null) {
                    if (prog.assessmentUpdateRequired || assessment.hunter.provisional) {
                        AppCard {
                            AppText("SYSTEM ASSESSMENT UPDATE", variant = TextVariant.HEADING, tone = TextTone.ACCENT)
                            AppText("The Hunter Ranking System has been improved. Complete the missing physical assessments to improve rank accuracy.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                            AppButton("Complete assessment", onAssessment, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    HunterRankPanel(
                        assessment.hunter,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppCard {
                        AppText("Level ${prog.level}", variant = TextVariant.TITLE)
                        XpBar(prog.level, prog.currentXp, modifier = Modifier.padding(top = Spacing.md).fillMaxWidth())
                    }
                }

                val monthly = monthlyComparison(data.sessions)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatChip("WORKOUTS (MO)", "${monthly.thisMonth.workouts} vs ${monthly.lastMonth.workouts}", modifier = Modifier.weight(1f))
                    StatChip("VOLUME (MO)", "${(monthly.thisMonth.volumeKg / 1000.0).roundToInt()}t vs ${(monthly.lastMonth.volumeKg / 1000.0).roundToInt()}t", modifier = Modifier.weight(1f))
                }

                val ranks = computeExerciseRanks(data.stats, data.bodyweightKg, data.sex)
                AppCard {
                    AppText("Exercise ranks", variant = TextVariant.HEADING, modifier = Modifier.padding(bottom = Spacing.md))
                    if (ranks.isEmpty()) {
                        AppText("Log the main barbell lifts to earn Exercise Ranks.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            ranks.forEach { rank ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                    RankBadge(rank.rank, size = RankBadgeSize.SM)
                                    AppText(rank.name, variant = TextVariant.LABEL, modifier = Modifier.weight(1f))
                                    rank.best1RMkg?.let {
                                        AppText("${it.roundToInt()} kg 1RM", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
